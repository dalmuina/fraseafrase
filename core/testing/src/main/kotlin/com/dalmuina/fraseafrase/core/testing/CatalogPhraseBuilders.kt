package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/**
 * Crea una [CatalogPhrase] de prueba con nivel CEFR A1 por defecto, como fijan los escenarios de composición.
 * Los campos nuevos de ADR-0013 tienen valores de prueba; los tests de composición solo usan `id` y `level`.
 */
fun aCatalogPhrase(
    id: String = "phrase-1",
    level: CefrLevel = CefrLevel.A1,
    topic: String = "saludos",
    target: String = "Bom dia",
    source: String = "Buenos días",
): CatalogPhrase = CatalogPhrase(
    id = PhraseId(id),
    level = level,
    topic = Topic(topic),
    targetLocale = LanguageTag("pt-PT"),
    sourceLocale = LanguageTag("es-ES"),
    target = target,
    source = source,
)
