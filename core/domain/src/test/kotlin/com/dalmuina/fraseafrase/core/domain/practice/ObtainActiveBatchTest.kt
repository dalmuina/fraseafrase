package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ObtainActiveBatchTest {

    private val today = LocalDate.of(2026, 3, 10)

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settings: UserSettings = UserSettings.DEFAULT,
        clock: FixedClock = FixedClock(today),
    ) = ObtainActiveBatch(
        composeNextBatch = ComposeNextBatch(clock),
        catalogRepository = FakeCatalogRepository(catalog),
        settingsRepository = FakeSettingsRepository(settings),
        practiceRepository = practice,
    )

    private fun a1Catalog(count: Int) = CatalogLoadResult.Loaded(
        (1..count).map { aCatalogPhrase(id = "a1-%02d".format(it), level = CefrLevel.A1) },
    )

    // Scenario: Existing (spec: practice-persistence, Tanda activa única)
    @Test
    fun `given an active batch when obtaining it then returns the same phrases in the saved order`() = runTest {
        // Given
        val active = PracticeSnapshot(
            progress = listOf(aPhraseProgress(id = "A"), aPhraseProgress(id = "B")),
            activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
            carry = CarryQueue(emptyList()),
        )
        val practice = FakePracticeRepository(active)

        // When
        val result = useCase(practice, a1Catalog(3)).invoke()

        // Then
        result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        result.batch.entries.map { it.id.value } shouldBe listOf("A", "B")
    }

    // Scenario: Primer uso crea la primera tanda (spec: practice-persistence)
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

    // Scenario: Composición sin frases disponibles (spec: practice-batch)
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

    // Scenario: Cambio de fecha con tanda activa (spec: practice-persistence) — regresión de 4.8
    @Test
    fun `given an active batch and a new day when obtaining it then the batch is kept`() = runTest {
        // Given
        val batchPhrases = listOf("A", "B", "C", "D", "E")
        val active = PracticeSnapshot(
            progress = batchPhrases.map { aPhraseProgress(id = it) } +
                aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 11)),
            activeBatch = ActiveBatch(order = batchPhrases.map { PhraseId(it) }, capacity = BatchSize(10)),
            carry = CarryQueue(emptyList()),
        )
        val practice = FakePracticeRepository(active)
        val nextDay = FixedClock(LocalDate.of(2026, 3, 11))

        // When
        val result = useCase(practice, a1Catalog(3), clock = nextDay).invoke()

        // Then
        result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        result.batch.entries.map { it.id.value } shouldBe batchPhrases
        result.batch.entries.map { it.id.value } shouldNotContain "P"
    }

    // Scenario: Repaso del nuevo día entra en la tanda siguiente (spec: practice-persistence)
    @Test
    fun `given a batch closed on the day of a due review when composing the next batch then the review enters it`() =
        runTest {
            // Given: tanda de cinco frases (A y B certificadas, C, D y E sin certificar) cerrada el 11-03 con
            // expectativa 3; P, de nivel 1, tiene repaso el 11-03
            val day = LocalDate.of(2026, 3, 11)
            val batchPhrases = listOf("A", "B", "C", "D", "E")
            val certifiedPhrases = listOf("A", "B").map { id ->
                aPhraseProgress(
                    id = id,
                    level = 1,
                    streak = 3,
                    answeredInBatch = true,
                    nextReview = day.plusDays(1),
                )
            }
            val uncertifiedPhrases = listOf("C", "D", "E").map { aPhraseProgress(id = it) }
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = certifiedPhrases + uncertifiedPhrases +
                        aPhraseProgress(id = "P", level = 1, nextReview = day),
                    activeBatch = ActiveBatch(order = batchPhrases.map { PhraseId(it) }, capacity = BatchSize(10)),
                    carry = CarryQueue(emptyList()),
                ),
            )
            CloseActiveBatch(practice, ResolveBatchClosure(FixedClock(day))).invoke(expectedUncertified = 3)

            // When
            val catalog = CatalogLoadResult.Loaded(
                (batchPhrases + "P").map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
            )
            val result = useCase(practice, catalog, clock = FixedClock(day)).invoke()

            // Then
            result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            result.batch.entries.map { it.id.value } shouldContain "P"
        }
}
