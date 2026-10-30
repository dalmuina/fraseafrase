package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Nivel de consolidación de una frase, entero entre 0 y [MAX_LEVEL] inclusive (ADR-0012).
 * El nivel 0 significa frase nueva o en aprendizaje. Fuera de rango es un error de programación,
 * por eso se lanza [IllegalArgumentException] y no se modela como fallo esperable.
 */
@JvmInline
value class Level(val value: Int) {

    init {
        require(value in 0..MAX_LEVEL) { "El nivel debe estar entre 0 y $MAX_LEVEL, recibido $value" }
    }

    companion object {
        const val MAX_LEVEL = 5
    }
}
