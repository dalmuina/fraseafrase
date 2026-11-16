package com.dalmuina.fraseafrase.core.platform

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

class AndroidDeviceLocaleProviderTest {

    // Scenario: Resolución del idioma de origen por defecto
    @Test
    fun `given a device locale when current is called then it is returned as a language tag`() {
        // Given
        val provider = AndroidDeviceLocaleProvider(locale = { Locale.forLanguageTag("pt-PT") })

        // When
        val result = provider.current()

        // Then
        result shouldBe LanguageTag("pt-PT")
    }
}
