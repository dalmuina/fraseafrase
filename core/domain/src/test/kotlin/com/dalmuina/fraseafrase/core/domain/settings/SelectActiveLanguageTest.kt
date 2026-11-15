package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Cambio del idioma destino activo (spec: user-settings)
class SelectActiveLanguageTest {

    private val ptPt = LanguageTag("pt-PT")
    private val ptBr = LanguageTag("pt-BR")

    private fun useCase(settingsRepository: FakeSettingsRepository) = SelectActiveLanguage(
        obtainAvailableLanguages = ObtainAvailableLanguages(
            FakeCatalogRepository.withCatalogs(
                mapOf(
                    ptPt to CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "a"))),
                    ptBr to CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "b"))),
                ),
            ),
        ),
        settingsRepository = settingsRepository,
    )

    // Scenario: Cambiar el idioma destino activo
    @Test
    fun `given pt-PT active when selecting pt-BR then a later read returns pt-BR`() = runTest {
        // Given
        val settingsRepository = FakeSettingsRepository(defaultActiveTargetLocale = ptPt)

        // When
        useCase(settingsRepository).invoke(ptBr)

        // Then
        settingsRepository.activeTargetLocale() shouldBe ptBr
    }

    // Escenario de validación: un idioma destino no disponible es un error de programación
    @Test
    fun `given a target locale that is not available when selecting it then it throws`() = runTest {
        // Given
        val settingsRepository = FakeSettingsRepository(defaultActiveTargetLocale = ptPt)

        // When / Then
        shouldThrow<IllegalArgumentException> {
            useCase(settingsRepository).invoke(LanguageTag("en-US"))
        }
    }
}
