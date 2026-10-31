package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Resultado de componer la tanda siguiente (ADR-0012). Que no haya nada que practicar es un resultado
 * esperable, no una excepción: [NothingAvailable] significa que no se crea tanda.
 */
sealed interface BatchComposition {

    /** Tanda creada con las frases disponibles, y la cola sobrante que sigue arrastrándose. */
    data class Composed(val batch: PracticeBatch, val leftover: CarryQueue) : BatchComposition

    /** No hay arrastradas, pendientes ni frases nuevas: no se crea tanda. */
    data object NothingAvailable : BatchComposition
}
