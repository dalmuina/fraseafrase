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
     * Ajustes mostrados y en edición. [activeTargetLocale] es el idioma destino elegido en el desplegable, que
     * no tiene por qué coincidir con el idioma destino activo persistido hasta que se confirma el guardado
     * (design `add-multi-language-catalog`: mismo patrón que [cefrLevel]/[batchSizeText], en edición hasta
     * [SettingsAction.Save]). [availableTargetLocales] son los idiomas destino que se pueden elegir.
     * [batchSizeText] es el texto en edición (no un `BatchSize`), porque mientras no sea válido no hay un
     * `BatchSize` que lo represente. [isBatchSizeValid] es la única condición para guardar.
     * [saveConfirmationPending] y [navigateToPracticePending] son señales de un solo uso (design D1 de
     * `add-settings-apply-flow`): la UI las consume con `ConsumeSaveConfirmation`/`ConsumeNavigateToPractice`
     * para que vuelvan a `false` y no se repitan en una recomposición o rotación.
     */
    data class Loaded(
        val activeTargetLocale: LanguageTag,
        val availableTargetLocales: List<LanguageTag>,
        val cefrLevel: CefrLevel,
        val batchSizeText: String,
        val isBatchSizeValid: Boolean,
        val saveConfirmationPending: Boolean = false,
        val navigateToPracticePending: Boolean = false,
    ) : SettingsUiState
}
