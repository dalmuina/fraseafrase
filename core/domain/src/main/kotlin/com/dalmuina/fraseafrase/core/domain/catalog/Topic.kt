package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Tema de una frase del catálogo (p. ej. "saludos"). Es un texto libre, no un enum: el catálogo crece con
 * contenido nuevo sin tocar código (ADR-0013). Un tema vacío es un dato inválido y lanza
 * [IllegalArgumentException].
 */
@JvmInline
value class Topic(val value: String) {

    init {
        require(value.isNotBlank()) { "A topic cannot be blank" }
    }
}
