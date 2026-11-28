package com.dalmuina.fraseafrase.core.domain.catalog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Base language used to match the device language (spec: phrase-catalog)
class LanguageTagTest {

    // Scenario: Otra región del mismo idioma usa el idioma disponible
    @Test
    fun `given a tag with region when reading its language then it returns the language subtag`() {
        // Given
        val tag = LanguageTag("en-US")

        // When
        val result = tag.language

        // Then
        result shouldBe "en"
    }

    // Scenario: Un idioma sin región usa la variante disponible
    @Test
    fun `given a tag without region when reading its language then it returns the whole tag`() {
        // Given
        val tag = LanguageTag("en")

        // When
        val result = tag.language

        // Then
        result shouldBe "en"
    }

    // Scenario: Portugués de Brasil usa pt-PT
    @Test
    fun `given a Brazilian Portuguese tag when reading its language then it returns pt`() {
        // Given
        val tag = LanguageTag("pt-BR")

        // When
        val result = tag.language

        // Then
        result shouldBe "pt"
    }
}
