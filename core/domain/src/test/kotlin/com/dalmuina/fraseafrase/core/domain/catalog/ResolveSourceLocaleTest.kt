package com.dalmuina.fraseafrase.core.domain.catalog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Resolución del idioma de origen por defecto (spec: phrase-catalog, ADR-0019)
class ResolveSourceLocaleTest {

    // Scenario: El locale del dispositivo coincide con el origen disponible
    @Test
    fun `given a device locale available in the catalog when resolving then it returns the device locale`() {
        // Given
        val deviceLocale = LanguageTag("es-ES")
        val available = listOf(LanguageTag("es-ES"))

        // When
        val result = ResolveSourceLocale(deviceLocale, available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }

    // Scenario: El locale del dispositivo no tiene contenido disponible
    @Test
    fun `given a device locale without available content when resolving then it falls back to es-ES`() {
        // Given
        val deviceLocale = LanguageTag("en-US")
        val available = listOf(LanguageTag("es-ES"))

        // When
        val result = ResolveSourceLocale(deviceLocale, available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }
}
