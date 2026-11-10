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
) : ViewModel() {

    private val _uiState = MutableStateFlow<PracticeSessionUiState>(PracticeSessionUiState.Loading)
    val uiState: StateFlow<PracticeSessionUiState> = _uiState.asStateFlow()

    // Ids de la tanda cargada, en orden, para que `resume()` detecte si de verdad cambió (design D4). No es parte
    // del `UiState` expuesto: es solo un apunte interno para esa comparación.
    private var loadedBatchIds: List<PhraseId> = emptyList()

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
        _uiState.value = state.copy(busy = true)
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
            _uiState.value = state.copy(busy = true)
            val levelBefore = state.current.level
            viewModelScope.launch {
                val levelAfter = answerPhrase(state.current.id, assessment)
                reload(previous = state)
                val badgeChange = badgeChangeFor(levelBefore, levelAfter)
                updatePracticing { copy(badgeChange = badgeChange, busy = badgeChange != null) }
            }
        }
    }

    /** Limpia el indicador de cambio de insignia ya mostrado y libera los controles (design D2/D3). */
    private fun consumeBadgeChange() = updatePracticing {
        copy(badgeChange = null, busy = false)
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
                )
            }
            PracticeSessionResult.NothingToPractice -> PracticeSessionUiState.NothingToPractice
            is PracticeSessionResult.CatalogFailed -> PracticeSessionUiState.CatalogError
        }
    }
}

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
 * [AnswerPhrase]); `null` si el nivel no cambió (spec: add-level-change-animation, design D1/D2).
 */
private fun badgeChangeFor(before: Level, after: Level): BadgeChange? {
    if (before.value == after.value) return null
    val direction = if (after.value > before.value) BadgeChange.Direction.UP else BadgeChange.Direction.DOWN
    val badgeLabel = requireNotNull(levelBadgeLabel(after.value)) {
        "Level ${after.value} should always have a badge label"
    }
    return BadgeChange(direction, badgeLabel)
}
