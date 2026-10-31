package com.dalmuina.fraseafrase.core.domain.catalog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogValuesTest {

    // Scenario: Campos obligatorios de cada frase (spec: phrase-catalog)
    @Test
    fun `given a blank topic when building it then throws IllegalArgumentException`() {
        // Given
        val blankTopic = "  "

        // When / Then
        shouldThrow<IllegalArgumentException> { Topic(blankTopic) }
    }

    @Test
    fun `given a topic with text when building it then keeps the value`() {
        // Given
        val text = "saludos"

        // When
        val topic = Topic(text)

        // Then
        topic.value shouldBe "saludos"
    }

    // Scenario: Locales por frase (spec: phrase-catalog)
    @Test
    fun `given an empty locale when building it then throws IllegalArgumentException`() {
        // Given
        val empty = ""

        // When / Then
        shouldThrow<IllegalArgumentException> { LanguageTag(empty) }
    }

    @Test
    fun `given a locale with underscore when building it then throws IllegalArgumentException`() {
        // Given
        val underscore = "pt_PT"

        // When / Then
        shouldThrow<IllegalArgumentException> { LanguageTag(underscore) }
    }

    @Test
    fun `given a bcp-47 locale when building it then keeps the value`() {
        // Given
        val tag = "pt-PT"

        // When
        val locale = LanguageTag(tag)

        // Then
        locale.value shouldBe "pt-PT"
    }
}
