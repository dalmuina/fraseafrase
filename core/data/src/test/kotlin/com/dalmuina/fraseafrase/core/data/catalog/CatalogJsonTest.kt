package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class CatalogJsonTest {

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
}
