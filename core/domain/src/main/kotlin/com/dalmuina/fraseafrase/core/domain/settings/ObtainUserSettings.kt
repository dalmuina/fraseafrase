package com.dalmuina.fraseafrase.core.domain.settings

import javax.inject.Inject

/** Devuelve los ajustes del usuario (spec `user-settings`). */
class ObtainUserSettings @Inject constructor(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): UserSettings = settingsRepository.settings()
}
