package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository

/**
 * ViewModel con los casos de uso reales sobre el fake de `:core:testing`. Ningún caso de uso se mockea.
 */
internal fun settingsViewModel(repository: FakeSettingsRepository): SettingsViewModel =
    SettingsViewModel(
        obtainUserSettings = ObtainUserSettings(repository),
        saveUserSettings = SaveUserSettings(repository),
    )
