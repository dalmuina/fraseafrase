package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.DefaultSourceLocale
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Idioma destino activo distinto del origen (spec: user-settings, "Idioma destino activo")
class ObtainActiveLanguageTest {

    private val esEs = LanguageTag("es-ES")
    private val ptPt = LanguageTag("pt-PT")
    private val enGb = LanguageTag("en-GB")

    private fun useCase(stored: LanguageTag, device: LanguageTag, targets: List<LanguageTag>): ObtainActiveLanguage {
        val loaded = CatalogLoadResult.Loaded(emptyList())
        val catalog = FakeCatalogRepository.withCatalogs(targets.associateWith { loaded })
        return ObtainActiveLanguage(
            FakeSettingsRepository(defaultActiveTargetLocale = stored),
            ObtainSourceLanguage(FakeDeviceLocaleProvider(device), catalog, DefaultSourceLocale(esEs)),
            catalog,
        )
    }

    // Scenario: destino guardado distinto del origen se recupera
    @Test
    fun `given a stored target different from the source when obtaining then it is returned`() = runTest {
        // Given
        val obtain = useCase(stored = ptPt, device = esEs, targets = listOf(esEs, ptPt))

        // When / Then
        obtain() shouldBe ptPt
    }

    // Scenario: Primera ejecución salta el idioma de origen
    @Test
    fun `given first run whose default equals the source when obtaining then it skips the source`() = runTest {
        // Given
        val obtain = useCase(stored = esEs, device = esEs, targets = listOf(esEs, ptPt))

        // When / Then
        obtain() shouldBe ptPt
    }

    // Scenario: Un idioma destino guardado igual al origen usa el primero distinto del origen
    @Test
    fun `given a stored target equal to the source when obtaining then the first different one is used`() = runTest {
        // Given
        val obtain = useCase(stored = ptPt, device = ptPt, targets = listOf(ptPt, enGb, esEs))

        // When / Then
        obtain() shouldBe enGb
    }
}
