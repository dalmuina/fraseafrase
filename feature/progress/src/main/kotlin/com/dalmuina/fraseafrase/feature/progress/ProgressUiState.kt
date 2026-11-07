package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

/**
 * Estado inmutable de la pantalla de progreso (ADR-0006). Vive en el `StateFlow` del ViewModel.
 */
sealed interface ProgressUiState {

    /** Carga inicial del progreso, antes de que llegue el primer resultado. */
    data object Loading : ProgressUiState

    /**
     * Progreso mostrado. [totalAvailable] es la suma de [byLevel] (total de frases del catálogo) y
     * [cefrLevel] es el nivel CEFR vigente de los ajustes (spec `progress-overview`, "Total de frases
     * disponibles y nivel CEFR vigente"). [byLevel] tiene una entrada por cada nivel de 0 a
     * [Level.MAX_LEVEL] con el total del catálogo en ese nivel. Ninguno de estos datos depende de si
     * hay tanda activa.
     */
    data class Content(
        val totalAvailable: Int,
        val cefrLevel: CefrLevel,
        val byLevel: Map<Level, Int>,
    ) : ProgressUiState

    /** El catálogo no se pudo leer: no hay ningún número que mostrar (spec `progress-overview`). */
    data object CatalogError : ProgressUiState
}
