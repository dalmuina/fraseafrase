package com.dalmuina.fraseafrase.core.domain.practice

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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Ciclo completo entre cierre y composición (spec practice-persistence, "Composición única tras el cierre";
// design D4). Persistencia en fake; la persistencia real en Room va en :core:data (tarea 8.6).
class CloseThenObtainTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val clock = FixedClock(today)

    private val catalog = CatalogLoadResult.Loaded(
        listOf("A", "B", "C").map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
    )

    private fun obtain(practice: FakePracticeRepository) = ObtainActiveBatch(
        composeNextBatch = ComposeNextBatch(clock),
        catalogRepository = FakeCatalogRepository(catalog),
        settingsRepository = FakeSettingsRepository(UserSettings.DEFAULT),
        practiceRepository = practice,
    )

    private suspend fun close(practice: FakePracticeRepository, expected: Int) =
        CloseActiveBatch(practice, ResolveBatchClosure(clock)).invoke(expected)

    // Certificada hoy, con repaso mañana: no vuelve a entrar en la tanda siguiente
    private fun certified(id: String) = aPhraseProgress(
        id = id,
        level = 1,
        streak = 3,
        answeredInBatch = true,
        nextReview = today.plusDays(1),
    )

    // Scenario: Cierre sin composición posible deja la tanda sin activar (spec: practice-persistence)
    @Test
    fun `given a closed batch and no new phrases left when obtaining then returns NothingAvailable`() = runTest {
        // Given: tanda con A y B certificadas y el catálogo solo tiene esas dos frases
        val onlyAB = CatalogLoadResult.Loaded(listOf("A", "B").map { aCatalogPhrase(id = it, level = CefrLevel.A1) })
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(certified("A"), certified("B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                carry = CarryQueue(emptyList()),
            ),
        )
        close(practice, expected = 0)

        // When
        val result = ObtainActiveBatch(
            composeNextBatch = ComposeNextBatch(clock),
            catalogRepository = FakeCatalogRepository(onlyAB),
            settingsRepository = FakeSettingsRepository(UserSettings.DEFAULT),
            practiceRepository = practice,
        ).invoke()

        // Then
        result shouldBe ActiveBatchResult.NothingAvailable
        practice.read().activeBatch shouldBe null
    }

    // Scenario: Obtener dos veces tras el cierre compone una sola vez (spec: practice-persistence)
    @Test
    fun `given a closed batch when obtaining twice then the next batch is composed once`() = runTest {
        // Given: tanda con A y B certificadas; C es nueva y está en el catálogo
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(certified("A"), certified("B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                carry = CarryQueue(emptyList()),
            ),
        )
        close(practice, expected = 0)

        // When
        val first = obtain(practice).invoke()
        val stateAfterFirst = practice.read()
        val second = obtain(practice).invoke()

        // Then
        first.shouldBeInstanceOf<ActiveBatchResult.Created>()
        first.batch.entries.map { it.id.value } shouldContainExactly listOf("C")
        second.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        second.batch.entries.map { it.id.value } shouldContainExactly listOf("C")
        practice.read() shouldBe stateAfterFirst
    }
}
