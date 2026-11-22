package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import javax.inject.Inject

/**
 * Cambia el idioma destino activo a [targetLocale] (spec `user-settings`, "Cambio del idioma destino
 * activo"). Primero valida que [targetLocale] esté entre los idiomas destino disponibles; si no lo está,
 * es un error de programación y lanza [IllegalArgumentException]. Después, si coincide con el idioma de origen vigente
 * devuelve [SelectLanguageResult.RejectedSourceLocale] sin guardar nada ("No se puede elegir como destino
 * el idioma de origen"); no lanza porque el origen puede cambiar con Ajustes abiertos.
 */
class SelectActiveLanguage @Inject constructor(
    private val obtainAvailableLanguages: ObtainAvailableLanguages,
    private val obtainSourceLanguage: ObtainSourceLanguage,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(targetLocale: LanguageTag): SelectLanguageResult {
        val available = obtainAvailableLanguages()
        require(targetLocale in available) { "Target locale ${targetLocale.value} is not available" }
        if (targetLocale == obtainSourceLanguage()) return SelectLanguageResult.RejectedSourceLocale
        settingsRepository.setActiveTargetLocale(targetLocale)
        return SelectLanguageResult.Selected
    }
}
