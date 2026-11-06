package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ForceCloseActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.practice.RegenerateActiveBatchForLevel
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import java.time.LocalDate

/** Fecha fija de los tests de presentación. */
internal val TODAY: LocalDate = LocalDate.of(2026, 3, 10)

/** Snapshot con una tanda activa de una sola frase sin certificar, para comprobar que se cierra. */
internal fun activeBatchSnapshot(id: String = "phrase-1"): PracticeSnapshot = PracticeSnapshot(
    progress = listOf(aPhraseProgress(id = id)),
    activeBatch = ActiveBatch(order = listOf(PhraseId(id)), capacity = BatchSize(10)),
    carry = CarryQueue(emptyList()),
)

/**
 * Snapshot con una tanda activa de una sola frase con racha 3 (lista para certificar si se cierra con
 * resolución completa), para distinguir [ForceCloseActiveBatch] (certifica) de
 * [RegenerateActiveBatchForLevel] (no certifica).
 */
internal fun certifiableActiveBatchSnapshot(id: String = "phrase-1", level: Level = Level(1)): PracticeSnapshot =
    PracticeSnapshot(
        progress = listOf(
            aPhraseProgress(
                id = id,
                level = level,
                streak = Streak(3),
                answeredInBatch = true,
                nextReview = TODAY.plusDays(1),
            ),
        ),
        activeBatch = ActiveBatch(order = listOf(PhraseId(id)), capacity = BatchSize(10)),
        carry = CarryQueue(emptyList()),
    )

/**
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing`. Ningún caso de uso se mockea: son
 * clases concretas y su lógica forma parte de lo que se verifica. [practice] por defecto no tiene tanda activa.
 */
internal fun settingsViewModel(
    repository: FakeSettingsRepository,
    practice: FakePracticeRepository = FakePracticeRepository(),
    clock: FixedClock = FixedClock(TODAY),
    catalog: CatalogLoadResult = CatalogLoadResult.Loaded(emptyList()),
): SettingsViewModel =
    SettingsViewModel(
        obtainUserSettings = ObtainUserSettings(repository),
        saveUserSettings = SaveUserSettings(repository),
        forceCloseActiveBatch = ForceCloseActiveBatch(practice, ResolveBatchClosure(clock)),
        regenerateActiveBatchForLevel = RegenerateActiveBatchForLevel(
            practiceRepository = practice,
            composeNextBatch = ComposeNextBatch(clock),
            catalogRepository = FakeCatalogRepository(catalog),
            settingsRepository = repository,
        ),
    )
