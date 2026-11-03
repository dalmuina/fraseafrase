package com.dalmuina.fraseafrase.core.domain.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

/** Resultado de [ObtainProgressOverview]: el resumen de progreso, o el motivo por el que no está disponible. */
sealed interface ProgressOverviewResult {

    /**
     * [batchProgress] es `null` sin tanda activa. [byLevel] tiene una entrada por cada nivel de 0 a
     * [Level.MAX_LEVEL] con el total de frases del catálogo en ese nivel.
     */
    data class Available(
        val batchProgress: BatchProgress?,
        val byLevel: Map<Level, Int>,
    ) : ProgressOverviewResult

    data class CatalogUnavailable(val error: CatalogLoadError) : ProgressOverviewResult
}

/** Certificadas (racha ≥ 3) sobre el total de frases de la tanda activa. */
data class BatchProgress(val certified: Int, val total: Int)
