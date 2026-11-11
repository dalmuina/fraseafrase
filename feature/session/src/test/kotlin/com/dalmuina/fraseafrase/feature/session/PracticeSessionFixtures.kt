package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.audio.PlayPhraseAudio
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.AnswerPhrase
import com.dalmuina.fraseafrase.core.domain.practice.GenerateSession
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.practice.RevertLastAnswer
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePhrasePlayer
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.types.shouldBeInstanceOf

/** Snapshot con una tanda activa de frases [order], en ese orden, y el progreso dado (por defecto, sin respuestas). */
internal fun activeBatchSnapshot(
    order: List<String>,
    progress: List<PhraseProgress> = order.map { aPhraseProgress(id = it) },
    sessionCount: Int = 0,
): PracticeSnapshot = PracticeSnapshot(
    progress = progress,
    activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
    sessionCount = sessionCount,
)

/**
 * Tanda A (nivel 2, racha 2) y B sin responder: un acierto en A certifica a nivel 3 (Oro), lo que muestra el
 * indicador de cambio de insignia (spec: add-level-change-animation, new-badge-animation).
 */
internal fun levelUpOnHitSnapshot(): PracticeSnapshot = activeBatchSnapshot(
    order = listOf("A", "B"),
    progress = listOf(
        aPhraseProgress(id = "A", level = Level(2), streak = Streak(2), answeredInBatch = true),
        aPhraseProgress(id = "B"),
    ),
)

/**
 * Tanda A (nivel 1, racha 0) y B sin responder: un acierto en A no certifica, así que no hay cambio de
 * insignia que animar (spec: add-level-change-animation, new-badge-animation).
 */
internal fun noLevelChangeOnHitSnapshot(): PracticeSnapshot = activeBatchSnapshot(
    order = listOf("A", "B"),
    progress = listOf(
        aPhraseProgress(id = "A", level = Level(1), streak = Streak(0), answeredInBatch = true),
        aPhraseProgress(id = "B"),
    ),
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
 * ViewModel con los casos de uso reales sobre los fakes de `:core:testing`.
 * Ningún caso de uso se mockea: son clases concretas y su lógica forma parte de lo que se verifica.
 */
internal fun sessionViewModel(
    practice: FakePracticeRepository,
    catalog: CatalogLoadResult,
    settings: UserSettings = UserSettings.DEFAULT,
    phrasePlayer: FakePhrasePlayer = FakePhrasePlayer(),
): PracticeSessionViewModel =
    sessionViewModelWith(practice, FakeCatalogRepository(catalog), settings, phrasePlayer)

/** Catálogo cuyo resultado puede cambiar entre lecturas, para simular un catálogo que deja de cargarse. */
internal class SwitchableCatalogRepository(var result: CatalogLoadResult) : CatalogRepository {
    override suspend fun phrases(): CatalogLoadResult = result
}

/** Igual que [sessionViewModel], pero con un catálogo cualquiera, por ejemplo uno que cambia durante el test. */
internal fun sessionViewModelWith(
    practice: FakePracticeRepository,
    catalogRepository: CatalogRepository,
    settings: UserSettings = UserSettings.DEFAULT,
    phrasePlayer: FakePhrasePlayer = FakePhrasePlayer(),
): PracticeSessionViewModel {
    val settingsRepository = FakeSettingsRepository(settings)
    val composeNextBatch = ComposeNextBatch()
    val obtainActiveBatch = ObtainActiveBatch(
        composeNextBatch = composeNextBatch,
        catalogRepository = catalogRepository,
        settingsRepository = settingsRepository,
        practiceRepository = practice,
    )
    return PracticeSessionViewModel(
        obtainPracticeSession = ObtainPracticeSession(obtainActiveBatch, catalogRepository),
        answerPhrase = AnswerPhrase(practice),
        generateSession = GenerateSession(
            practiceRepository = practice,
            composeNextBatch = composeNextBatch,
            catalogRepository = catalogRepository,
            settingsRepository = settingsRepository,
        ),
        playPhraseAudio = PlayPhraseAudio(phrasePlayer),
        revertLastAnswer = RevertLastAnswer(practice),
    )
}
