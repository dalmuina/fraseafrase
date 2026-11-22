package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.DefaultSourceLocale
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainAvailableLanguages
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SelectActiveLanguage
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository

/**
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing`. Ningún caso de uso se mockea: son
 * clases concretas y su lógica forma parte de lo que se verifica. [availableTargetLocales] configura los
 * idiomas destino disponibles (por defecto, solo [FakeSettingsRepository.DEFAULT_LOCALE]), cada uno con el
 * mismo [catalog]. Desde `remove-settings-save-button` (design D3), el `ViewModel` ya no orquesta ninguna
 * recomposición de la tanda activa: eso ocurre al obtener la tanda activa (`practice-persistence`), no aquí.
 */
internal fun settingsViewModel(
    repository: FakeSettingsRepository,
    catalog: CatalogLoadResult = CatalogLoadResult.Loaded(emptyList()),
    availableTargetLocales: List<LanguageTag> = listOf(FakeSettingsRepository.DEFAULT_LOCALE),
    deviceLocale: FakeDeviceLocaleProvider = FakeDeviceLocaleProvider(LanguageTag("es-ES")),
): SettingsViewModel {
    val catalogRepository = FakeCatalogRepository.withCatalogs(
        availableTargetLocales.associateWith { catalog },
    )
    val obtainAvailableLanguages = ObtainAvailableLanguages(catalogRepository)
    val obtainSourceLanguage = ObtainSourceLanguage(
        deviceLocale,
        catalogRepository,
        DefaultSourceLocale(LanguageTag("es-ES")),
    )
    return SettingsViewModel(
        obtainUserSettings = ObtainUserSettings(repository),
        saveUserSettings = SaveUserSettings(repository),
        obtainActiveLanguage = ObtainActiveLanguage(repository, obtainSourceLanguage, catalogRepository),
        obtainAvailableLanguages = obtainAvailableLanguages,
        selectActiveLanguage = SelectActiveLanguage(obtainAvailableLanguages, obtainSourceLanguage, repository),
        obtainSourceLanguage = obtainSourceLanguage,
    )
}
