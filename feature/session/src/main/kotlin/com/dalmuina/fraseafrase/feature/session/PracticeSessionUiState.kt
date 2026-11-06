package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem

/**
 * Estado inmutable de la pantalla de práctica (ADR-0006, design D3). Vive en el `StateFlow` del ViewModel.
 */
sealed interface PracticeSessionUiState {

    /** Carga inicial de la sesión, antes de que llegue el primer resultado. */
    data object Loading : PracticeSessionUiState

    /**
     * Práctica de la tanda activa. [current] es la frase que se muestra. [revealed] indica si su traducción en
     * portugués está visible. [busy] está a true mientras se registra una respuesta o se genera la sesión
     * siguiente. [completed] es si la tanda activa ya ha dado una vuelta completa (todas sus frases respondidas);
     * mientras es `true`, la pantalla muestra el mensaje de tanda completada en vez de [current] (design D7 de
     * `replace-batch-closure-with-session-generation`). [index] es la posición (1-based) de [current] dentro de la
     * vuelta actual a la tanda; [total] es su tamaño (design D1 de `add-practice-batch-indicator`).
     * [remainingHitsForNextLevel] es cuántos aciertos consecutivos le faltan a [current] para la insignia
     * [nextLevelBadgeLabel] (spec: practice-session, "Aciertos para el próximo nivel visibles").
     */
    data class Practicing(
        val current: PracticeItem,
        val revealed: Boolean,
        val busy: Boolean,
        val completed: Boolean,
        val index: Int,
        val total: Int,
        val remainingHitsForNextLevel: Int,
        val nextLevelBadgeLabel: String,
    ) : PracticeSessionUiState

    /** No hay frases para practicar (ADR-0012). */
    data object NothingToPractice : PracticeSessionUiState

    /** El catálogo no se puede cargar: no se puede practicar. */
    data object CatalogError : PracticeSessionUiState
}
