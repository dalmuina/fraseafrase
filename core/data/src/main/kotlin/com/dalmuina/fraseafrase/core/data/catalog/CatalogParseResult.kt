package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/**
 * Resultado de parsear uno de los dos tipos de documento de catálogo (frases destino o traducción).
 * A diferencia de [com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult], no es un subtipo
 * suyo: `CatalogLoadResult` es un `sealed interface` del dominio, declarado en `:core:domain`, y
 * Kotlin exige que todo subtipo directo de un tipo sellado se declare en el mismo módulo de
 * compilación que la declaración (design.md - "Dos tipos de documento JSON"). Además, una frase
 * destino sin `source` todavía no es un `CatalogPhrase` del dominio válido (exige `source` no
 * vacío), así que este resultado intermedio usa sus propios tipos ([TargetPhrase],
 * [CatalogTranslation]) hasta que `RoomCatalogDataSource` combine ambos tras el JOIN.
 */
sealed interface CatalogParseResult {

    data class PhrasesParsed(val phrases: List<TargetPhrase>) : CatalogParseResult

    data class TranslationsParsed(val translations: List<CatalogTranslation>) : CatalogParseResult

    data class Failed(val error: CatalogLoadError) : CatalogParseResult
}

/** Frase destino validada, sin traducción todavía: le falta el `source` que aporta un documento de traducción. */
data class TargetPhrase(
    val id: PhraseId,
    val level: CefrLevel,
    val topic: Topic,
    val targetLocale: LanguageTag,
    val target: String,
)

/** Traducción validada de una frase destino de [targetLocale], desde el idioma de origen [sourceLocale]. */
data class CatalogTranslation(
    val id: PhraseId,
    val targetLocale: LanguageTag,
    val sourceLocale: LanguageTag,
    val source: String,
)
