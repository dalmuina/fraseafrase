package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import javax.inject.Inject

/** Devuelve el idioma destino activo (spec `user-settings`, "Idioma destino activo"). */
class ObtainActiveLanguage @Inject constructor(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke(): LanguageTag = settingsRepository.activeTargetLocale()
}
