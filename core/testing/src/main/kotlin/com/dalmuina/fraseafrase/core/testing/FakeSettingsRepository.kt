package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings

/**
 * Fake de [SettingsRepository] con ajustes independientes por idioma destino y un idioma destino activo.
 * [initialSettings] configura los ajustes de [DEFAULT_LOCALE]; [defaultActiveTargetLocale] es el idioma
 * destino activo cuando no se ha cambiado con [setActiveTargetLocale]. Los métodos sin `targetLocale` son
 * un atajo para los tests que solo ejercitan [DEFAULT_LOCALE].
 */
class FakeSettingsRepository(
    initialSettings: UserSettings = UserSettings.DEFAULT,
    private val defaultActiveTargetLocale: LanguageTag = DEFAULT_LOCALE,
) : SettingsRepository {

    private val settingsByLocale = mutableMapOf(DEFAULT_LOCALE to initialSettings)
    private var activeLocale: LanguageTag? = null

    override suspend fun settings(targetLocale: LanguageTag): UserSettings =
        settingsByLocale[targetLocale] ?: UserSettings.DEFAULT

    override suspend fun save(targetLocale: LanguageTag, settings: UserSettings) {
        settingsByLocale[targetLocale] = settings
    }

    override suspend fun activeTargetLocale(): LanguageTag = activeLocale ?: defaultActiveTargetLocale

    override suspend fun setActiveTargetLocale(targetLocale: LanguageTag) {
        activeLocale = targetLocale
    }

    /** Atajo para los tests que solo ejercitan [DEFAULT_LOCALE]. */
    suspend fun settings(): UserSettings = settings(DEFAULT_LOCALE)

    /** Atajo para los tests que solo ejercitan [DEFAULT_LOCALE]. */
    suspend fun save(settings: UserSettings) = save(DEFAULT_LOCALE, settings)

    companion object {
        val DEFAULT_LOCALE: LanguageTag = LanguageTag("pt-PT")
    }
}
