package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/**
 * Frase del catálogo precargado (ADR-0012, ADR-0013): identificador, nivel CEFR, tema, locales BCP-47 y textos.
 * `target` es el texto en el idioma que se aprende y `source` el de tu idioma. Ningún campo lleva el nombre de un
 * idioma concreto (ADR-0003). El orden de la lista de catálogo se usa solo como desempate determinista.
 * Un texto vacío es un dato inválido y lanza [IllegalArgumentException].
 */
data class CatalogPhrase(
    val id: PhraseId,
    val level: CefrLevel,
    val topic: Topic,
    val targetLocale: LanguageTag,
    val sourceLocale: LanguageTag,
    val target: String,
    val source: String,
) {

    init {
        require(target.isNotBlank()) { "The target text cannot be blank" }
        require(source.isNotBlank()) { "The source text cannot be blank" }
    }
}
