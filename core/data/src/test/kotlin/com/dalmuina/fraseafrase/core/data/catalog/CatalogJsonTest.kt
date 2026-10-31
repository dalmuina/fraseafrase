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
}
