package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Idioma de origen: dispositivo o por defecto (spec: phrase-catalog)
class ObtainSourceLanguageTest {

    private val esEs = LanguageTag("es-ES")
    private val ptPt = LanguageTag("pt-PT")

    private fun catalog() = FakeCatalogRepository.withCatalogs(
        mapOf(esEs to CatalogLoadResult.Loaded(emptyList()), ptPt to CatalogLoadResult.Loaded(emptyList())),
    )

    // Scenario: El idioma de origen no depende del idioma destino activo
    @Test
    fun `given a device locale available when obtaining source then it is the device locale regardless of target`() =
        runTest {
            // Given
            val useCase = ObtainSourceLanguage(
                FakeDeviceLocaleProvider(ptPt),
                catalog(),
                DefaultSourceLocale(esEs),
            )

            // When
            val result = useCase()

            // Then
            result shouldBe ptPt
        }

    // Scenario: El idioma del dispositivo no está disponible
    @Test
    fun `given an unavailable device locale when obtaining source then it is the default`() = runTest {
        // Given
        val useCase = ObtainSourceLanguage(
            FakeDeviceLocaleProvider(LanguageTag("en-US")),
            catalog(),
            DefaultSourceLocale(esEs),
        )

        // When
        val result = useCase()

        // Then
        result shouldBe esEs
    }

    // Scenario: Se usa el siguiente idioma de la lista si el primero no casa
    @Test
    fun `given a first device language without match when obtaining source then the next one is used`() = runTest {
        // Given
        val enGb = LanguageTag("en-GB")
        val useCase = ObtainSourceLanguage(
            FakeDeviceLocaleProvider(listOf(LanguageTag("eu-ES"), LanguageTag("en-US"))),
            FakeCatalogRepository.withCatalogs(
                mapOf(
                    esEs to CatalogLoadResult.Loaded(emptyList()),
                    ptPt to CatalogLoadResult.Loaded(emptyList()),
                    enGb to CatalogLoadResult.Loaded(emptyList()),
                ),
            ),
            DefaultSourceLocale(esEs),
        )

        // When
        val result = useCase()

        // Then
        result shouldBe enGb
    }

    // Scenario: Un cambio del idioma del dispositivo cambia el origen
    @Test
    fun `given a device locale change when obtaining source then the source follows the device`() = runTest {
        // Given
        val device = FakeDeviceLocaleProvider(esEs)
        val useCase = ObtainSourceLanguage(device, catalog(), DefaultSourceLocale(esEs))
        useCase() shouldBe esEs

        // When
        device.locale = ptPt

        // Then
        useCase() shouldBe ptPt
    }
}
