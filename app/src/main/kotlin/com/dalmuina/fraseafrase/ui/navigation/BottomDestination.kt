package com.dalmuina.fraseafrase.ui.navigation

/**
 * Destinos de la barra de navegación inferior (design.md - Decisions, "Barra de navegación
 * inferior en `:app`"). Solo `:app` conoce las tres features (ADR-0010).
 */
sealed interface BottomDestination {
    data object Practice : BottomDestination

    data object Progress : BottomDestination

    data object Settings : BottomDestination
}
