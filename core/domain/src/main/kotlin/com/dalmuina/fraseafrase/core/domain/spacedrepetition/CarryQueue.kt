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
}
