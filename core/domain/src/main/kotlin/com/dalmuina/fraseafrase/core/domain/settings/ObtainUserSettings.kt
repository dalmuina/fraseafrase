package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import javax.inject.Inject

/** Devuelve los ajustes del usuario de [targetLocale] (spec `user-settings`). */
class ObtainUserSettings @Inject constructor(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(targetLocale: LanguageTag): UserSettings = settingsRepository.settings(targetLocale)
}
