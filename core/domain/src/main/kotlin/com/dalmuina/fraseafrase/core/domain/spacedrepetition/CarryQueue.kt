package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Cola de frases arrastradas entre tandas, en orden: la primera ocupa la posición 0 (ADR-0012).
 * Es el único lugar que guarda ese orden. Los ids deben ser únicos; si no lo son, es un error de
 * programación y se lanza [IllegalArgumentException].
 */
data class CarryQueue(val ids: List<PhraseId>) {

    init {
        require(ids.distinct().size == ids.size) { "La cola de arrastradas no admite ids duplicados" }
    }

    companion object {

        /**
         * Cola de la tanda siguiente al cerrar: la cola sobrante seguida de las frases arrastradas del cierre,
         * en el orden de la tanda (ADR-0012). Función pura. Si una frase ya estaba en la cola sobrante,
         * la construcción lanza [IllegalArgumentException] por la invariante de unicidad.
         */
        fun afterClosure(leftover: CarryQueue, outcomes: List<BatchClosureOutcome>): CarryQueue {
            val carried = outcomes
                .filterIsInstance<BatchClosureOutcome.Carried>()
                .map { it.progress.id }
            return CarryQueue(leftover.ids + carried)
        }
    }
}
