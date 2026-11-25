package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
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
import com.dalmuina.fraseafrase.core.testing.anObtainCatalogPhrases
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Spec: practice-batch, "Regeneración de la tanda activa por cambio de nivel CEFR sin resolución"
class RegenerateActiveBatchForLevelTest {

    private val targetLocale = LanguageTag("pt-PT")

    private fun useCase(practice: FakePracticeRepository, catalog: CatalogLoadResult) = RegenerateActiveBatchForLevel(
        practiceRepository = practice,
        composeNextBatch = ComposeNextBatch(),
        obtainCatalogPhrases = anObtainCatalogPhrases(FakeCatalogRepository(catalog)),
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
            val result = useCase(practice, catalog).invoke(
                targetLocale,
                CefrLevel.B1,
                BatchSize(10),
                practice.read().activeBatch,
            )
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
        useCase(practice, catalog).invoke(targetLocale, CefrLevel.B1, BatchSize(10), practice.read().activeBatch)

        // Then
        practice.read().sessionCount shouldBe 9
    }

    // Scenario: La tanda siguiente se compone por cuotas con el nuevo nivel (spec: practice-batch), con el
    // resultado nuevo del delta: mismas reglas que una generación de sesión (ADR-0025)
    @Test
    fun `given material for the new level when composing the next batch then it uses the same rules as a session`() =
        runTest {
            // Given: doce pendientes B1 de nivel 1 con el mismo retraso, ninguna de nivel 0, y veinte nuevas B1
            val pending = (1..12).map { "pending-$it" }
            val pendingProgress = pending.map { aPhraseProgress(id = it, level = Level(1), dueAtSession = 0) }
            val practice = FakePracticeRepository(
                PracticeSnapshot(progress = pendingProgress, activeBatch = null, sessionCount = 0),
            )
            val newPhrases = (1..20).map { "new-b1-$it" }
            val catalog = CatalogLoadResult.Loaded(
                newPhrases.map { aCatalogPhrase(id = it, level = CefrLevel.B1) } +
                    pending.map { aCatalogPhrase(id = it, level = CefrLevel.B1) },
            )

            // When
            val result = useCase(practice, catalog).invoke(
                targetLocale,
                CefrLevel.B1,
                BatchSize(20),
                practice.read().activeBatch,
            )

            // Then: N=20 con pendientes de nivel 1: 8 nuevas (40 %) y 12 pendientes
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            val ids = created.batch.entries.map { it.id.value }
            ids.count { it.startsWith("new-b1-") } shouldBe 8
            ids.count { it in pending } shouldBe 12
        }

    // Scenario: El progreso de un nivel CEFR abandonado se conserva y vuelve a ofrecerse al volver a ese nivel
    @Test
    fun `given an abandoned cefr level phrase when its level is chosen again then it reappears unchanged`() =
        runTest {
            // Given: una frase a reforzar de nivel CEFR B1 con nivel de consolidación 3 y racha 2, nivel elegido B1
            val progress = listOf(
                aPhraseProgress(id = "b1-phrase", level = Level(3), streak = Streak(2), dueAtSession = 0),
            )
            val practice = FakePracticeRepository(snapshot(order = listOf("b1-phrase"), progress = progress))
            val catalog = CatalogLoadResult.Loaded(
                listOf(
                    aCatalogPhrase(id = "b1-phrase", level = CefrLevel.B1),
                    aCatalogPhrase(id = "a1-phrase", level = CefrLevel.A1),
                ),
            )
            val sut = useCase(practice, catalog)

            // When: el usuario cambia el nivel CEFR elegido a A1, practica (compone con A1), y después vuelve a B1
            val switchedToA1 = sut.invoke(targetLocale, CefrLevel.A1, BatchSize(10), practice.read().activeBatch)
            val switchedToA1Batch = switchedToA1.shouldBeInstanceOf<ActiveBatchResult.Created>()
            switchedToA1Batch.batch.entries.map { it.id.value } shouldBe listOf("a1-phrase")

            val switchedBackToB1 = sut.invoke(targetLocale, CefrLevel.B1, BatchSize(10), practice.read().activeBatch)

            // Then: la frase de B1 conserva su nivel de consolidación y racha, y vuelve a ser candidata a la tanda
            val switchedBackBatch = switchedBackToB1.shouldBeInstanceOf<ActiveBatchResult.Created>()
            switchedBackBatch.batch.entries.map { it.id.value } shouldBe listOf("b1-phrase")
            val stored = practice.read().progress.associateBy { it.id.value }
            stored["b1-phrase"]!!.level shouldBe Level(3)
            stored["b1-phrase"]!!.streak shouldBe Streak(2)
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
            useCase(practice, catalog).invoke(targetLocale, CefrLevel.B1, BatchSize(10), practice.read().activeBatch)

            // Then: el desglose de la tanda anterior no se arrastra a la nueva composición
            practice.read().batchOutcome shouldBe BatchOutcome(hits = 0, misses = 0, levelUps = 0)
        }

    // Scenario: La obtención que llega tarde no recompone la tanda ya compuesta (spec: practice-persistence)
    @Test
    fun `given another batch than the observed one when regenerating then it returns the current one unchanged`() =
        runTest {
            // Given: se observó "sin tanda activa", pero ya hay guardada una tanda de diez frases
            val ids = (1..10).map { "stored-%02d".format(it) }
            val stored = snapshot(order = ids.reversed(), progress = ids.map { aPhraseProgress(id = it) })
            val practice = FakePracticeRepository(stored)
            val catalog = CatalogLoadResult.Loaded((ids + "new").map { aCatalogPhrase(it) })

            // When
            val result = useCase(practice, catalog).invoke(targetLocale, CefrLevel.A1, BatchSize(10), null)

            // Then
            val existing = result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
            existing.batch.entries.map { it.id.value } shouldBe ids.reversed()
            practice.read() shouldBe stored
        }

    // Scenario: La obtención que llega tarde no recompone la tanda ya compuesta (spec: practice-persistence),
    // with a non-null observed batch that differs from the stored one
    @Test
    fun `given a previous batch was observed and replaced when regenerating then the stored one stays`() = runTest {
        // Given: batch A was observed, but batch B is the one stored by now
        val observed = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10))
        val stored = snapshot(order = listOf("B"), progress = listOf("A", "B").map { aPhraseProgress(id = it) })
        val practice = FakePracticeRepository(stored)
        val catalog = CatalogLoadResult.Loaded(listOf("A", "B", "new").map { aCatalogPhrase(it) })

        // When
        val result = useCase(practice, catalog).invoke(targetLocale, CefrLevel.A1, BatchSize(10), observed)

        // Then
        val existing = result.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        existing.batch.entries.map { it.id.value } shouldBe listOf("B")
        practice.read() shouldBe stored
    }

    // Scenario: La obtención que llega tarde sin tanda vigente devuelve que no hay frases
    @Test
    fun `given the observed batch was emptied when regenerating then it returns nothing available unchanged`() =
        runTest {
            // Given: se observó una tanda con A, pero ya se ha vaciado
            val observed = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10))
            val emptied = PracticeSnapshot(
                progress = listOf(aPhraseProgress(id = "A")),
                activeBatch = null,
                sessionCount = 9,
            )
            val practice = FakePracticeRepository(emptied)
            val catalog = CatalogLoadResult.Loaded(listOf("A", "new").map { aCatalogPhrase(it) })

            // When
            val result = useCase(practice, catalog).invoke(targetLocale, CefrLevel.A1, BatchSize(10), observed)

            // Then
            result shouldBe ActiveBatchResult.NothingAvailable
            practice.read() shouldBe emptied
        }
}
