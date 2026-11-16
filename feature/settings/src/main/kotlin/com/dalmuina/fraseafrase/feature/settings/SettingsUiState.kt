package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Estado inmutable de la pantalla de ajustes (ADR-0006). Vive en el `StateFlow` del ViewModel.
 */
sealed interface SettingsUiState {

    /** Carga inicial de los ajustes, antes de que llegue el primer resultado. */
    data object Loading : SettingsUiState

    /**
     * Ajustes mostrados, ya autoguardados según se eligen (design D3 de `remove-settings-save-button`).
     * [activeTargetLocale] es el idioma destino elegido en el desplegable, que coincide con el idioma destino
     * activo persistido: elegirlo lo activa de inmediato. [availableTargetLocales] son los idiomas destino que
     * se pueden elegir. [batchSizeText] es el texto en edición (no un `BatchSize`), porque mientras no sea
     * válido no hay un `BatchSize` que lo represente. [isBatchSizeValid] indica si el texto actual es un N
     * autoguardable.
     */
    data class Loaded(
        val activeTargetLocale: LanguageTag,
        val availableTargetLocales: List<LanguageTag>,
        val cefrLevel: CefrLevel,
        val batchSizeText: String,
        val isBatchSizeValid: Boolean,
    ) : SettingsUiState
}
