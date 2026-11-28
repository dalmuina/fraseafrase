package com.dalmuina.fraseafrase.core.platform

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

class AndroidDeviceLocaleProviderTest {

    // Scenario: Se usa el siguiente idioma de la lista si el primero no casa
    @Test
    fun `given several device locales when preferred is called then they are returned in order`() {
        // Given
        val provider = AndroidDeviceLocaleProvider(
            locales = { listOf(Locale.forLanguageTag("eu-ES"), Locale.forLanguageTag("en-US")) },
        )

        // When
        val result = provider.preferred()

        // Then
        result shouldBe listOf(LanguageTag("eu-ES"), LanguageTag("en-US"))
    }

    // Requirement: Idioma de origen según el dispositivo (resolving never fails)
    @Test
    fun `given a locale with a malformed tag when preferred is called then it is left out`() {
        // Given
        val provider = AndroidDeviceLocaleProvider(
            locales = { listOf(Locale.forLanguageTag("x-private"), Locale.forLanguageTag("pt-PT")) },
        )

        // When
        val result = provider.preferred()

        // Then
        result shouldBe listOf(LanguageTag("pt-PT"))
    }
}
