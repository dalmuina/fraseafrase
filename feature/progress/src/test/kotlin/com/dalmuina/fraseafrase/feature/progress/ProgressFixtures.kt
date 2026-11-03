package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.progress.ObtainProgressOverview
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository

/**
 * ViewModel con el caso de uso real sobre los fakes de `:core:testing`. Ningún caso de uso se mockea.
 */
internal fun progressViewModel(
    catalog: CatalogLoadResult,
    practice: FakePracticeRepository = FakePracticeRepository(),
): ProgressViewModel = ProgressViewModel(
    obtainProgressOverview = ObtainProgressOverview(
        catalogRepository = FakeCatalogRepository(catalog),
        practiceRepository = practice,
    ),
)
