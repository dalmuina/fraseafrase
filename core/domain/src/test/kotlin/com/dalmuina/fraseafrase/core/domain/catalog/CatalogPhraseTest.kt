package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogPhraseTest {

    // Scenario: Campos obligatorios de cada frase (spec: phrase-catalog)
    @Test
    fun `given a complete catalog phrase when building it then keeps every field`() {
        // Given
        val id = PhraseId("a1-saludos-001")

        // When
        val phrase = CatalogPhrase(
            id = id,
            level = CefrLevel.A1,
            topic = Topic("saludos"),
            targetLocale = LanguageTag("pt-PT"),
            sourceLocale = LanguageTag("es-ES"),
            target = "Bom dia, tudo bem?",
            source = "Buenos días, ¿qué tal?",
        )

        // Then
        phrase.id shouldBe id
        phrase.level shouldBe CefrLevel.A1
        phrase.topic shouldBe Topic("saludos")
        phrase.targetLocale shouldBe LanguageTag("pt-PT")
        phrase.sourceLocale shouldBe LanguageTag("es-ES")
        phrase.target shouldBe "Bom dia, tudo bem?"
        phrase.source shouldBe "Buenos días, ¿qué tal?"
    }

    // Scenario: Campos obligatorios de cada frase, texto vacío (spec: phrase-catalog)
    @Test
    fun `given a blank target text when building the phrase then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            CatalogPhrase(
                id = PhraseId("a1-saludos-001"),
                level = CefrLevel.A1,
                topic = Topic("saludos"),
                targetLocale = LanguageTag("pt-PT"),
                sourceLocale = LanguageTag("es-ES"),
                target = "",
                source = "Buenos días",
            )
        }
    }
}
