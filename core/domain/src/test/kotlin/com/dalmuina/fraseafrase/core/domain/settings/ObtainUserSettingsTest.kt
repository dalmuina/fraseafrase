package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObtainUserSettingsTest {

    // Scenario: Valores por defecto (spec: user-settings)
    @Test
    fun `given no settings saved when invoked then returns the repository's settings`() = runTest {
        // Given
        val repository = FakeSettingsRepository()
        val obtainUserSettings = ObtainUserSettings(repository)

        // When
        val result = obtainUserSettings()

        // Then
        result shouldBe UserSettings.DEFAULT
    }
}
