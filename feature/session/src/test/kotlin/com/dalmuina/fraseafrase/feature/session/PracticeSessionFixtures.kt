package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.AnswerPhrase
import com.dalmuina.fraseafrase.core.domain.practice.CloseActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

/** Fecha fija de los tests de presentación. */
internal val TODAY: LocalDate = LocalDate.of(2026, 3, 10)

/** Snapshot con una tanda activa de frases [order], en ese orden, y el progreso dado (por defecto, sin respuestas). */
internal fun activeBatchSnapshot(
    order: List<String>,
    progress: List<PhraseProgress> = order.map { aPhraseProgress(id = it) },
): PracticeSnapshot = PracticeSnapshot(
    progress = progress,
    activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
    carry = CarryQueue(emptyList()),
)

internal fun loaded(vararg phrases: CatalogPhrase): CatalogLoadResult.Loaded =
    CatalogLoadResult.Loaded(phrases.toList())

/** Catálogo con una frase de prueba por cada id, en el orden dado. */
internal fun catalogOf(vararg ids: String): CatalogLoadResult.Loaded =
    loaded(*ids.map { aCatalogPhrase(id = it) }.toTypedArray())

/** Orden de ids de la tanda activa guardada, para comprobar la persistencia tras una acción. */
internal suspend fun FakePracticeRepository.activeOrder(): List<String> =
    read().activeBatch?.order.orEmpty().map { it.value }

/** Racha guardada de la frase [id]. */
internal suspend fun FakePracticeRepository.streakOf(id: String): Streak =
    read().progress.single { it.id == PhraseId(id) }.streak

/** Estado actual, que debe ser `Practicing`; si no lo es, el test falla con la aserción de tipo. */
internal fun PracticeSessionViewModel.practicing(): PracticeSessionUiState.Practicing =
    uiState.value.shouldBeInstanceOf<PracticeSessionUiState.Practicing>()

/**
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing` y un reloj fijo.
 * Ningún caso de uso se mockea: son clases concretas y su lógica forma parte de lo que se verifica.
 */
internal fun sessionViewModel(
    practice: FakePracticeRepository,
    catalog: CatalogLoadResult,
    settings: UserSettings = UserSettings.DEFAULT,
    clock: FixedClock = FixedClock(TODAY),
): PracticeSessionViewModel = sessionViewModelWith(practice, FakeCatalogRepository(catalog), settings, clock)

/** Catálogo cuyo resultado puede cambiar entre lecturas, para simular un catálogo que deja de cargarse. */
internal class SwitchableCatalogRepository(var result: CatalogLoadResult) : CatalogRepository {
    override suspend fun phrases(): CatalogLoadResult = result
}

/** Igual que [sessionViewModel], pero con un catálogo cualquiera, por ejemplo uno que cambia durante el test. */
internal fun sessionViewModelWith(
    practice: FakePracticeRepository,
    catalogRepository: CatalogRepository,
    settings: UserSettings = UserSettings.DEFAULT,
    clock: FixedClock = FixedClock(TODAY),
): PracticeSessionViewModel {
    val obtainActiveBatch = ObtainActiveBatch(
        composeNextBatch = ComposeNextBatch(clock),
        catalogRepository = catalogRepository,
        settingsRepository = FakeSettingsRepository(settings),
        practiceRepository = practice,
    )
    return PracticeSessionViewModel(
        obtainPracticeSession = ObtainPracticeSession(obtainActiveBatch, catalogRepository),
        answerPhrase = AnswerPhrase(practice),
        closeActiveBatch = CloseActiveBatch(practice, ResolveBatchClosure(clock)),
    )
}
