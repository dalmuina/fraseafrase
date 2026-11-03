package com.dalmuina.fraseafrase.core.domain.settings

import javax.inject.Inject

/** Guarda los ajustes del usuario, sustituyendo los anteriores (spec `user-settings`). */
class SaveUserSettings @Inject constructor(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(settings: UserSettings) = settingsRepository.save(settings)
}
