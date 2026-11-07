package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.practice.RegenerateActiveBatchForLevel
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress

/** Snapshot con una tanda activa de una sola frase sin progreso previo destacable, para comprobar que se recompone. */
internal fun activeBatchSnapshot(id: String = "phrase-1", sessionCount: Int = 0): PracticeSnapshot = PracticeSnapshot(
    progress = listOf(aPhraseProgress(id = id)),
    activeBatch = ActiveBatch(order = listOf(PhraseId(id)), capacity = BatchSize(10)),
    sessionCount = sessionCount,
)

/**
 * Snapshot con una tanda activa de una sola frase con racha y `dueAtSession` ya avanzados, para comprobar que
 * recomponer la tanda activa por un cambio de ajustes nunca toca su progreso (design D4/D5 de
 * `repeat-by-session-counter`).
 */
internal fun activeBatchSnapshotWithProgress(
    id: String = "phrase-1",
    level: Level = Level(1),
    streak: Streak = Streak(2),
    dueAtSession: Int = 5,
    sessionCount: Int = 0,
): PracticeSnapshot = PracticeSnapshot(
    progress = listOf(
        aPhraseProgress(id = id, level = level, streak = streak, answeredInBatch = true, dueAtSession = dueAtSession),
    ),
    activeBatch = ActiveBatch(order = listOf(PhraseId(id)), capacity = BatchSize(10)),
    sessionCount = sessionCount,
)

/**
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing`. Ningún caso de uso se mockea: son
 * clases concretas y su lógica forma parte de lo que se verifica. [practice] por defecto no tiene tanda activa.
 */
internal fun settingsViewModel(
    repository: FakeSettingsRepository,
    practice: FakePracticeRepository = FakePracticeRepository(),
    catalog: CatalogLoadResult = CatalogLoadResult.Loaded(emptyList()),
): SettingsViewModel =
    SettingsViewModel(
        obtainUserSettings = ObtainUserSettings(repository),
        saveUserSettings = SaveUserSettings(repository),
        regenerateActiveBatchForLevel = RegenerateActiveBatchForLevel(
            practiceRepository = practice,
            composeNextBatch = ComposeNextBatch(),
            catalogRepository = FakeCatalogRepository(catalog),
        ),
    )
