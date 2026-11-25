package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import com.dalmuina.fraseafrase.core.testing.anObtainCatalogPhrases
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObtainActiveBatchTest {

    private val targetLocale = LanguageTag("pt-PT")

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
    ) = ObtainActiveBatch(
        settingsRepository = settingsRepository,
        practiceRepository = practice,
        regenerateActiveBatchForLevel = RegenerateActiveBatchForLevel(
            practiceRepository = practice,
            composeNextBatch = ComposeNextBatch(),
            obtainCatalogPhrases = anObtainCatalogPhrases(FakeCatalogRepository(catalog)),
        ),
    )

    private fun a1Catalog(count: Int) = CatalogLoadResult.Loaded(
        (1..count).map { aCatalogPhrase(id = "a1-%02d".format(it), level = CefrLevel.A1) },
    )

    private suspend fun clearActiveBatch(practice: FakePracticeRepository) {
        practice.transact { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }
    }

    // Scenario: Existing (spec: practice-persistence, Tanda activa única)
    @Test
    fun `given an active batch when obtaining it then returns the same phrases in the saved order`() = runTest {
        // Given
        val active = PracticeSnapshot(
            progress = listOf(aPhraseProgress(id = "A"), aPhraseProgress(id = "B")),
            activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
            sessionCount = 0,
        )
        val practice = FakePracticeRepository(active)

        // When
        val result = useCase(practice, a1Catalog(3)).invoke(targetLocale)

        // Then
        result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        result.batch.entries.map { it.id.value } shouldBe listOf("A", "B")
    }

    // Scenario: Primer uso crea la primera tanda (spec: practice-persistence, Primer uso)
    @Test
    fun `given an empty database when obtaining the batch then creates the first batch with new phrases`() = runTest {
        // Given
        val practice = FakePracticeRepository()
        val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))

        // When
        val result = useCase(practice, a1Catalog(15), settingsRepository).invoke(targetLocale)

        // Then
        result.shouldBeInstanceOf<ActiveBatchResult.Created>()
        result.batch.entries.map { it.id.value } shouldBe (1..15).map { "a1-%02d".format(it) }
        result.batch.entries.map { it.level }.toSet() shouldBe setOf(Level(0))
        practice.read().activeBatch.shouldNotBeNull().order.size shouldBe 15
    }

    // Scenario: Catálogo vacío sin progreso no crea tanda (spec: practice-batch)
    @Test
    fun `given a catalog with no phrases when obtaining the batch then returns nothing available`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Loaded(emptyList())).invoke(targetLocale)

        // Then
        result shouldBe ActiveBatchResult.NothingAvailable
        practice.read().activeBatch shouldBe null
    }

    // Scenario: Catálogo no disponible (spec: phrase-catalog, Asset de catálogo no legible)
    @Test
    fun `given an unreadable catalog when obtaining the batch then returns the catalog error`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)).invoke(targetLocale)

        // Then
        result shouldBe ActiveBatchResult.CatalogUnavailable(CatalogLoadError.AssetUnreadable)
    }

    // Scenario: Una frase pendiente vuelve a estar disponible en su sesión (spec: practice-batch)
    @Test
    fun `given a phrase due at the current session when composing the next batch then it enters it`() =
        runTest {
            // Given: tanda de cinco frases (A y B certificadas con repaso muy futuro) cerrada; P, de nivel 1,
            // vence en la sesión actual (0)
            val batchPhrases = listOf("A", "B", "C", "D", "E")
            val certifiedPhrases = listOf("A", "B").map { id ->
                aPhraseProgress(id = id, level = Level(1), streak = Streak(3), dueAtSession = 100)
            }
            val uncertifiedPhrases = listOf("C", "D", "E").map { aPhraseProgress(id = it) }
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = certifiedPhrases + uncertifiedPhrases +
                        aPhraseProgress(id = "P", level = Level(1), dueAtSession = 0),
                    activeBatch = ActiveBatch(order = batchPhrases.map { PhraseId(it) }, capacity = BatchSize(10)),
                    sessionCount = 0,
                ),
            )
            clearActiveBatch(practice)

            // When
            val catalog = CatalogLoadResult.Loaded(
                (batchPhrases + "P").map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
            )
            val result = useCase(practice, catalog).invoke(targetLocale)

            // Then
            result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            result.batch.entries.map { it.id.value } shouldContain "P"
        }

    // Scenario: Generar sesión reinicia el desglose para la tanda siguiente (spec: practice-session), aplicado
    // también a componer la tanda sin una activa (ObtainActiveBatch comparte `composeNextBatchInto`)
    @Test
    fun `given a stale batch outcome with no active batch when composing the next batch then it resets to zero`() =
        runTest {
            // Given: sin tanda activa, pero con un desglose no reiniciado de una tanda anterior
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = emptyList(),
                    activeBatch = null,
                    sessionCount = 0,
                    batchOutcome = BatchOutcome(hits = 3, misses = 2, levelUps = 1),
                ),
            )

            // When
            useCase(practice, a1Catalog(15)).invoke(targetLocale)

            // Then
            practice.read().batchOutcome shouldBe BatchOutcome(hits = 0, misses = 0, levelUps = 0)
        }

    // Scenario: Ajustes aplicados distintos recomponen sin tocar el progreso (spec: practice-persistence,
    // "Recomposición de la tanda activa al detectar ajustes aplicados distintos")
    @Test
    fun `given different applied settings when obtaining the batch then it recomposes without touching progress`() =
        runTest {
            // Given: tanda activa compuesta con A1/N10, registrada como aplicada, y una frase con racha 2
            val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(10)))
            settingsRepository.recordAppliedSettings(targetLocale, UserSettings(CefrLevel.A1, BatchSize(10)))
            val trackedPhrase = aPhraseProgress(id = "A", level = Level(2), streak = Streak(2), dueAtSession = 5)
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(trackedPhrase),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10)),
                    sessionCount = 3,
                ),
            )
            val catalog = CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "A", level = CefrLevel.A1)) +
                    (1..10).map { aCatalogPhrase(id = "b1-%02d".format(it), level = CefrLevel.B1) },
            )

            // When: los ajustes guardados pasan a B1 y se obtiene la tanda activa
            val result = useCase(practice, catalog, settingsRepository).invoke(targetLocale)

            // Then: se recompone con frases B1, sin incrementar el contador de sesiones ni tocar el progreso
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value }.shouldContain("b1-01")
            practice.read().sessionCount shouldBe 3
            val stored = practice.read().progress.single { it.id.value == "A" }
            stored.streak shouldBe Streak(2)
            stored.level shouldBe Level(2)
            stored.dueAtSession shouldBe 5
        }

    // Scenario: Ajustes aplicados iguales no recomponen (spec: practice-persistence)
    @Test
    fun `given applied settings equal to saved ones when obtaining the batch then it does not recompose`() =
        runTest {
            // Given: tanda activa compuesta con B1/N20, registrada como aplicada y vigente
            val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(20)))
            settingsRepository.recordAppliedSettings(targetLocale, UserSettings(CefrLevel.B1, BatchSize(20)))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(aPhraseProgress(id = "A")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(20)),
                    sessionCount = 1,
                ),
            )

            // When
            val result = useCase(practice, a1Catalog(5), settingsRepository).invoke(targetLocale)

            // Then: se devuelve la misma tanda activa, sin recomponerla
            val existing = result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
            existing.batch.entries.map { it.id.value } shouldBe listOf("A")
        }

    // Scenario: Sin ajustes aplicados registrados todavía no se recompone (spec: practice-persistence)
    @Test
    fun `given no applied settings registered when obtaining it then returns existing and records settings`() =
        runTest {
            // Given: tanda activa sin ningún ajuste aplicado registrado, y ajustes guardados distintos del nivel
            // con el que se compuso (irrelevante aquí porque no hay nada que comparar)
            val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(20)))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(aPhraseProgress(id = "A")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(20)),
                    sessionCount = 1,
                ),
            )

            // When
            val result = useCase(practice, a1Catalog(5), settingsRepository).invoke(targetLocale)

            // Then
            val existing = result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
            existing.batch.entries.map { it.id.value } shouldBe listOf("A")
            settingsRepository.appliedSettings(targetLocale) shouldBe UserSettings(CefrLevel.B1, BatchSize(20))
        }

    // Scenario: Tras registrarse, un cambio posterior de ajustes ya recompone (spec: practice-persistence)
    @Test
    fun `given applied settings recorded on first obtain when saved settings change later then it recomposes`() =
        runTest {
            // Given: tanda activa sin ajustes aplicados registrados, con A1/N20 guardados
            val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(aPhraseProgress(id = "A")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(20)),
                    sessionCount = 1,
                ),
            )
            val catalog = CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "A", level = CefrLevel.A1)) +
                    (1..20).map { aCatalogPhrase(id = "b1-%02d".format(it), level = CefrLevel.B1) },
            )
            val useCase = useCase(practice, catalog, settingsRepository)

            // When: la primera obtención registra A1/N20 como ajustes aplicados, luego los ajustes guardados
            // cambian a B1 y se obtiene la tanda activa de nuevo
            useCase.invoke(targetLocale)
            settingsRepository.save(targetLocale, UserSettings(CefrLevel.B1, BatchSize(20)))
            val result = useCase.invoke(targetLocale)

            // Then: la tanda activa se recompone con frases del nivel CEFR B1
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value }.shouldContain("b1-01")
        }

    // Scenario: El cambio sobrevive al reinicio de la app (spec: practice-persistence)
    @Test
    fun `given a saved change made without reobtaining when the app restarts then the batch recomposes`() =
        runTest {
            // Given: tanda activa compuesta con A1, registrada como aplicada, y un cambio posterior de los
            // ajustes guardados a B1 sin volver a obtener la tanda activa
            val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(10)))
            settingsRepository.recordAppliedSettings(targetLocale, UserSettings(CefrLevel.A1, BatchSize(10)))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(aPhraseProgress(id = "A")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10)),
                    sessionCount = 0,
                ),
            )
            settingsRepository.save(targetLocale, UserSettings(CefrLevel.B1, BatchSize(10)))
            val catalog = CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "A", level = CefrLevel.A1)) +
                    (1..10).map { aCatalogPhrase(id = "b1-%02d".format(it), level = CefrLevel.B1) },
            )

            // When: la app se reinicia (nueva instancia de ObtainActiveBatch sobre el mismo estado persistido)
            // y se obtiene la tanda activa
            val result = useCase(practice, catalog, settingsRepository).invoke(targetLocale)

            // Then: la tanda activa se recompone con frases del nivel CEFR B1
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value }.shouldContain("b1-01")
        }

    // Scenario: El primer uso registra sus ajustes aplicados (spec: practice-persistence, "Registro de los
    // ajustes aplicados al componer")
    @Test
    fun `given no active batch and no applied settings when obtaining the batch then it records the saved settings`() =
        runTest {
            // Given: sin tanda activa ni ajustes aplicados registrados, con A1/N20 guardados
            val settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val practice = FakePracticeRepository()

            // When
            useCase(practice, a1Catalog(15), settingsRepository).invoke(targetLocale)

            // Then
            settingsRepository.appliedSettings(targetLocale) shouldBe UserSettings(CefrLevel.A1, BatchSize(20))
        }
}
