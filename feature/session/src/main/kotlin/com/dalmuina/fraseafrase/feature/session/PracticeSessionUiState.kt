package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem

/**
 * Estado inmutable de la pantalla de práctica (ADR-0006, design D3). Vive en el `StateFlow` del ViewModel.
 */
sealed interface PracticeSessionUiState {

    /** Carga inicial de la sesión, antes de que llegue el primer resultado. */
    data object Loading : PracticeSessionUiState

    /**
     * Práctica de la tanda activa. [current] es la frase que se muestra, o null si la tanda llega vacía.
     * [revealed] indica si su traducción en portugués está visible. [busy] está a true mientras se registra una
     * respuesta o se cierra la tanda. [closeWarning] es el número de no certificadas del aviso de cierre, o null si
     * el diálogo no está abierto.
     */
    data class Practicing(
        val current: PracticeItem?,
        val revealed: Boolean,
        val busy: Boolean,
        val closeWarning: Int?,
    ) : PracticeSessionUiState

    /** No hay frases para practicar (ADR-0012). */
    data object NothingToPractice : PracticeSessionUiState

    /** El catálogo no se puede cargar: no se puede practicar. */
    data object CatalogError : PracticeSessionUiState
}
