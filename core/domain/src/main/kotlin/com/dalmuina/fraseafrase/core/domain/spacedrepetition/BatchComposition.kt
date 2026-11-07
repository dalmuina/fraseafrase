package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Resultado de componer la tanda siguiente (ADR-0012, ADR-0017). Que no haya nada que practicar es un
 * resultado esperable, no una excepción: [NothingAvailable] significa que no se crea tanda.
 */
sealed interface BatchComposition {

    /** Tanda creada con las frases disponibles. */
    data class Composed(val batch: PracticeBatch) : BatchComposition

    /** No hay a reforzar, dominadas ni frases nuevas disponibles: no se crea tanda. */
    data object NothingAvailable : BatchComposition
}
