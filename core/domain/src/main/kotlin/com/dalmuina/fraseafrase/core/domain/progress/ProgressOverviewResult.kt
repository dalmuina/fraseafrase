package com.dalmuina.fraseafrase.core.domain.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

/** Resultado de [ObtainProgressOverview]: el resumen de progreso, o el motivo por el que no está disponible. */
sealed interface ProgressOverviewResult {

    /**
     * [totalAvailable] es el total de frases del catálogo del nivel CEFR vigente (suma de [byLevel]);
     * [cefrLevel] es ese mismo nivel CEFR vigente en ajustes. Ninguno de los dos depende de si hay
     * tanda activa (ADR-0018). [byLevel] tiene una entrada por cada nivel de 0 a [Level.MAX_LEVEL]
     * con el total de frases del catálogo en ese nivel.
     */
    data class Available(
        val totalAvailable: Int,
        val cefrLevel: CefrLevel,
        val byLevel: Map<Level, Int>,
    ) : ProgressOverviewResult

    data class CatalogUnavailable(val error: CatalogLoadError) : ProgressOverviewResult
}
