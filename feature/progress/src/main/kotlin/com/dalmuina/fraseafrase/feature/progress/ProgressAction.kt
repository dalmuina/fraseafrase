package com.dalmuina.fraseafrase.feature.progress

/**
 * Acciones del usuario sobre la pantalla de progreso. Llegan por un único `onAction()` (ADR-0006).
 * La spec `progress-overview` no define ninguna acción todavía: la pantalla solo lee el progreso al abrirse.
 */
sealed interface ProgressAction
