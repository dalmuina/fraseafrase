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
                sourceLocale = document.sourceLocale,
            )
        } else {
            null
        }
    }

    /**
     * Parsea un documento de frases destino (sin `sourceLocale` ni `source`), copiando `targetLocale`
     * de la raíz a cada frase. El `parse()` que sigue, de un único documento con `source` incluido, es
     * el formato legado (ADR-0013) que usa todavía `RoomCatalogDataSource`, pendiente de bifurcarse a
     * `parsePhrases`/`parseTranslations` (tasks.md, grupo 4).
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

    /**
     * Parsea un documento de traducción, copiando `targetLocale`/`sourceLocale` de la raíz a cada
     * traducción. Delega en [TranslationParsing] para no superar el límite de funciones por objeto.
     */
    fun parseTranslations(text: String): CatalogParseResult = TranslationParsing.parse(text)

    /**
     * Formato legado de un único documento con `source` incluido (ADR-0013), usado todavía por
     * `RoomCatalogDataSource` hasta que se bifurque a `parsePhrases`/`parseTranslations` (tasks.md,
     * grupo 4). Delega en [LegacyCatalogParsing] para no superar el límite de funciones por objeto.
     */
    fun parse(text: String): CatalogLoadResult = LegacyCatalogParsing.parse(text)

    private fun isValidLocale(tag: String?): Boolean = tag != null && LanguageTag.isWellFormed(tag)
}

/** Parseo del documento de traducción (`targetLocale` + `sourceLocale`), design.md - "Dos tipos de documento JSON". */
private object TranslationParsing {

    private const val SUPPORTED_SCHEMA_VERSION = 1
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): CatalogParseResult {
        val document = runCatching { json.decodeFromString<TranslationCatalogDocument>(text) }
            .getOrElse { return CatalogParseResult.Failed(CatalogLoadError.MalformedDocument) }
        val error = headerErrorOf(document) ?: translationsErrorOf(document)
        return if (error != null) {
            CatalogParseResult.Failed(error)
        } else {
            CatalogParseResult.TranslationsParsed(toTranslations(document))
        }
    }

    private fun headerErrorOf(document: TranslationCatalogDocument): CatalogLoadError? {
        val version = document.schemaVersion ?: return CatalogLoadError.MissingSchemaVersion
        return when {
            version != SUPPORTED_SCHEMA_VERSION -> CatalogLoadError.UnsupportedSchemaVersion(version)
            !isValidLocale(document.targetLocale) -> CatalogLoadError.InvalidLocale("targetLocale")
            !isValidLocale(document.sourceLocale) -> CatalogLoadError.InvalidLocale("sourceLocale")
            else -> null
        }
    }

    /** Primer error de las traducciones en orden de documento: id ausente, id repetido, o `source` vacío. */
    private fun translationsErrorOf(document: TranslationCatalogDocument): CatalogLoadError? {
        val seen = mutableSetOf<String>()
        return document.translations.orEmpty().firstNotNullOfOrNull { raw ->
            val id = raw.id?.takeIf(String::isNotBlank)
                ?: return@firstNotNullOfOrNull CatalogLoadError.InvalidPhrase(raw.id.orEmpty(), "id")
            when {
                !seen.add(id) -> CatalogLoadError.DuplicateId(id)
                raw.source.isNullOrBlank() -> CatalogLoadError.InvalidPhrase(id, "source")
                else -> null
            }
        }
    }

    /** Solo se llama con un documento validado por [headerErrorOf] y [translationsErrorOf]. */
    private fun toTranslations(document: TranslationCatalogDocument): List<CatalogTranslation> {
        val targetLocale = LanguageTag(checkNotNull(document.targetLocale))
        val sourceLocale = LanguageTag(checkNotNull(document.sourceLocale))
        return document.translations.orEmpty().map { raw ->
            CatalogTranslation(
                id = PhraseId(checkNotNull(raw.id)),
                targetLocale = targetLocale,
                sourceLocale = sourceLocale,
                source = checkNotNull(raw.source),
            )
        }
    }

    private fun isValidLocale(tag: String?): Boolean = tag != null && LanguageTag.isWellFormed(tag)
}

/** Parseo del formato legado de documento único (ADR-0013), pendiente de retirarse en el grupo 4. */
private object LegacyCatalogParsing {

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

/**
 * Cabecera mínima de un documento de catálogo, usada por el sembrado antes de validarlo por completo.
 * [sourceLocale] distingue un documento de traducción de uno de frases destino por su sola presencia
 * en la cabecera (design.md - "Dos tipos de documento JSON"); [isTranslation] lo deriva.
 */
data class CatalogHeader(val schemaVersion: Int, val targetLocale: String, val sourceLocale: String? = null) {

    val isTranslation: Boolean get() = sourceLocale != null
}

@Serializable
private data class CatalogHeaderDocument(
    val schemaVersion: Int? = null,
    val targetLocale: String? = null,
    val sourceLocale: String? = null,
)

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

/** Documento de frases destino (sin `sourceLocale` ni `source`), design.md - "Dos tipos de documento JSON". */
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

/** Documento de traducción (`targetLocale` + `sourceLocale`), design.md - "Dos tipos de documento JSON". */
@Serializable
private data class TranslationCatalogDocument(
    val schemaVersion: Int? = null,
    val targetLocale: String? = null,
    val sourceLocale: String? = null,
    val translations: List<TranslationDocument>? = null,
)

@Serializable
private data class TranslationDocument(
    val id: String? = null,
    val source: String? = null,
)
