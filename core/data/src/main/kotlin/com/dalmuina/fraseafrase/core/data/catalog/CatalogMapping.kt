package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/** Mapeos entre las filas de Room y los tipos del dominio (ADR-0010: los mapeos viven en :core:data). */
internal fun CatalogPhraseEntity.toDomain(): CatalogPhrase = CatalogPhrase(
    id = PhraseId(id),
    level = CefrLevel.valueOf(level),
    topic = Topic(topic),
    targetLocale = LanguageTag(targetLocale),
    sourceLocale = LanguageTag(sourceLocale),
    target = target,
    source = source,
)

internal fun CatalogPhrase.toEntity(position: Int): CatalogPhraseEntity = CatalogPhraseEntity(
    id = id.value,
    targetLocale = targetLocale.value,
    sourceLocale = sourceLocale.value,
    position = position,
    level = level.name,
    topic = topic.value,
    target = target,
    source = source,
)
