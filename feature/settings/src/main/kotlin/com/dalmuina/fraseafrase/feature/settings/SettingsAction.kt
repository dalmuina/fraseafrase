package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Acciones del usuario sobre la pantalla de ajustes. Llegan por un único `onAction()` (ADR-0006). Desde
 * `remove-settings-save-button` (design D3), cada edición válida se autoguarda de inmediato: no hay ninguna
 * acción explícita de guardado ni señal de un solo uso que consumir.
 */
sealed interface SettingsAction {

    /** Elige un idioma destino del desplegable: lo activa de inmediato (spec: "Elegir un idioma destino lo
     * activa de inmediato"). */
    data class SelectLanguage(val targetLocale: LanguageTag) : SettingsAction

    /** Elige un nivel CEFR de la lista cerrada del enum: siempre un valor válido, se autoguarda de inmediato. */
    data class SelectCefrLevel(val level: CefrLevel) : SettingsAction

    /** Cambia el texto en edición del tamaño de tanda N; si es válido, se autoguarda de inmediato. */
    data class ChangeBatchSizeText(val text: String) : SettingsAction
}
