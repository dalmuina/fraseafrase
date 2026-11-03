package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings

/**
 * Fake de [SettingsRepository] que devuelve los ajustes configurados (por defecto, los de la spec); [save]
 * reemplaza el valor que devuelve [settings].
 */
class FakeSettingsRepository(initialSettings: UserSettings = UserSettings.DEFAULT) : SettingsRepository {

    private var settings: UserSettings = initialSettings

    override suspend fun settings(): UserSettings = settings

    override suspend fun save(settings: UserSettings) {
        this.settings = settings
    }
}
