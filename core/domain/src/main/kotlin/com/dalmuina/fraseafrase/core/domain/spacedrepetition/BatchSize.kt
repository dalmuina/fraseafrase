package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Tamaño N de la tanda: número máximo de frases entre [MIN] y [MAX] inclusive, con [DEFAULT] por defecto
 * (ADR-0012). Un valor fuera de rango es un error de programación, por eso se lanza
 * [IllegalArgumentException]; la capa de datos hace el fallback a [DEFAULT] si un ajuste está corrupto.
 */
@JvmInline
value class BatchSize(val value: Int) {

    init {
        require(value in MIN..MAX) { "Batch size must be between $MIN and $MAX, got $value" }
    }

    companion object {
        const val MIN = 10
        const val MAX = 30
        val DEFAULT = BatchSize(20)
    }
}
