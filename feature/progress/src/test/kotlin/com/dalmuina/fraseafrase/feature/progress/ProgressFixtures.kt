package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.DefaultSourceLocale
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import com.dalmuina.fraseafrase.core.domain.progress.ObtainProgressOverview
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.anObtainCatalogPhrases

/**
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing`. Ningún caso de uso se mockea.
 * [activeTargetLocale] es el idioma destino activo según los ajustes (por defecto
 * [FakeSettingsRepository.DEFAULT_LOCALE]), usado también como clave del catálogo (group 8,
 * `add-multi-language-catalog`).
 */
internal fun progressViewModel(
    catalog: CatalogLoadResult,
    practice: FakePracticeRepository = FakePracticeRepository(),
    settings: UserSettings = UserSettings.DEFAULT,
    activeTargetLocale: LanguageTag = FakeSettingsRepository.DEFAULT_LOCALE,
): ProgressViewModel {
    return progressViewModel(
        settingsRepository = FakeSettingsRepository(settings, defaultActiveTargetLocale = activeTargetLocale),
        catalogRepository = FakeCatalogRepository(catalog, targetLocale = activeTargetLocale),
        practice = practice,
    )
}

/** Variante con los repositorios ya creados, para que un test cambie los ajustes tras abrir la pantalla. */
internal fun progressViewModel(
    settingsRepository: FakeSettingsRepository,
    catalogRepository: FakeCatalogRepository,
    practice: FakePracticeRepository = FakePracticeRepository(),
): ProgressViewModel {
    val obtainUserSettings = ObtainUserSettings(settingsRepository)
    val obtainSourceLanguage = ObtainSourceLanguage(
        FakeDeviceLocaleProvider(LanguageTag("es-ES")),
        catalogRepository,
        DefaultSourceLocale(LanguageTag("es-ES")),
    )
    return ProgressViewModel(
        obtainProgressOverview = ObtainProgressOverview(
            obtainCatalogPhrases = anObtainCatalogPhrases(catalogRepository),
            practiceRepository = practice,
            obtainUserSettings = obtainUserSettings,
        ),
        obtainUserSettings = obtainUserSettings,
        obtainActiveLanguage = ObtainActiveLanguage(settingsRepository, obtainSourceLanguage, catalogRepository),
    )
}
