package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parseo puro del documento de frases del catálogo JSON (ADR-0023). Convierte el texto en los tipos
 * intermedios de [CatalogParseResult], copiando el locale de la raíz a cada frase, y devuelve errores tipados en
 * lugar de excepciones. Los campos son anulables para que la validación sea nuestra y no un fallo del
 * deserializador: así cada motivo de rechazo tiene su resultado.
 */
object CatalogJson {

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Cabecera de un documento (schemaVersion + targetLocale), sin validar el resto. El sembrado de
     * [RoomCatalogDataSource] la usa para decidir si resembrar un idioma destino antes de (y aunque falle) el
     * `parse` completo.
     */
    fun headerOf(text: String): CatalogHeader? {
        // Decodifica solo la cabecera (sin `phrases`): con `ignoreUnknownKeys` ya activo en [json], el
        // array de frases se salta durante el parseo en vez de materializarse en memoria, para no pagar
        // el coste de las ~1000 frases dos veces por idioma destino en cada arranque (una vez aquí, otra
        // en `parse`, cuando el sembrado sí hace falta).
        val document = runCatching { json.decodeFromString<CatalogHeaderDocument>(text) }.getOrNull()
        val schemaVersion = document?.schemaVersion
        val targetLocale = document?.targetLocale?.takeIf { isValidLocale(it) }
        return if (schemaVersion != null && targetLocale != null) {
            CatalogHeader(
                schemaVersion = schemaVersion,
                targetLocale = targetLocale,
            )
        } else {
            null
        }
    }

    /**
     * Parsea un documento de frases, copiando `targetLocale`
     * de la raíz a cada frase.
     */
    fun parsePhrases(text: String): CatalogParseResult {
        val document = runCatching { json.decodeFromString<PhraseCatalogDocument>(text) }
            .getOrElse { return CatalogParseResult.Failed(CatalogLoadError.MalformedDocument) }
        val error = phraseHeaderErrorOf(document) ?: targetPhrasesErrorOf(document)
        return if (error != null) {
            CatalogParseResult.Failed(error)
        } else {
            CatalogParseResult.PhrasesParsed(toTargetPhrases(document))
        }
    }

    private fun phraseHeaderErrorOf(document: PhraseCatalogDocument): CatalogLoadError? {
        val version = document.schemaVersion ?: return CatalogLoadError.MissingSchemaVersion
        return when {
            version != SUPPORTED_SCHEMA_VERSION -> CatalogLoadError.UnsupportedSchemaVersion(version)
            !isValidLocale(document.targetLocale) -> CatalogLoadError.InvalidLocale("targetLocale")
            else -> null
        }
    }

    /** Primer error de las frases destino en orden de catálogo: id ausente, id repetido, o un campo inválido. */
    private fun targetPhrasesErrorOf(document: PhraseCatalogDocument): CatalogLoadError? {
        val seen = mutableSetOf<String>()
        return document.phrases.orEmpty().firstNotNullOfOrNull { raw ->
            val id = raw.id?.takeIf(String::isNotBlank)
                ?: return@firstNotNullOfOrNull CatalogLoadError.InvalidPhrase(raw.id.orEmpty(), "id")
            when {
                !seen.add(id) -> CatalogLoadError.DuplicateId(id)
                else -> invalidTargetFieldOf(raw)?.let { CatalogLoadError.InvalidPhrase(id, it) }
            }
        }
    }

    /** Nombre del primer campo de la frase destino que no cumple la spec, o null si todos son válidos. */
    private fun invalidTargetFieldOf(raw: TargetPhraseDocument): String? = when {
        raw.level.isNullOrBlank() || CefrLevel.entries.none { it.name == raw.level } -> "level"
        raw.topic.isNullOrBlank() -> "topic"
        raw.target.isNullOrBlank() -> "target"
        else -> null
    }

    /** Solo se llama con un documento validado por [phraseHeaderErrorOf] y [targetPhrasesErrorOf]. */
    private fun toTargetPhrases(document: PhraseCatalogDocument): List<TargetPhrase> {
        val targetLocale = LanguageTag(checkNotNull(document.targetLocale))
        return document.phrases.orEmpty().map { raw ->
            TargetPhrase(
                id = PhraseId(checkNotNull(raw.id)),
                level = CefrLevel.valueOf(checkNotNull(raw.level)),
                topic = Topic(checkNotNull(raw.topic)),
                targetLocale = targetLocale,
                target = checkNotNull(raw.target),
            )
        }
    }

    private fun isValidLocale(tag: String?): Boolean = tag != null && LanguageTag.isWellFormed(tag)
}

/** Cabecera mínima de un documento de frases, usada por el sembrado antes de validarlo por completo. */
data class CatalogHeader(val schemaVersion: Int, val targetLocale: String)

@Serializable
private data class CatalogHeaderDocument(
    val schemaVersion: Int? = null,
    val targetLocale: String? = null,
)

@Serializable
private data class PhraseCatalogDocument(
    val schemaVersion: Int? = null,
    val targetLocale: String? = null,
    val phrases: List<TargetPhraseDocument>? = null,
)

@Serializable
private data class TargetPhraseDocument(
    val id: String? = null,
    val level: String? = null,
    val topic: String? = null,
    val target: String? = null,
)
