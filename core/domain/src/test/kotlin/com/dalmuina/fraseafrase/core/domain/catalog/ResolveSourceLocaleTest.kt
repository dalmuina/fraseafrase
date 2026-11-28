package com.dalmuina.fraseafrase.core.domain.catalog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Source language resolution from the ordered device language list (spec: phrase-catalog)
class ResolveSourceLocaleTest {

    private val default = LanguageTag("es-ES")

    // Scenario: El idioma del dispositivo está disponible
    @Test
    fun `given a device language available when resolving then it returns that language`() {
        // Given
        val device = tags("fr-FR")
        val available = tags("pt-PT", "fr-FR", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("fr-FR")
    }

    // Scenario: El idioma del dispositivo no está disponible
    @Test
    fun `given a device language not available when resolving then it returns the default`() {
        // Given
        val device = tags("ja-JP")
        val available = tags("pt-PT", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }

    // Scenario: Otra región del mismo idioma usa el idioma disponible
    @Test
    fun `given another region of an available language when resolving then it returns the available one`() {
        // Given
        val device = tags("en-US")
        val available = tags("pt-PT", "en-GB", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("en-GB")
    }

    // Scenario: Español de otra región usa es-ES
    @Test
    fun `given Mexican Spanish when resolving then it returns es-ES`() {
        // Given
        val device = tags("es-MX")
        val available = tags("pt-PT", "en-GB", "es-ES")

        // When
        val result = ResolveSourceLocale(device, LanguageTag("en-GB"), available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }

    // Scenario: Portugués de Brasil usa pt-PT
    @Test
    fun `given Brazilian Portuguese without a pt-BR catalog when resolving then it returns pt-PT`() {
        // Given
        val device = tags("pt-BR")
        val available = tags("pt-PT", "en-GB", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("pt-PT")
    }

    // Scenario: Un idioma sin región usa la variante disponible
    @Test
    fun `given a device language without region when resolving then it returns the available variant`() {
        // Given
        val device = tags("en")
        val available = tags("pt-PT", "en-GB", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("en-GB")
    }

    // Scenario: La coincidencia exacta gana a la del mismo idioma
    @Test
    fun `given an exact match and another variant when resolving then the exact match wins`() {
        // Given
        val device = tags("pt-PT")
        val available = tags("pt-PT", "pt-BR", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("pt-PT")
    }

    // Scenario: Varias variantes del mismo idioma se desempatan por orden alfabético
    @Test
    fun `given several variants of the same language when resolving then the first alphabetically wins`() {
        // Given
        val device = tags("pt-AO")
        val available = tags("pt-PT", "pt-BR", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("pt-BR")
    }

    // Scenario: Se usa el siguiente idioma de la lista si el primero no casa
    @Test
    fun `given a first device language without match when resolving then the next one is used`() {
        // Given
        val device = tags("eu-ES", "en-US")
        val available = tags("pt-PT", "en-GB", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("en-GB")
    }

    // Scenario: El primer idioma de la lista gana aunque solo case por idioma base
    @Test
    fun `given a base language match before an exact match when resolving then the first in the list wins`() {
        // Given
        val device = tags("en-US", "es-ES")
        val available = tags("pt-PT", "en-GB", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("en-GB")
    }

    // Scenario: Ningún idioma de la lista casa
    @Test
    fun `given no device language matching when resolving then it returns the default`() {
        // Given
        val device = tags("ja-JP", "eu-ES")
        val available = tags("pt-PT", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }

    // Scenario: Una lista de idiomas vacía usa el idioma por defecto
    @Test
    fun `given an empty device language list when resolving then it returns the default`() {
        // Given
        val device = emptyList<LanguageTag>()
        val available = tags("pt-PT", "es-ES")

        // When
        val result = ResolveSourceLocale(device, default, available)

        // Then
        result shouldBe LanguageTag("es-ES")
    }

    private fun tags(vararg values: String): List<LanguageTag> = values.map(::LanguageTag)
}
