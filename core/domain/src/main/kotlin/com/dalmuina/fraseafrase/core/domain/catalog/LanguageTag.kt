package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Etiqueta de idioma BCP-47 (`pt-PT`, `es-ES`), sin valores por defecto en el dominio (ADR-0003).
 * Se valida la forma básica: un subtag de idioma de 2 o 3 letras y subtags separados por guion.
 * Un valor mal formado lanza [IllegalArgumentException].
 */
@JvmInline
value class LanguageTag(val value: String) {

    init {
        require(isWellFormed(value)) { "Invalid BCP-47 language tag: '$value'" }
    }

    companion object {
        private val BCP47_PATTERN = Regex("^[a-z]{2,3}(-[A-Za-z0-9]{1,8})*$")

        /** Indica si [value] tiene la forma básica de una etiqueta BCP-47. No valida el registro de subtags. */
        fun isWellFormed(value: String): Boolean = BCP47_PATTERN.matches(value)
    }
}
