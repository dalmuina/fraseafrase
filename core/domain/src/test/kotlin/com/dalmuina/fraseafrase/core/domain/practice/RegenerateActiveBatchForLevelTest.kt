package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Spec: practice-batch, "Regeneración de la tanda activa por cambio de nivel CEFR sin resolución"
class RegenerateActiveBatchForLevelTest {

    private fun useCase(practice: FakePracticeRepository, catalog: CatalogLoadResult) = RegenerateActiveBatchForLevel(
        practiceRepository = practice,
        composeNextBatch = ComposeNextBatch(),
        catalogRepository = FakeCatalogRepository(catalog),
    )

    private fun snapshot(order: List<String>, progress: List<PhraseProgress>, sessionCount: Int = 9) =
        PracticeSnapshot(
            progress = progress,
            activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
            sessionCount = sessionCount,
        )

    // Scenario: Cambiar el nivel CEFR no toca racha, nivel ni dueAtSession
    @Test
    fun `given an active batch when changing the cefr level then no phrase changes level, streak or dueAtSession`() =
        runTest {
            // Given: tanda activa con una frase de nivel 3 y racha 2, y otra de nivel 1 y racha 0
            val progress = listOf(
                aPhraseProgress(id = "A", level = Level(3), streak = Streak(2), dueAtSession = 15),
                aPhraseProgress(id = "B", level = Level(1), streak = Streak(0), dueAtSession = 3),
            )
            val practice = FakePracticeRepository(snapshot(order = listOf("A", "B"), progress = progress))
            val catalog = CatalogLoadResult.Loaded(
                listOf("A", "B", "C").map { aCatalogPhrase(id = it, level = CefrLevel.B1) },
            )

            // When
            val result = useCase(practice, catalog).invoke(CefrLevel.B1, BatchSize(10))
            result.shouldBeInstanceOf<ActiveBatchResult.Created>()

            // Then
            val stored = practice.read().progress.associateBy { it.id.value }
            stored["A"]!!.level shouldBe Level(3)
            stored["A"]!!.streak shouldBe Streak(2)
            stored["A"]!!.dueAtSession shouldBe 15
            stored["B"]!!.level shouldBe Level(1)
            stored["B"]!!.streak shouldBe Streak(0)
            stored["B"]!!.dueAtSession shouldBe 3
        }

    // Scenario: Cambiar el nivel CEFR no incrementa el contador de sesiones
    @Test
    fun `given the session counter at 9 when changing the cefr level then the counter stays at 9`() = runTest {
        // Given
        val practice = FakePracticeRepository(
            snapshot(order = listOf("A"), progress = listOf(aPhraseProgress(id = "A")), sessionCount = 9),
        )
        val catalog = CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "A", level = CefrLevel.B1)))

        // When
        useCase(practice, catalog).invoke(CefrLevel.B1, BatchSize(10))

        // Then
        practice.read().sessionCount shouldBe 9
    }

    // Scenario: La tanda siguiente se compone por cuotas con el nuevo nivel
    @Test
    fun `given material for the new level when composing the next batch then it uses the same quotas as a session`() =
        runTest {
            // Given: material suficiente de a reforzar y dominadas (B1) y nuevas (B1), sin tanda activa
            val reinforce = (1..11).map { "reinforce-$it" }
            val reinforceProgress = reinforce.map { aPhraseProgress(id = it, level = Level(1), dueAtSession = 0) }
            val mastered = (1..6).map { "mastered-$it" }
            val masteredProgress = mastered.map {
                aPhraseProgress(id = it, level = Level(4), streak = Streak(0), dueAtSession = 100)
            }
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = reinforceProgress + masteredProgress,
                    activeBatch = null,
                    sessionCount = 0,
                ),
            )
            val newPhrases = (1..20).map { "new-b1-$it" }
            val catalog = CatalogLoadResult.Loaded(
                newPhrases.map { aCatalogPhrase(id = it, level = CefrLevel.B1) } +
                    (reinforce + mastered).map { aCatalogPhrase(id = it, level = CefrLevel.B1) },
            )

            // When
            val result = useCase(practice, catalog).invoke(CefrLevel.B1, BatchSize(20))

            // Then: mismas cuotas que una generación de sesión con N=20: 4 nuevas, 11 a reforzar, 5 dominadas
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            val ids = created.batch.entries.map { it.id.value }
            ids.count { it.startsWith("new-b1-") } shouldBe 4
            ids.count { it in reinforce } shouldBe 11
            ids.count { it in mastered } shouldBe 5
        }

    // Scenario: Generar sesión reinicia el desglose para la tanda siguiente (spec: practice-session), aplicado
    // también al cambio de nivel CEFR/N desde Ajustes (ADR-0017)
    @Test
    fun `given a completed batch outcome when changing the cefr level then the batch outcome resets to zero`() =
        runTest {
            // Given: tanda activa con un desglose de 2 aciertos y 1 fallo acumulado
            val practice = FakePracticeRepository(
                snapshot(order = listOf("A"), progress = listOf(aPhraseProgress(id = "A")))
                    .copy(batchOutcome = BatchOutcome(hits = 2, misses = 1, levelUps = 0)),
            )
            val catalog = CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "A", level = CefrLevel.B1)))

            // When
            useCase(practice, catalog).invoke(CefrLevel.B1, BatchSize(10))

            // Then: el desglose de la tanda anterior no se arrastra a la nueva composición
            practice.read().batchOutcome shouldBe BatchOutcome(hits = 0, misses = 0, levelUps = 0)
        }
}
