package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId

/**
 * Resultado de parsear un documento de frases del catálogo. A diferencia de
 * [com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult], no es un subtipo suyo: es un `sealed interface`
 * del dominio y Kotlin exige declarar sus subtipos directos en el mismo módulo. Además, una frase sin `source` no
 * es un `CatalogPhrase` válido (exige `source` no vacío), así que este resultado usa [TargetPhrase] hasta que el
 * self-JOIN de [CatalogDao.phrasesFor] aporte el texto de origen.
 */
sealed interface CatalogParseResult {

    data class PhrasesParsed(val phrases: List<TargetPhrase>) : CatalogParseResult

    data class Failed(val error: CatalogLoadError) : CatalogParseResult
}

/** Frase validada de un documento, sin texto de origen todavía: lo aporta el self-JOIN con otro idioma. */
data class TargetPhrase(
    val id: PhraseId,
    val level: CefrLevel,
    val topic: Topic,
    val targetLocale: LanguageTag,
    val target: String,
)
