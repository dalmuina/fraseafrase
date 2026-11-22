package com.dalmuina.fraseafrase.core.domain.catalog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Resolución del idioma de origen: dispositivo o por defecto (spec: phrase-catalog)
class ResolveSourceLocaleTest {

    private val default = LanguageTag("es-ES")

    // Scenario: El idioma del dispositivo está disponible
    @Test
    fun `given a device locale available in the catalog when resolving then it returns the device locale`() {
        // Given
        val device = LanguageTag("pt-PT")
        val available = listOf(LanguageTag("es-ES"), LanguageTag("pt-PT"))

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("pt-PT")
    }

    // Scenario: El idioma del dispositivo no está disponible
    @Test
    fun `given a device locale not available when resolving then it returns the default`() {
        // Given
        val device = LanguageTag("en-US")
        val available = listOf(LanguageTag("es-ES"), LanguageTag("pt-PT"))

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }
}
