package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Composición de ObtainSourceLanguage + CatalogRepository
// (spec: phrase-catalog, "El origen sale del texto destino del otro idioma")
class ObtainCatalogPhrasesForActiveLanguageTest {

    private val ptPt = LanguageTag("pt-PT")
    private val esEs = LanguageTag("es-ES")
    private val enGb = LanguageTag("en-GB")

    private val multiLanguageCatalog = FakeCatalogRepository.withCatalogs(
        mapOf(
            ptPt to CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "a", target = "Bom dia"))),
            esEs to CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "a", target = "Buenos días").copy(targetLocale = esEs)),
            ),
            enGb to CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "a", target = "Good morning").copy(targetLocale = enGb)),
            ),
        ),
    )

    private fun useCase(device: LanguageTag, catalog: FakeCatalogRepository) = ObtainCatalogPhrasesForActiveLanguage(
        obtainSourceLanguage = ObtainSourceLanguage(
            FakeDeviceLocaleProvider(device),
            catalog,
            DefaultSourceLocale(esEs),
        ),
        catalogRepository = catalog,
    )

    // Scenario: El origen sale del texto destino del otro idioma (dispositivo con catálogo)
    @Test
    fun `given device in en-GB with en-GB available when invoked then source is the en-GB text of the same id`() =
        runTest {
            // When
            val result = useCase(enGb, multiLanguageCatalog)(ptPt)

            // Then
            val phrase = result.shouldBeInstanceOf<CatalogLoadResult.Loaded>().phrases.single()
            phrase.source shouldBe "Good morning"
            phrase.sourceLocale shouldBe enGb
            multiLanguageCatalog.lastPhrasesCall shouldBe (ptPt to enGb)
        }

    // Scenario: El origen sale del texto destino del otro idioma (respaldo por defecto)
    @Test
    fun `given device in ja-JP when invoked then source is the es-ES text of the same id`() = runTest {
        // When
        val result = useCase(LanguageTag("ja-JP"), multiLanguageCatalog)(ptPt)

        // Then
        val phrase = result.shouldBeInstanceOf<CatalogLoadResult.Loaded>().phrases.single()
        phrase.source shouldBe "Buenos días"
        multiLanguageCatalog.lastPhrasesCall shouldBe (ptPt to esEs)
    }

    // Scenario: Un catálogo de un solo idioma conserva el source de sus frases
    @Test
    fun `given a single language fake when invoked then the source of the phrases is kept`() = runTest {
        // Given
        val catalog = FakeCatalogRepository(
            CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "a", source = "Hola"))),
            ptPt,
        )

        // When
        val result = useCase(esEs, catalog)(ptPt)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>().phrases.single().source shouldBe "Hola"
    }
}
