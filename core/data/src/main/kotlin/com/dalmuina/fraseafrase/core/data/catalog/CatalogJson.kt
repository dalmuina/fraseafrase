package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parseo puro del catálogo JSON (ADR-0013). Convierte el texto en frases del dominio, copiando los locales de la raíz
 * a cada frase, y devuelve errores tipados en lugar de excepciones. Los campos son anulables para que la validación
 * sea nuestra y no un fallo del deserializador: así cada motivo de rechazo tiene su resultado.
 */
object CatalogJson {

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): CatalogLoadResult {
        val document = runCatching { json.decodeFromString<CatalogDocument>(text) }
            .getOrElse { return CatalogLoadResult.Failed(CatalogLoadError.MalformedDocument) }
        val error = headerErrorOf(document) ?: phrasesErrorOf(document)
        return if (error != null) {
            CatalogLoadResult.Failed(error)
        } else {
            CatalogLoadResult.Loaded(toPhrases(document))
        }
    }

    /** Errores de la cabecera: versión del esquema y locales de la raíz. */
    private fun headerErrorOf(document: CatalogDocument): CatalogLoadError? {
        val version = document.schemaVersion ?: return CatalogLoadError.MissingSchemaVersion
        return when {
            version != SUPPORTED_SCHEMA_VERSION -> CatalogLoadError.UnsupportedSchemaVersion(version)
            !isValidLocale(document.targetLocale) -> CatalogLoadError.InvalidLocale("targetLocale")
            !isValidLocale(document.sourceLocale) -> CatalogLoadError.InvalidLocale("sourceLocale")
            else -> null
        }
    }

    /** Primer error de las frases en orden de catálogo: id ausente, id repetido, o un campo inválido. */
    private fun phrasesErrorOf(document: CatalogDocument): CatalogLoadError? {
        val seen = mutableSetOf<String>()
        return document.phrases.orEmpty().firstNotNullOfOrNull { raw ->
            val id = raw.id?.takeIf(String::isNotBlank)
                ?: return@firstNotNullOfOrNull CatalogLoadError.InvalidPhrase(raw.id.orEmpty(), "id")
            when {
                !seen.add(id) -> CatalogLoadError.DuplicateId(id)
                else -> invalidFieldOf(raw)?.let { CatalogLoadError.InvalidPhrase(id, it) }
            }
        }
    }

    /** Nombre del primer campo de la frase que no cumple la spec, o null si todos son válidos. */
    private fun invalidFieldOf(raw: PhraseDocument): String? = when {
        raw.level.isNullOrBlank() || CefrLevel.entries.none { it.name == raw.level } -> "level"
        raw.topic.isNullOrBlank() -> "topic"
        raw.target.isNullOrBlank() -> "target"
        raw.source.isNullOrBlank() -> "source"
        else -> null
    }

    /** Solo se llama con un documento validado por [headerErrorOf] y [phrasesErrorOf]. */
    private fun toPhrases(document: CatalogDocument): List<CatalogPhrase> {
        val targetLocale = LanguageTag(checkNotNull(document.targetLocale))
        val sourceLocale = LanguageTag(checkNotNull(document.sourceLocale))
        return document.phrases.orEmpty().map { raw ->
            CatalogPhrase(
                id = PhraseId(checkNotNull(raw.id)),
                level = CefrLevel.valueOf(checkNotNull(raw.level)),
                topic = Topic(checkNotNull(raw.topic)),
                targetLocale = targetLocale,
                sourceLocale = sourceLocale,
                target = checkNotNull(raw.target),
                source = checkNotNull(raw.source),
            )
        }
    }

    private fun isValidLocale(tag: String?): Boolean = tag != null && LanguageTag.isWellFormed(tag)
}

@Serializable
private data class CatalogDocument(
    val schemaVersion: Int? = null,
    val targetLocale: String? = null,
    val sourceLocale: String? = null,
    val phrases: List<PhraseDocument>? = null,
)

@Serializable
private data class PhraseDocument(
    val id: String? = null,
    val level: String? = null,
    val topic: String? = null,
    val target: String? = null,
    val source: String? = null,
)
