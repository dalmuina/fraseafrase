package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel

/** Acciones del usuario sobre la pantalla de ajustes. Llegan por un único `onAction()` (ADR-0006). */
sealed interface SettingsAction {

    /** Elige un nivel CEFR de la lista cerrada del enum: siempre un valor válido. */
    data class SelectCefrLevel(val level: CefrLevel) : SettingsAction

    /** Cambia el texto en edición del tamaño de tanda N. */
    data class ChangeBatchSizeText(val text: String) : SettingsAction

    /** Confirma el guardado de los ajustes editados. */
    data object Save : SettingsAction

    /** Consume la confirmación visual de guardado pendiente, para que no se repita (design D1). */
    data object ConsumeSaveConfirmation : SettingsAction

    /** Consume la navegación a práctica pendiente, para que no se repita (design D1). */
    data object ConsumeNavigateToPractice : SettingsAction
}
