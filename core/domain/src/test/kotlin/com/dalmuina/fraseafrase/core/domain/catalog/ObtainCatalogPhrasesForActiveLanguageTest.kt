package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Composición de DeviceLocaleProvider + ResolveSourceLocale + CatalogRepository
// (spec: phrase-catalog, "Resolución del idioma de origen por defecto")
class ObtainCatalogPhrasesForActiveLanguageTest {

    private val targetLocale = LanguageTag("pt-PT")
    private val catalog = FakeCatalogRepository(
        CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "a"))),
        targetLocale,
    )

    // Scenario: El locale del dispositivo coincide con el origen disponible
    @Test
    fun `given a device locale available in the catalog when invoked then it resolves the device locale`() =
        runTest {
            // Given
            val useCase = ObtainCatalogPhrasesForActiveLanguage(
                deviceLocaleProvider = FakeDeviceLocaleProvider(LanguageTag("es-ES")),
                catalogRepository = catalog,
            )

            // When
            val result = useCase(targetLocale)

            // Then
            result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
            catalog.lastPhrasesCall shouldBe (targetLocale to LanguageTag("es-ES"))
        }

    // Scenario: El locale del dispositivo no tiene contenido disponible
    @Test
    fun `given a device locale without available content when invoked then it falls back to es-ES`() = runTest {
        // Given
        val useCase = ObtainCatalogPhrasesForActiveLanguage(
            deviceLocaleProvider = FakeDeviceLocaleProvider(LanguageTag("en-US")),
            catalogRepository = catalog,
        )

        // When
        val result = useCase(targetLocale)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        catalog.lastPhrasesCall shouldBe (targetLocale to LanguageTag("es-ES"))
    }

    // Scenario: El respaldo no coincide con el propio idioma destino
    @Test
    fun `given a target locale matching an available source when invoked then it does not resolve to itself`() =
        runTest {
            // Given
            val esEsTargetLocale = LanguageTag("es-ES")
            val ptPtSourceLocale = LanguageTag("pt-PT")
            val catalogWithSelfMatchingSource = FakeCatalogRepository(
                CatalogLoadResult.Loaded(
                    listOf(
                        aCatalogPhrase(id = "a").copy(
                            targetLocale = esEsTargetLocale,
                            sourceLocale = esEsTargetLocale,
                        ),
                        aCatalogPhrase(id = "b").copy(
                            targetLocale = esEsTargetLocale,
                            sourceLocale = ptPtSourceLocale,
                        ),
                    ),
                ),
                esEsTargetLocale,
            )
            val useCase = ObtainCatalogPhrasesForActiveLanguage(
                deviceLocaleProvider = FakeDeviceLocaleProvider(esEsTargetLocale),
                catalogRepository = catalogWithSelfMatchingSource,
            )

            // When
            useCase(esEsTargetLocale)

            // Then
            catalogWithSelfMatchingSource.lastPhrasesCall shouldBe (esEsTargetLocale to ptPtSourceLocale)
        }
}
