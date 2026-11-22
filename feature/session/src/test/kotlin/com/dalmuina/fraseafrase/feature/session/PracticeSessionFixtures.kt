package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.audio.PlayPhraseAudio
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.DefaultSourceLocale
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainCatalogPhrasesForActiveLanguage
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.AnswerPhrase
import com.dalmuina.fraseafrase.core.domain.practice.GenerateSession
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.practice.RegenerateActiveBatchForLevel
import com.dalmuina.fraseafrase.core.domain.practice.RevertLastAnswer
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
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
 * [activeTargetLocale] es el idioma destino activo según los ajustes (por defecto
 * [FakeSettingsRepository.DEFAULT_LOCALE]), usado también como clave del catálogo (group 8,
 * `add-multi-language-catalog`).
 */
internal fun sessionViewModel(
    practice: FakePracticeRepository,
    catalog: CatalogLoadResult,
    settings: UserSettings = UserSettings.DEFAULT,
    phrasePlayer: FakePhrasePlayer = FakePhrasePlayer(),
    activeTargetLocale: LanguageTag = FakeSettingsRepository.DEFAULT_LOCALE,
): PracticeSessionViewModel =
    sessionViewModelWith(
        practice = practice,
        catalogRepository = FakeCatalogRepository(catalog, targetLocale = activeTargetLocale),
        settings = settings,
        phrasePlayer = phrasePlayer,
        activeTargetLocale = activeTargetLocale,
    )

/** Catálogo cuyo resultado puede cambiar entre lecturas, para simular un catálogo que deja de cargarse. */
internal class SwitchableCatalogRepository(var result: CatalogLoadResult) : CatalogRepository {
    override suspend fun availableTargetLocales(): List<LanguageTag> = listOf(FakeSettingsRepository.DEFAULT_LOCALE)
    override suspend fun availableSourceLocales(targetLocale: LanguageTag): List<LanguageTag> = listOf(SOURCE_LOCALE)
    override suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult = result

    private companion object {
        val SOURCE_LOCALE = LanguageTag("es-ES")
    }
}

/** Igual que [sessionViewModel], pero con un catálogo cualquiera, por ejemplo uno que cambia durante el test. */
internal fun sessionViewModelWith(
    practice: FakePracticeRepository,
    catalogRepository: CatalogRepository,
    settings: UserSettings = UserSettings.DEFAULT,
    phrasePlayer: FakePhrasePlayer = FakePhrasePlayer(),
    activeTargetLocale: LanguageTag = FakeSettingsRepository.DEFAULT_LOCALE,
): PracticeSessionViewModel {
    val settingsRepository = FakeSettingsRepository(settings, defaultActiveTargetLocale = activeTargetLocale)
    val obtainCatalogPhrases = ObtainCatalogPhrasesForActiveLanguage(
        deviceLocaleProvider = FakeDeviceLocaleProvider(LanguageTag("es-ES")),
        catalogRepository = catalogRepository,
    )
    val composeNextBatch = ComposeNextBatch()
    val regenerateActiveBatchForLevel = RegenerateActiveBatchForLevel(
        practiceRepository = practice,
        composeNextBatch = composeNextBatch,
        obtainCatalogPhrases = obtainCatalogPhrases,
    )
    val obtainActiveBatch = ObtainActiveBatch(
        settingsRepository = settingsRepository,
        practiceRepository = practice,
        regenerateActiveBatchForLevel = regenerateActiveBatchForLevel,
    )
    return PracticeSessionViewModel(
        obtainPracticeSession = ObtainPracticeSession(obtainActiveBatch, obtainCatalogPhrases, practice),
        answerPhrase = AnswerPhrase(practice),
        generateSession = GenerateSession(
            practiceRepository = practice,
            composeNextBatch = composeNextBatch,
            obtainCatalogPhrases = obtainCatalogPhrases,
            settingsRepository = settingsRepository,
        ),
        playPhraseAudio = PlayPhraseAudio(phrasePlayer),
        revertLastAnswer = RevertLastAnswer(practice),
        obtainActiveLanguage = ObtainActiveLanguage(
            settingsRepository,
            ObtainSourceLanguage(
                FakeDeviceLocaleProvider(LanguageTag("es-ES")),
                catalogRepository,
                DefaultSourceLocale(LanguageTag("es-ES")),
            ),
            catalogRepository,
        ),
    )
}
