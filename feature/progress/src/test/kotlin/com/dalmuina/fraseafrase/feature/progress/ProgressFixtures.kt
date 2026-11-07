package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.progress.ObtainProgressOverview
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository

/**
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing`. Ningún caso de uso se mockea.
 */
internal fun progressViewModel(
    catalog: CatalogLoadResult,
    practice: FakePracticeRepository = FakePracticeRepository(),
    settings: UserSettings = UserSettings.DEFAULT,
): ProgressViewModel = ProgressViewModel(
    obtainProgressOverview = ObtainProgressOverview(
        catalogRepository = FakeCatalogRepository(catalog),
        practiceRepository = practice,
    ),
    obtainUserSettings = ObtainUserSettings(FakeSettingsRepository(settings)),
)
