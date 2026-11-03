package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

/**
 * Estado inmutable de la pantalla de progreso (ADR-0006). Vive en el `StateFlow` del ViewModel.
 */
sealed interface ProgressUiState {

    /** Carga inicial del progreso, antes de que llegue el primer resultado. */
    data object Loading : ProgressUiState

    /**
     * Progreso mostrado. [certifiedInBatch] es `null` sin tanda activa (spec `progress-overview`,
     * "Progreso sin tanda activa") y, si existe, lleva las certificadas sobre el total de la tanda.
     * [byLevel] tiene una entrada por cada nivel de 0 a [Level.MAX_LEVEL] con el total del catálogo en ese nivel.
     */
    data class Content(
        val certifiedInBatch: CertifiedCount?,
        val byLevel: Map<Level, Int>,
    ) : ProgressUiState

    /** El catálogo no se pudo leer: no hay ningún número que mostrar (spec `progress-overview`). */
    data object CatalogError : ProgressUiState
}

/** Certificadas (racha ≥ 3) sobre el total de frases de la tanda activa. */
data class CertifiedCount(val certified: Int, val total: Int)
