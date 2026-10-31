package com.dalmuina.fraseafrase.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.domain.practice.AnswerPhrase
import com.dalmuina.fraseafrase.core.domain.practice.CloseActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.CloseBatchResult
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSessionResult
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
    private val closeActiveBatch: CloseActiveBatch,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PracticeSessionUiState>(PracticeSessionUiState.Loading)
    val uiState: StateFlow<PracticeSessionUiState> = _uiState.asStateFlow()

    /** Frases no certificadas de la última sesión leída: es el número que muestra el aviso de cierre (design D4). */
    private var uncertifiedCount = 0

    init {
        viewModelScope.launch { reload() }
    }

    fun onAction(action: PracticeSessionAction) {
        when (action) {
            PracticeSessionAction.Reveal -> reveal()
            is PracticeSessionAction.Answer -> answer(action.assessment)
            PracticeSessionAction.RequestClose -> requestClose()
            PracticeSessionAction.CancelClose -> cancelClose()
            PracticeSessionAction.ConfirmClose -> confirmClose()
        }
    }

    /** Abre el aviso con el número de frases no certificadas de la última sesión. Cero también se acepta. */
    private fun requestClose() {
        val state = _uiState.value as? PracticeSessionUiState.Practicing ?: return
        _uiState.value = state.copy(closeWarning = uncertifiedCount)
    }

    private fun cancelClose() {
        val state = _uiState.value as? PracticeSessionUiState.Practicing ?: return
        _uiState.value = state.copy(closeWarning = null)
    }

    /**
     * Cierra la tanda con el número que mostró el aviso (D4). Se ignora sin aviso abierto, así que un doble toque no
     * cierra dos veces. El diálogo se cierra y `busy` evita otra acción mientras se cierra.
     */
    private fun confirmClose() {
        val state = _uiState.value as? PracticeSessionUiState.Practicing ?: return
        val expected = state.closeWarning ?: return
        _uiState.value = state.copy(busy = true, closeWarning = null)
        viewModelScope.launch {
            when (closeActiveBatch(expected)) {
                // Stale y NoActiveBatch no son errores: se recarga la tanda guardada (spec: practice-session).
                CloseBatchResult.Closed, CloseBatchResult.Stale, CloseBatchResult.NoActiveBatch -> reload()
            }
        }
    }

    private fun reveal() {
        val state = _uiState.value as? PracticeSessionUiState.Practicing ?: return
        _uiState.value = state.copy(revealed = true)
    }

    /**
     * Registra la autoevaluación de la frase revelada. Se ignora sin revelar, mientras hay una respuesta en curso
     * (`busy`) y con el aviso de cierre abierto (spec: practice-session; design D3).
     */
    private fun answer(assessment: SelfAssessment) {
        val state = _uiState.value as? PracticeSessionUiState.Practicing ?: return
        val current = state.current
        if (current != null && state.isAnswerable()) {
            // `busy` se activa antes de lanzar la corrutina: un segundo toque antes de terminar se descarta (D2).
            _uiState.value = state.copy(busy = true)
            viewModelScope.launch {
                answerPhrase(current.id, assessment)
                reload()
            }
        }
    }

    private fun PracticeSessionUiState.Practicing.isAnswerable(): Boolean =
        revealed && !busy && closeWarning == null

    /** Vuelve a leer la sesión y reemplaza el estado. La siguiente frase y su orden los decide la persistencia (D2). */
    private suspend fun reload() {
        _uiState.value = when (val session = obtainPracticeSession()) {
            is PracticeSessionResult.Ready -> {
                uncertifiedCount = session.uncertifiedCount
                PracticeSessionUiState.Practicing(
                    current = session.items.firstOrNull(),
                    revealed = false,
                    busy = false,
                    closeWarning = null,
                )
            }
            PracticeSessionResult.NothingToPractice -> PracticeSessionUiState.NothingToPractice
            is PracticeSessionResult.CatalogFailed -> PracticeSessionUiState.CatalogError
        }
    }
}
