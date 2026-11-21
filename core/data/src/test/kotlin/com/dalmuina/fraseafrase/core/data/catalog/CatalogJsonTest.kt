package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class CatalogJsonTest {

    private val validPhrase = """
        { "id": "a1-saludos-001", "level": "A1", "topic": "saludos",
          "target": "Bom dia, tudo bem?", "source": "Buenos días, ¿qué tal?" }
    """.trimIndent()

    private fun document(
        schemaVersion: String = "\"schemaVersion\": 1,",
        targetLocale: String = "\"targetLocale\": \"pt-PT\",",
        sourceLocale: String = "\"sourceLocale\": \"es-ES\",",
        phrases: String = "[ $validPhrase ]",
    ) = """
        {
          $schemaVersion
          $targetLocale
          $sourceLocale
          "phrases": $phrases
        }
    """.trimIndent()

    // Scenario: Formato del catálogo (spec: phrase-catalog)
    @Test
    fun `given a valid document when parsing then returns the phrases in catalog order`() {
        // When
        val result = CatalogJson.parse(document())

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.map { it.id } shouldBe listOf(PhraseId("a1-saludos-001"))
        result.phrases.single().level shouldBe CefrLevel.A1
        result.phrases.single().topic shouldBe Topic("saludos")
    }

    // Scenario: Frase hereda los locales de la raíz (spec: phrase-catalog, Locales por frase)
    @Test
    fun `given root locales when parsing then every phrase inherits them`() {
        // When
        val phrase = (CatalogJson.parse(document()) as CatalogLoadResult.Loaded).phrases.single()

        // Then
        phrase.targetLocale shouldBe LanguageTag("pt-PT")
        phrase.sourceLocale shouldBe LanguageTag("es-ES")
    }

    // Scenario: Esquema desconocido se rechaza (spec: phrase-catalog, Formato del catálogo)
    @Test
    fun `given schemaVersion 2 when parsing then fails with UnsupportedSchemaVersion`() {
        // When
        val result = CatalogJson.parse(document(schemaVersion = "\"schemaVersion\": 2,"))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.UnsupportedSchemaVersion(found = 2))
    }

    // Scenario: Sin schemaVersion rechaza la carga (spec: phrase-catalog, Versión del esquema ausente)
    @Test
    fun `given no schemaVersion when parsing then fails with MissingSchemaVersion`() {
        // When
        val result = CatalogJson.parse(document(schemaVersion = ""))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.MissingSchemaVersion)
    }

    // Scenario: Locale vacío rechaza la carga (spec: phrase-catalog, Locales por frase)
    @Test
    fun `given an empty target locale when parsing then fails with InvalidLocale`() {
        // When
        val result = CatalogJson.parse(document(targetLocale = "\"targetLocale\": \"\","))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidLocale("targetLocale"))
    }

    // Scenario: Locale con formato no BCP-47 rechaza la carga (spec: phrase-catalog)
    @Test
    fun `given a locale with underscore when parsing then fails with InvalidLocale`() {
        // When
        val result = CatalogJson.parse(document(targetLocale = "\"targetLocale\": \"pt_PT\","))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidLocale("targetLocale"))
    }

    // Scenario: Locale ausente rechaza la carga (spec: phrase-catalog)
    @Test
    fun `given no source locale when parsing then fails with InvalidLocale`() {
        // When
        val result = CatalogJson.parse(document(sourceLocale = ""))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidLocale("sourceLocale"))
    }

    // Scenario: Campos obligatorios de cada frase (spec: phrase-catalog)
    @Test
    fun `given a level outside A1 to C1 when parsing then fails with InvalidPhrase on level`() {
        // Given
        val phrase = validPhrase.replace("\"A1\"", "\"D1\"")

        // When
        val result = CatalogJson.parse(document(phrases = "[ $phrase ]"))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidPhrase("a1-saludos-001", "level"))
    }

    // Scenario: Texto vacío rechaza la carga (spec: phrase-catalog)
    @Test
    fun `given an empty target text when parsing then fails with InvalidPhrase on target`() {
        // Given
        val phrase = validPhrase.replace("\"Bom dia, tudo bem?\"", "\"\"")

        // When
        val result = CatalogJson.parse(document(phrases = "[ $phrase ]"))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidPhrase("a1-saludos-001", "target"))
    }

    // Scenario: Topic vacío rechaza la carga (spec: phrase-catalog)
    @Test
    fun `given an empty topic when parsing then fails with InvalidPhrase on topic`() {
        // Given
        val phrase = validPhrase.replace("\"saludos\"", "\"\"")

        // When
        val result = CatalogJson.parse(document(phrases = "[ $phrase ]"))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidPhrase("a1-saludos-001", "topic"))
    }

    // Scenario: Ids duplicados rechazan la carga (spec: phrase-catalog, Ids únicos y no vacíos)
    @Test
    fun `given two phrases with the same id when parsing then fails with DuplicateId`() {
        // When
        val result = CatalogJson.parse(document(phrases = "[ $validPhrase, $validPhrase ]"))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.DuplicateId("a1-saludos-001"))
    }

    // Scenario: Id vacío rechaza la carga (spec: phrase-catalog, Ids únicos y no vacíos)
    @Test
    fun `given an empty id when parsing then fails with InvalidPhrase on id`() {
        // Given
        val phrase = validPhrase.replace("\"a1-saludos-001\"", "\"\"")

        // When
        val result = CatalogJson.parse(document(phrases = "[ $phrase ]"))

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.InvalidPhrase("", "id"))
    }

    // Caso de implementación: JSON mal formado (no contemplado en la spec; ver tasks.md 6.1)
    @Test
    fun `given text that is not json when parsing then fails with MalformedDocument`() {
        // When
        val result = CatalogJson.parse("esto no es json")

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.MalformedDocument)
    }

    private val targetPhrase = """
        { "id": "a1-saludos-001", "level": "A1", "topic": "saludos", "target": "Bom dia, tudo bem?" }
    """.trimIndent()

    private fun phraseDocument(
        schemaVersion: String = "\"schemaVersion\": 1,",
        targetLocale: String = "\"targetLocale\": \"pt-PT\",",
        phrases: String = "[ $targetPhrase ]",
    ) = """
        {
          $schemaVersion
          $targetLocale
          "phrases": $phrases
        }
    """.trimIndent()

    // Scenario: Frase hereda los locales de la raíz (spec: phrase-catalog, Locales por frase)
    @Test
    fun `given a phrase document without sourceLocale when parsing then every phrase inherits targetLocale`() {
        // When
        val result = CatalogJson.parsePhrases(phraseDocument())

        // Then
        result.shouldBeInstanceOf<CatalogParseResult.PhrasesParsed>()
        result.phrases.single().targetLocale shouldBe LanguageTag("pt-PT")
    }

    private val translation = """
        { "id": "a1-saludos-001", "source": "Buenos días, ¿qué tal?" }
    """.trimIndent()

    private fun translationDocument(
        schemaVersion: String = "\"schemaVersion\": 1,",
        targetLocale: String = "\"targetLocale\": \"pt-PT\",",
        sourceLocale: String = "\"sourceLocale\": \"es-ES\",",
        translations: String = "[ $translation ]",
    ) = """
        {
          $schemaVersion
          $targetLocale
          $sourceLocale
          "translations": $translations
        }
    """.trimIndent()

    // Scenario: Traducción hereda los locales de la raíz (spec: phrase-catalog, Locales por frase)
    @Test
    fun `given a valid translation document when parsing then every translation inherits target and source locale`() {
        // When
        val result = CatalogJson.parseTranslations(translationDocument())

        // Then
        result.shouldBeInstanceOf<CatalogParseResult.TranslationsParsed>()
        val translation = result.translations.single()
        translation.targetLocale shouldBe LanguageTag("pt-PT")
        translation.sourceLocale shouldBe LanguageTag("es-ES")
    }

    // Scenario: Sin schemaVersion rechaza la carga (spec: phrase-catalog, Versión del esquema ausente)
    @Test
    fun `given no schemaVersion when parsing a translation document then fails with MissingSchemaVersion`() {
        // When
        val result = CatalogJson.parseTranslations(translationDocument(schemaVersion = ""))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.MissingSchemaVersion)
    }

    // Scenario: Esquema desconocido se rechaza (spec: phrase-catalog, Formato del catálogo)
    @Test
    fun `given schemaVersion 2 when parsing a translation document then fails with UnsupportedSchemaVersion`() {
        // When
        val result = CatalogJson.parseTranslations(translationDocument(schemaVersion = "\"schemaVersion\": 2,"))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.UnsupportedSchemaVersion(found = 2))
    }

    // Scenario: Locale vacío rechaza la carga (spec: phrase-catalog, Locales por frase)
    @Test
    fun `given an empty target locale when parsing a translation document then fails with InvalidLocale`() {
        // When
        val result = CatalogJson.parseTranslations(translationDocument(targetLocale = "\"targetLocale\": \"\","))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.InvalidLocale("targetLocale"))
    }

    // Scenario: Locale ausente rechaza la carga (spec: phrase-catalog, Locales por frase)
    @Test
    fun `given no source locale when parsing a translation document then fails with InvalidLocale`() {
        // When
        val result = CatalogJson.parseTranslations(translationDocument(sourceLocale = ""))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.InvalidLocale("sourceLocale"))
    }

    // Scenario: Locale con formato no BCP-47 rechaza la carga (spec: phrase-catalog)
    @Test
    fun `given a source locale with underscore when parsing a translation document then fails with InvalidLocale`() {
        // When
        val result = CatalogJson.parseTranslations(
            translationDocument(sourceLocale = "\"sourceLocale\": \"es_ES\","),
        )

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.InvalidLocale("sourceLocale"))
    }

    // Scenario: Id vacío rechaza la carga (spec: phrase-catalog, Ids únicos y no vacíos)
    @Test
    fun `given an empty id when parsing a translation document then fails with InvalidPhrase on id`() {
        // Given
        val raw = translation.replace("\"a1-saludos-001\"", "\"\"")

        // When
        val result = CatalogJson.parseTranslations(translationDocument(translations = "[ $raw ]"))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.InvalidPhrase("", "id"))
    }

    // Scenario: Texto de traducción vacío rechaza la carga (spec: phrase-catalog, Campos obligatorios de cada frase)
    @Test
    fun `given an empty source text when parsing a translation document then fails with InvalidPhrase on source`() {
        // Given
        val raw = translation.replace("\"Buenos días, ¿qué tal?\"", "\"\"")

        // When
        val result = CatalogJson.parseTranslations(translationDocument(translations = "[ $raw ]"))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.InvalidPhrase("a1-saludos-001", "source"))
    }

    // Scenario: Ids duplicados rechazan la carga de un documento de traducción (spec: phrase-catalog)
    @Test
    fun `given two translations with the same id when parsing then fails with DuplicateId`() {
        // When
        val result = CatalogJson.parseTranslations(translationDocument(translations = "[ $translation, $translation ]"))

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.DuplicateId("a1-saludos-001"))
    }

    // Caso de implementación: JSON mal formado (mismo motivo que en el documento de frases destino)
    @Test
    fun `given text that is not json when parsing a translation document then fails with MalformedDocument`() {
        // When
        val result = CatalogJson.parseTranslations("esto no es json")

        // Then
        result shouldBe CatalogParseResult.Failed(CatalogLoadError.MalformedDocument)
    }

    // Scenario: Formato del catálogo (spec: phrase-catalog) - headerOf() distingue el tipo de documento
    @Test
    fun `given a translation document when reading the header then isTranslation is true`() {
        // When
        val header = CatalogJson.headerOf(translationDocument())

        // Then
        header?.isTranslation shouldBe true
    }

    // Scenario: Formato del catálogo (spec: phrase-catalog) - headerOf() distingue el tipo de documento
    @Test
    fun `given a phrase document when reading the header then isTranslation is false`() {
        // When
        val header = CatalogJson.headerOf(phraseDocument())

        // Then
        header?.isTranslation shouldBe false
    }
}
