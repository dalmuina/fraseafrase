package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/** Mapeos entre las filas de Room y los tipos del dominio (ADR-0010: los mapeos viven en :core:data). */
internal fun CatalogPhraseWithSource.toDomain(): CatalogPhrase = CatalogPhrase(
    id = PhraseId(phrase.id),
    level = CefrLevel.valueOf(phrase.level),
    topic = Topic(phrase.topic),
    targetLocale = LanguageTag(phrase.targetLocale),
    sourceLocale = LanguageTag(sourceLocale),
    target = phrase.target,
    source = source,
)

internal fun TargetPhrase.toEntity(position: Int): CatalogPhraseEntity = CatalogPhraseEntity(
    id = id.value,
    targetLocale = targetLocale.value,
    position = position,
    level = level.name,
    topic = topic.value,
    target = target,
)
