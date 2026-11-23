package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Idioma destino activo y ajustes por idioma destino (spec: user-settings)
class SettingsRepositoryTest {

    private val ptPt = LanguageTag("pt-PT")
    private val enGb = LanguageTag("en-GB")

    // Scenario: Un idioma destino nuevo no hereda los ajustes de otro
    @Test
    fun `given settings saved for one target locale when reading another then it returns the defaults`() = runTest {
        // Given
        val repository = FakeSettingsRepository()
        repository.save(ptPt, UserSettings(CefrLevel.B1, BatchSize(25)))

        // When
        val result = repository.settings(enGb)

        // Then
        result shouldBe UserSettings.DEFAULT
    }

    // Scenario: Los ajustes de dos idiomas destino no se mezclan
    @Test
    fun `given settings saved for two target locales when reading each then they do not mix`() = runTest {
        // Given
        val repository = FakeSettingsRepository()
        repository.save(ptPt, UserSettings(CefrLevel.B1, BatchSize(25)))
        repository.save(enGb, UserSettings(CefrLevel.A2, BatchSize(10)))

        // When / Then
        repository.settings(ptPt) shouldBe UserSettings(CefrLevel.B1, BatchSize(25))
        repository.settings(enGb) shouldBe UserSettings(CefrLevel.A2, BatchSize(10))
    }

    // Scenario: Primera ejecución usa el primer idioma destino disponible
    @Test
    fun `given no active target locale saved when reading it then it returns the configured default`() = runTest {
        // Given
        val repository = FakeSettingsRepository(defaultActiveTargetLocale = ptPt)

        // When
        val result = repository.activeTargetLocale()

        // Then
        result shouldBe ptPt
    }

    // Scenario: Cambiar el idioma destino activo
    @Test
    fun `given an active target locale when changing it then a later read returns the new one`() = runTest {
        // Given
        val repository = FakeSettingsRepository(defaultActiveTargetLocale = ptPt)

        // When
        repository.setActiveTargetLocale(enGb)

        // Then
        repository.activeTargetLocale() shouldBe enGb
    }
}
