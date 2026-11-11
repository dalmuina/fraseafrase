package com.dalmuina.fraseafrase.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.designsystem.component.levelBadgeLabel
import com.dalmuina.fraseafrase.core.domain.audio.PlayPhraseAudio
import com.dalmuina.fraseafrase.core.domain.practice.AnswerPhrase
import com.dalmuina.fraseafrase.core.domain.practice.GenerateSession
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSessionResult
import com.dalmuina.fraseafrase.core.domain.practice.RevertLastAnswer
import com.dalmuina.fraseafrase.core.domain.practice.UndoableAnswer
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentación de la práctica (ADR-0006). Lee la sesión al iniciar y solo depende de casos de uso. El estado vive en
 * memoria, en el `StateFlow`: sobrevive a la rotación, pero no a la muerte del proceso (design D3).
 */
@HiltViewModel
class PracticeSessionViewModel @Inject constructor(
    private val obtainPracticeSession: ObtainPracticeSession,
    private val answerPhrase: AnswerPhrase,
    private val generateSession: GenerateSession,
    private val playPhraseAudio: PlayPhraseAudio,
    private val revertLastAnswer: RevertLastAnswer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PracticeSessionUiState>(PracticeSessionUiState.Loading)
    val uiState: StateFlow<PracticeSessionUiState> = _uiState.asStateFlow()

    // Ids de la tanda cargada, en orden, para que `resume()` detecte si de verdad cambió (design D4). No es parte
    // del `UiState` expuesto: es solo un apunte interno para esa comparación.
    private var loadedBatchIds: List<PhraseId> = emptyList()

    // Autoevaluación más reciente, pendiente de revertir (design D3). Se reemplaza con cada autoevaluación nueva,
    // nunca se acumula, y se limpia a `null` al revertir, al generar sesión y en cualquier recarga que no sea
    // continuación directa de una autoevaluación (`reload()` sin `previous`).
    private var pendingUndo: PendingUndo? = null

    init {
        viewModelScope.launch { reload() }
    }

    fun onAction(action: PracticeSessionAction) {
        when (action) {
            PracticeSessionAction.Reveal -> reveal()
            is PracticeSessionAction.Answer -> answer(action.assessment)
            PracticeSessionAction.PlayAudio -> playAudio()
            PracticeSessionAction.GenerateSession -> requestGenerateSession()
            // Vuelve a leer la sesión al entrar en la pestaña (design D4). Solo sustituye el estado si la tanda
            // leída (frase actual, orden y tamaño) difiere de la ya cargada; si es la misma, no toca `revealed`,
            // `busy`, `completed` ni `index`.
            PracticeSessionAction.Resume -> viewModelScope.launch { reload(onlyIfChanged = true) }
            PracticeSessionAction.ConsumeBadgeChange -> consumeBadgeChange()
            PracticeSessionAction.RevertLastAnswer -> revertLast()
        }
    }

    /**
     * Genera la sesión siguiente: resuelve la tanda activa (certifica, arrastra) y compone la siguiente en un solo
     * paso (design D3 de `replace-batch-closure-with-session-generation`). Se ignora mientras hay una respuesta en
     * curso (`busy`).
     */
    private fun requestGenerateSession() {
        val state = currentPracticing() ?: return
        if (state.busy) return
        // Ninguna autoevaluación de la tanda que se cierra es revertible una vez que se genera la siguiente
        // (spec: practice-session, "Generar sesión hace desaparecer el botón").
        pendingUndo = null
        _uiState.value = state.copy(busy = true, canRevertLastAnswer = false)
        viewModelScope.launch {
            generateSession()
            reload()
        }
    }

    /** Revelar se ignora con una respuesta en curso (`busy`) o con la tanda completada (design D3). */
    private fun reveal() = updatePracticing {
        if (busy || completed) this else copy(revealed = true)
    }

    /**
     * Registra la autoevaluación de la frase revelada. Se ignora sin revelar, mientras hay una respuesta en curso
     * (`busy`) y con la tanda completada (spec: practice-session; design D3).
     */
    private fun answer(assessment: SelfAssessment) {
        val state = currentPracticing() ?: return
        if (state.isAnswerable()) {
            // `busy` se activa antes de lanzar la corrutina: un segundo toque antes de terminar se descarta (D2).
            // `canRevertLastAnswer` se fija a `false` en el mismo `copy()`, de forma síncrona: el botón de
            // revertir desaparece de inmediato al autoevaluar, sin esperar a que termine la corrutina (design D4).
            _uiState.value = state.copy(busy = true, canRevertLastAnswer = false)
            val levelBefore = state.current.level
            val indexBefore = state.index
            viewModelScope.launch {
                val outcome = answerPhrase(state.current.id, assessment)
                val levelAfter = outcome.resultingLevel
                pendingUndo = PendingUndo(outcome.undo, indexBefore)
                val badgeChange = badgeChangeFor(levelBefore, levelAfter)
                if (badgeChange != null) {
                    // Con cambio de nivel, la frase respondida sigue en pantalla con `busy` ya en `true`: el avance
                    // lo dispara `consumeBadgeChange()` al terminar la transición (spec: new-badge-animation,
                    // design, Decisión 1), no esta respuesta. `canRevertLastAnswer` se fija a `false` de forma
                    // explícita, no heredada del `copy()` anterior (design D4): el botón no aparece mientras dura
                    // la transición de insignia.
                    updatePracticing { copy(badgeChange = badgeChange, canRevertLastAnswer = false) }
                } else {
                    reload(previous = state)
                }
            }
        }
    }

    /**
     * Avanza a la frase siguiente al terminar la transición de insignia (spec: new-badge-animation, design,
     * Decisión 1). `reload()` ya construye el siguiente estado con `busy = false` y `badgeChange = null` por
     * defecto, así que no hace falta ningún `copy` adicional para limpiarlos.
     */
    private fun consumeBadgeChange() {
        val state = currentPracticing() ?: return
        viewModelScope.launch { reload(previous = state) }
    }

    /** Escuchar se ignora sin revelar, con una respuesta en curso (`busy`) o con la tanda completada. */
    private fun playAudio() {
        val state = currentPracticing() ?: return
        if (state.isAnswerable()) {
            playPhraseAudio(state.current.target, state.current.targetLocale)
        }
    }

    private fun currentPracticing(): PracticeSessionUiState.Practicing? =
        _uiState.value as? PracticeSessionUiState.Practicing

    private inline fun updatePracticing(
        transform: PracticeSessionUiState.Practicing.() -> PracticeSessionUiState.Practicing,
    ) {
        val state = currentPracticing() ?: return
        _uiState.value = state.transform()
    }

    /**
     * Vuelve a leer la sesión y reemplaza el estado. Sin [previous], el contador de posición arranca en 1 (carga
     * inicial o recarga tras generar sesión). Con [previous], avanza el contador a partir de su `index`/`total`
     * (design D1): sube en uno, o vuelve a 1 si se completó la vuelta a la tanda; `total` se recalcula con la
     * lectura fresca. Con [onlyIfChanged], no reemplaza el estado si la tanda leída es igual a la ya cargada (design
     * D4 de `add-settings-apply-flow`).
     */
    private suspend fun reload(previous: PracticeSessionUiState.Practicing? = null, onlyIfChanged: Boolean = false) {
        val session = obtainPracticeSession()
        if (onlyIfChanged && session.hasSameBatchAs(loadedBatchIds)) return
        if (session is PracticeSessionResult.Ready) loadedBatchIds = session.items.map { it.id }
        // Sin `previous` no hay ninguna autoevaluación de la que `pendingUndo` sea reflejo (carga inicial, Resume
        // tras un cambio externo de tanda o recarga tras generar sesión): se limpia para no apuntar a una frase
        // que puede ya no estar en la última posición de la tanda nueva (design D3).
        if (previous == null) pendingUndo = null
        _uiState.value = when (session) {
            is PracticeSessionResult.Ready -> {
                val current = session.items.first()
                PracticeSessionUiState.Practicing(
                    current = current,
                    revealed = false,
                    busy = false,
                    completed = session.isComplete,
                    index = previous.nextIndex().coerceAtMost(session.items.size),
                    total = session.items.size,
                    remainingHitsForNextLevel = current.remainingHitsForNextLevel(),
                    nextLevelBadgeLabel = current.nextLevelBadgeLabel(),
                    canRevertLastAnswer = pendingUndo != null,
                    hits = session.batchOutcome.hits,
                    misses = session.batchOutcome.misses,
                    levelUps = session.batchOutcome.levelUps,
                )
            }
            PracticeSessionResult.NothingToPractice -> PracticeSessionUiState.NothingToPractice
            is PracticeSessionResult.CatalogFailed -> PracticeSessionUiState.CatalogError
        }
    }

    /**
     * Revierte la autoevaluación más reciente, descrita por [pendingUndo] (spec: revert-selection, design D5). Se
     * ignora sin efecto si ya hay una respuesta en curso (`busy`) o si no hay ninguna autoevaluación pendiente de
     * revertir. El `Practicing` siguiente se reconstruye a mano, sin pasar por `reload()`, con `revealed = true` y
     * en `pendingUndo.indexBefore`, porque `reload()` solo sabe avanzar la posición, no retroceder (design D5).
     */
    private fun revertLast() {
        val state = currentPracticing() ?: return
        val undo = pendingUndo
        if (state.busy || undo == null) return
        // Mismo guard síncrono que `answer()` (design D5): fija `busy = true` y limpia `pendingUndo` antes de
        // lanzar la corrutina, para que un segundo toque o una autoevaluación casi simultánea no compitan por la
        // misma transacción de `PracticeRepository.transact`.
        _uiState.value = state.copy(busy = true, canRevertLastAnswer = false)
        pendingUndo = null
        viewModelScope.launch {
            revertLastAnswer(undo.answer)
            val session = obtainPracticeSession()
            if (session is PracticeSessionResult.Ready) {
                val current = session.items.first()
                _uiState.value = PracticeSessionUiState.Practicing(
                    current = current,
                    revealed = true,
                    busy = false,
                    completed = session.isComplete,
                    index = undo.indexBefore,
                    total = session.items.size,
                    remainingHitsForNextLevel = current.remainingHitsForNextLevel(),
                    nextLevelBadgeLabel = current.nextLevelBadgeLabel(),
                    badgeChange = null,
                    canRevertLastAnswer = false,
                    hits = session.batchOutcome.hits,
                    misses = session.batchOutcome.misses,
                    levelUps = session.batchOutcome.levelUps,
                )
            }
        }
    }
}

/**
 * Autoevaluación más reciente de toda la tanda pendiente de revertir (design D3): [answer] es lo necesario para
 * revertirla con `RevertLastAnswer`; [indexBefore] es el `index` ("X de Y") que tenía la pantalla justo antes de
 * autoevaluarla, para restaurar el indicador de posición al revertir.
 */
private data class PendingUndo(val answer: UndoableAnswer, val indexBefore: Int)

/** Compara [this] con [ids], la tanda ya cargada: distinta si cambió de resultado, frase, orden o tamaño (D4). */
private fun PracticeSessionResult.hasSameBatchAs(ids: List<PhraseId>): Boolean =
    this is PracticeSessionResult.Ready && items.map { it.id } == ids

private fun PracticeSessionUiState.Practicing?.nextIndex(): Int = when {
    this == null -> 1
    index == total -> 1
    else -> index + 1
}

private fun PracticeSessionUiState.Practicing.isAnswerable(): Boolean =
    revealed && !busy && !completed

/** Aciertos consecutivos que le faltan a la frase para certificar la racha de la tanda (spec: practice-session). */
private const val HITS_TO_CERTIFY = 3

/** Nivel de consolidación máximo (spec: practice-session, ADR-0012/ADR-0015). */
private const val MAX_CONSOLIDATION_LEVEL = 5

/** Cuántos aciertos consecutivos le faltan a esta frase para la insignia del siguiente nivel. */
private fun PracticeItem.remainingHitsForNextLevel(): Int = (HITS_TO_CERTIFY - streak.value).coerceAtLeast(0)

/** Nombre de la insignia del siguiente nivel de consolidación de esta frase (ADR-0010: UI usa `Int`, no `Level`). */
private fun PracticeItem.nextLevelBadgeLabel(): String {
    val nextLevel = (level.value + 1).coerceAtMost(MAX_CONSOLIDATION_LEVEL)
    return requireNotNull(levelBadgeLabel(nextLevel)) {
        "Level $nextLevel should always have a badge label"
    }
}

/**
 * Cambio de insignia a animar tras comparar el nivel [before] (antes de autoevaluar) con [after] (devuelto por
 * [AnswerPhrase]); `null` si el nivel no cambió (spec: add-level-change-animation, design D1/D2; spec:
 * new-badge-animation, design, Decisión 3).
 */
private fun badgeChangeFor(before: Level, after: Level): BadgeChange? {
    if (before.value == after.value) return null
    val direction = if (after.value > before.value) BadgeChange.Direction.UP else BadgeChange.Direction.DOWN
    val badgeLabel = requireNotNull(levelBadgeLabel(after.value)) {
        "Level ${after.value} should always have a badge label"
    }
    return BadgeChange(direction, badgeLabel, fromLevel = before.value, toLevel = after.value)
}
