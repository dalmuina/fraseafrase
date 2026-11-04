package com.dalmuina.fraseafrase.core.domain.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ObtainProgressOverviewTest {

    private fun useCase(practice: FakePracticeRepository, catalog: CatalogLoadResult) = ObtainProgressOverview(
        catalogRepository = FakeCatalogRepository(catalog),
        practiceRepository = practice,
    )

    private fun activeBatch(order: List<String>, progress: List<PhraseProgress>) = PracticeSnapshot(
        progress = progress,
        activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
        carry = CarryQueue(emptyList()),
    )

    private fun loaded(vararg phrases: CatalogPhrase) = CatalogLoadResult.Loaded(phrases.toList())

    /** Frase certificada en la tanda: racha 3 respondida. */
    private fun certified(id: String) = aPhraseProgress(id = id, streak = 3, answeredInBatch = true)

    // Scenario: Tanda con certificadas y no certificadas (spec: progress-overview)
    @Test
    fun `given certified and uncertified phrases when opening progress then it shows two of five certified`() =
        runTest {
            // Given: tanda activa de cinco frases, dos con racha 3 y tres sin certificar
            val progress = listOf(certified("A"), certified("B")) +
                listOf("C", "D", "E").map { aPhraseProgress(id = it) }
            val practice = FakePracticeRepository(
                activeBatch(order = listOf("A", "B", "C", "D", "E"), progress = progress),
            )
            val catalog = loaded(*listOf("A", "B", "C", "D", "E").map { aCatalogPhrase(id = it) }.toTypedArray())

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
            val batchProgress = requireNotNull(available.batchProgress)
            batchProgress.certified shouldBe 2
            batchProgress.total shouldBe 5
        }

    // Scenario: Tanda sin ninguna certificada (spec: progress-overview)
    @Test
    fun `given a batch with no certified phrases when opening progress then it shows zero of three certified`() =
        runTest {
            // Given: tanda activa de tres frases, ninguna con racha 3
            val progress = listOf("A", "B", "C").map { aPhraseProgress(id = it) }
            val practice = FakePracticeRepository(
                activeBatch(order = listOf("A", "B", "C"), progress = progress),
            )
            val catalog = loaded(*listOf("A", "B", "C").map { aCatalogPhrase(id = it) }.toTypedArray())

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
            val batchProgress = requireNotNull(available.batchProgress)
            batchProgress.certified shouldBe 0
            batchProgress.total shouldBe 3
        }

    // Scenario: Catálogo con frases en varios niveles (spec: progress-overview)
    @Test
    fun `given phrases at several levels when opening progress then byLevel counts each level`() = runTest {
        // Given: catálogo de diez frases donde tres tienen progreso de nivel 2, una de nivel 5 y las
        // seis restantes no tienen progreso
        val levelTwoIds = listOf("A", "B", "C")
        val levelFiveId = "D"
        val withoutProgressIds = listOf("E", "F", "G", "H", "I", "J")
        val progress = levelTwoIds.map {
            aPhraseProgress(id = it, level = 2, nextReview = LocalDate.of(2026, 3, 11))
        } + aPhraseProgress(id = levelFiveId, level = 5, nextReview = LocalDate.of(2026, 3, 11))
        val practice = FakePracticeRepository(
            PracticeSnapshot(progress = progress, activeBatch = null, carry = CarryQueue(emptyList())),
        )
        val catalog = loaded(
            *(levelTwoIds + levelFiveId + withoutProgressIds).map { aCatalogPhrase(id = it) }.toTypedArray(),
        )

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.byLevel shouldContainExactly mapOf(
            Level(0) to 6,
            Level(1) to 0,
            Level(2) to 3,
            Level(3) to 0,
            Level(4) to 0,
            Level(5) to 1,
        )
    }

    // Scenario: Ninguna frase con progreso (spec: progress-overview)
    @Test
    fun `given a catalog without any progress when opening progress then byLevel counts everything at level zero`() =
        runTest {
            // Given: catálogo de ocho frases sin ningún progreso registrado
            val practice = FakePracticeRepository()
            val catalog = loaded(*(1..8).map { aCatalogPhrase(id = "phrase-$it") }.toTypedArray())

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
            available.byLevel shouldContainExactly mapOf(
                Level(0) to 8,
                Level(1) to 0,
                Level(2) to 0,
                Level(3) to 0,
                Level(4) to 0,
                Level(5) to 0,
            )
        }

    // Scenario: Frase de la tanda activa cuenta en nivel 0 (spec: progress-overview)
    @Test
    fun `given a batch phrase at level zero when opening progress then it counts in level zero`() = runTest {
        // Given: catálogo de cinco frases donde una está en la tanda activa con nivel 0 y racha 2, y
        // las otras cuatro no tienen progreso
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A"),
                progress = listOf(aPhraseProgress(id = "A", level = 0, streak = 2, answeredInBatch = true)),
            ),
        )
        val catalog = loaded(*listOf("A", "B", "C", "D", "E").map { aCatalogPhrase(id = it) }.toTypedArray())

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.byLevel[Level(0)] shouldBe 5
    }

    // Scenario: Sin tanda activa muestra solo el total por nivel (spec: progress-overview)
    @Test
    fun `given no active batch when opening progress then batchProgress is null`() = runTest {
        // Given: no hay tanda activa y un catálogo de seis frases sin progreso
        val practice = FakePracticeRepository()
        val catalog = loaded(*(1..6).map { aCatalogPhrase(id = "phrase-$it") }.toTypedArray())

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.batchProgress shouldBe null
        available.byLevel[Level(0)] shouldBe 6
    }

    // Scenario: Catálogo ilegible al abrir el progreso (spec: progress-overview)
    @Test
    fun `given an unreadable catalog when opening progress then it returns catalog unavailable`() = runTest {
        // Given: un catálogo que no se puede leer
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)).invoke()

        // Then
        result shouldBe ProgressOverviewResult.CatalogUnavailable(CatalogLoadError.AssetUnreadable)
    }
}
