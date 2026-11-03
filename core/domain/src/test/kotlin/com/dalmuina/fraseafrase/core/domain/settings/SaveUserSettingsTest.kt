package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SaveUserSettingsTest {

    // Scenario: Guardar sustituye los ajustes anteriores (spec: user-settings)
    @Test
    fun `given a new cefr level and batch size when invoked then the repository receives them`() = runTest {
        // Given
        val repository = FakeSettingsRepository(UserSettings.DEFAULT)
        val saveUserSettings = SaveUserSettings(repository)
        val newSettings = UserSettings(CefrLevel.B2, BatchSize(15))

        // When
        saveUserSettings(newSettings)

        // Then
        repository.settings() shouldBe newSettings
    }
}
