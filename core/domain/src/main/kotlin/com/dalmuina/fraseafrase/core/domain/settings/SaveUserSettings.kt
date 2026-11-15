package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import javax.inject.Inject

/** Guarda los ajustes de [targetLocale], sustituyendo los anteriores (spec `user-settings`). */
class SaveUserSettings @Inject constructor(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(targetLocale: LanguageTag, settings: UserSettings) =
        settingsRepository.save(targetLocale, settings)
}
