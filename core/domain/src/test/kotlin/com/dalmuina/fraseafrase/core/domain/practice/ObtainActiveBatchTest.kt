package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
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
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObtainActiveBatchTest {

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settings: UserSettings = UserSettings.DEFAULT,
    ) = ObtainActiveBatch(
        composeNextBatch = ComposeNextBatch(),
        catalogRepository = FakeCatalogRepository(catalog),
        settingsRepository = FakeSettingsRepository(settings),
        practiceRepository = practice,
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
        val result = useCase(practice, a1Catalog(3)).invoke()

        // Then
        result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        result.batch.entries.map { it.id.value } shouldBe listOf("A", "B")
    }

    // Scenario: Primera tanda con catálogo disponible (spec: practice-batch)
    @Test
    fun `given an empty database when obtaining the batch then creates the first batch with new phrases`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, a1Catalog(15)).invoke()

        // Then
        result.shouldBeInstanceOf<ActiveBatchResult.Created>()
        result.batch.entries.map { it.id.value } shouldBe (1..15).map { "a1-%02d".format(it) }
        practice.read().activeBatch.shouldNotBeNull().order.size shouldBe 15
    }

    // Scenario: Catálogo vacío sin progreso no crea tanda (spec: practice-batch)
    @Test
    fun `given a catalog with no phrases when obtaining the batch then returns nothing available`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Loaded(emptyList())).invoke()

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
        val result = useCase(practice, CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)).invoke()

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
            val result = useCase(practice, catalog).invoke()

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
            useCase(practice, a1Catalog(15)).invoke()

            // Then
            practice.read().batchOutcome shouldBe BatchOutcome(hits = 0, misses = 0, levelUps = 0)
        }
}
