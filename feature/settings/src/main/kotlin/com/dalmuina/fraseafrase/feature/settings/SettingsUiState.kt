package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel

/**
 * Estado inmutable de la pantalla de ajustes (ADR-0006). Vive en el `StateFlow` del ViewModel.
 */
sealed interface SettingsUiState {

    /** Carga inicial de los ajustes, antes de que llegue el primer resultado. */
    data object Loading : SettingsUiState

    /**
     * Ajustes mostrados y en edición. [batchSizeText] es el texto en edición (no un `BatchSize`), porque mientras
     * no sea válido no hay un `BatchSize` que lo represente. [isBatchSizeValid] es la única condición para guardar.
     */
    data class Loaded(
        val cefrLevel: CefrLevel,
        val batchSizeText: String,
        val isBatchSizeValid: Boolean,
    ) : SettingsUiState
}
