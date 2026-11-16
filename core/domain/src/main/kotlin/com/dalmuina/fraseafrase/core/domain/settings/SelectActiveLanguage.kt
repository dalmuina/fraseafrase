package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import javax.inject.Inject

/**
 * Cambia el idioma destino activo a [targetLocale] (spec `user-settings`, "Cambio del idioma destino
 * activo"). Valida que [targetLocale] esté entre los idiomas destino disponibles; si no lo está, es un
 * error de programación y lanza [IllegalArgumentException].
 */
class SelectActiveLanguage @Inject constructor(
    private val obtainAvailableLanguages: ObtainAvailableLanguages,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(targetLocale: LanguageTag) {
        val available = obtainAvailableLanguages()
        require(targetLocale in available) { "Target locale ${targetLocale.value} is not available" }
        settingsRepository.setActiveTargetLocale(targetLocale)
    }
}
