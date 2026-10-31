package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings

/** Fake de [SettingsRepository] que devuelve siempre los ajustes configurados (por defecto, los de la spec). */
class FakeSettingsRepository(private val settings: UserSettings = UserSettings.DEFAULT) : SettingsRepository {

    override suspend fun settings(): UserSettings = settings
}
