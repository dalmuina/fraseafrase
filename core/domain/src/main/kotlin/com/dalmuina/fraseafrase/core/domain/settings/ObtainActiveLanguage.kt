package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import javax.inject.Inject

/**
 * Devuelve el idioma destino activo, nunca igual al de origen cuando existe otro disponible
 * (spec `user-settings`, "Idioma destino activo").
 */
class ObtainActiveLanguage @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val obtainSourceLanguage: ObtainSourceLanguage,
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(): LanguageTag {
        val stored = settingsRepository.activeTargetLocale()
        val source = obtainSourceLanguage()
        if (stored != source) return stored
        return catalogRepository.availableTargetLocales().firstOrNull { it != source } ?: stored
    }
}
