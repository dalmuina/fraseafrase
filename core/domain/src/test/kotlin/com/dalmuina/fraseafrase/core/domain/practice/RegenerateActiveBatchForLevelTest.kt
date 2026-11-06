package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Spec: practice-batch, "Regeneración de la tanda activa por cambio de nivel CEFR"
class RegenerateActiveBatchForLevelTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val clock = FixedClock(today)

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settings: UserSettings = UserSettings.DEFAULT,
    ) = RegenerateActiveBatchForLevel(
        practiceRepository = practice,
        composeNextBatch = ComposeNextBatch(clock),
        catalogRepository = FakeCatalogRepository(catalog),
        settingsRepository = FakeSettingsRepository(settings),
    )

    private fun snapshot(order: List<String>, progress: List<PhraseProgress>) =
        PracticeSnapshot(
            progress = progress,
            activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
            carry = CarryQueue(emptyList()),
        )

    // Scenario: Cambiar el nivel CEFR no certifica ni cambia niveles
    @Test
    fun `given an active batch when changing the cefr level then no phrase is certified or changes level`() = runTest {
        // Given: tanda activa con una frase de nivel 3 y racha 3, y otra de nivel 1 y racha 0
        val progress = listOf(
            aPhraseProgress(
                id = "A",
                level = Level(3),
                streak = Streak(3),
                answeredInBatch = true,
                nextReview = today.plusDays(5),
            ),
            aPhraseProgress(id = "B", level = Level(1), streak = Streak(0), nextReview = today.plusDays(1)),
        )
        val practice = FakePracticeRepository(snapshot(order = listOf("A", "B"), progress = progress))
        val catalog = CatalogLoadResult.Loaded(
            listOf("A", "B", "C").map { aCatalogPhrase(id = it, level = CefrLevel.B1) },
        )

        // When
        val result: ActiveBatchResult = useCase(practice, catalog).invoke(CefrLevel.B1)
        result.shouldBeInstanceOf<ActiveBatchResult.Created>()

        // Then
        val stored = practice.read().progress.associateBy { it.id.value }
        stored["A"]!!.level shouldBe Level(3)
        stored["A"]!!.nextReview shouldBe progress[0].nextReview
        stored["B"]!!.level shouldBe Level(1)
        stored["B"]!!.nextReview shouldBe progress[1].nextReview
    }

    // Scenario: Las frases de la tanda activa pasan a arrastradas con el nuevo nivel
    @Test
    fun `given an active batch with leftover queue when changing the cefr level then the next batch starts with it`() =
        runTest {
            // Given: tanda activa en el orden B, C, A, cola sobrante con Z, y catálogo con frases nuevas de B1
            val progress = listOf("A", "B", "C").map { aPhraseProgress(id = it) } +
                aPhraseProgress(id = "Z", level = Level(0))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = progress,
                    activeBatch = ActiveBatch(
                        order = listOf("B", "C", "A").map { PhraseId(it) },
                        capacity = BatchSize(10),
                    ),
                    carry = CarryQueue(listOf(PhraseId("Z"))),
                ),
            )
            val catalog = CatalogLoadResult.Loaded(
                listOf("A", "B", "C", "Z", "N1", "N2").map { aCatalogPhrase(id = it, level = CefrLevel.B1) },
            )

            // When
            val result = useCase(practice, catalog).invoke(CefrLevel.B1)

            // Then
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value }.take(4) shouldContainExactly listOf("Z", "B", "C", "A")
        }

    // Scenario: La tanda siguiente se compone por cuotas con el nuevo nivel
    @Test
    fun `given a batch replaced by a cefr level change when composing the next batch then it uses the same quotas`() =
        runTest {
            // Given: material suficiente de arrastradas (la tanda activa), pendientes, dominadas y nuevas de B1
            val carriedOver = (1..3).map { "carry-$it" }
            val carriedProgress = carriedOver.map { aPhraseProgress(id = it) }
            val pending = (1..10).map { "pending-$it" }
            val pendingProgress = pending.map {
                aPhraseProgress(id = it, level = Level(1), nextReview = today)
            }
            val mastered = (1..6).map { "mastered-$it" }
            val masteredProgress = mastered.map {
                aPhraseProgress(
                    id = it,
                    level = Level(4),
                    streak = Streak(3),
                    answeredInBatch = true,
                    nextReview = today.plusDays(10),
                )
            }
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = carriedProgress + pendingProgress + masteredProgress,
                    activeBatch = ActiveBatch(
                        order = carriedOver.map { PhraseId(it) },
                        capacity = BatchSize(20),
                    ),
                    carry = CarryQueue(emptyList()),
                ),
            )
            val newPhrases = (1..20).map { "new-b1-$it" }
            val catalog = CatalogLoadResult.Loaded(
                newPhrases.map { aCatalogPhrase(id = it, level = CefrLevel.B1) } +
                    carriedOver.map { aCatalogPhrase(id = it, level = CefrLevel.A1) } +
                    pending.map { aCatalogPhrase(id = it, level = CefrLevel.A1) } +
                    mastered.map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
            )

            // When
            val result = useCase(practice, catalog).invoke(CefrLevel.B1)

            // Then: mismas cuotas que una generación de sesión con N=20: 8 nuevas, 8 a reforzar, 4 dominadas
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            val ids = created.batch.entries.map { it.id.value }
            ids.count { it.startsWith("new-b1-") } shouldBe 8
            ids.count { it in carriedOver || it in pending } shouldBe 8
            ids.count { it in mastered } shouldBe 4
        }
}
