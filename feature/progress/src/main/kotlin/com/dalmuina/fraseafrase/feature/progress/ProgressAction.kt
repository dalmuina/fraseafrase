package com.dalmuina.fraseafrase.feature.progress

/**
 * Acciones sobre la pantalla de progreso. Llegan por un único `onAction()` (ADR-0006).
 */
sealed interface ProgressAction {
    /** La pantalla se ha mostrado (también al volver a ella): relee el progreso vigente sin pasar por `Loading`. */
    data object Resume : ProgressAction
}
