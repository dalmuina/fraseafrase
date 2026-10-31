package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PracticeBatch

/** Resultado de obtener la tanda activa. Todos los casos son esperables: ninguno lanza excepción. */
sealed interface ActiveBatchResult {

    /** Ya había una tanda activa: se devuelve tal cual, en su orden guardado. */
    data class Existing(val batch: PracticeBatch) : ActiveBatchResult

    /** No había tanda activa y se compuso y guardó una nueva. */
    data class Created(val batch: PracticeBatch) : ActiveBatchResult

    /** No hay arrastradas, pendientes ni frases nuevas: no se crea tanda (ADR-0012). */
    data object NothingAvailable : ActiveBatchResult

    /** El catálogo no se puede cargar, así que no se puede componer ninguna tanda. */
    data class CatalogUnavailable(val error: CatalogLoadError) : ActiveBatchResult
}
