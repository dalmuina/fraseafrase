package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatchResult
import com.dalmuina.fraseafrase.core.domain.practice.CloseActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.CloseBatchResult
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Ciclo completo de dos tandas con persistencia real en Room (tarea 8.6; spec practice-persistence, "Composición única
// tras el cierre", y practice-batch, "La cola sobrante entra primera tras el cierre").
@OptIn(ExperimentalCoroutinesApi::class)
class RoomPracticeCycleTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val clock = FixedClock(today)
    private lateinit var database: PracticeDatabase
    private lateinit var source: RoomPracticeDataSource

    private val catalog = CatalogLoadResult.Loaded(
        listOf("A", "B", "C", "Z").map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
    )

    @BeforeEach
    fun openDatabase() {
        database = inMemoryPracticeDatabase()
        source = RoomPracticeDataSource(database, UnconfinedTestDispatcher())
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    private suspend fun obtain() = ObtainActiveBatch(
        composeNextBatch = ComposeNextBatch(clock),
        catalogRepository = FakeCatalogRepository(catalog),
        settingsRepository = FakeSettingsRepository(UserSettings.DEFAULT),
        practiceRepository = source,
    ).invoke()

    private suspend fun close(expected: Int): CloseBatchResult =
        CloseActiveBatch(source, ResolveBatchClosure(clock)).invoke(expected)

    // Certificada hoy, con repaso mañana: no vuelve a entrar en la tanda siguiente
    private fun certified(id: String) = aPhraseProgress(
        id = id,
        level = 1,
        streak = 3,
        answeredInBatch = true,
        nextReview = today.plusDays(1),
    )

    // Scenario: La cola sobrante entra primera tras el cierre; obtener dos veces compone una sola vez
    @Test
    fun `given two batches when closing and obtaining twice then carry enters first and composes once`() = runTest {
        // Given: tanda activa con A certificada, B sin certificar y Z en la cola sobrante, todo en Room
        val initial = PracticeSnapshot(
            progress = listOf(certified("A"), aPhraseProgress(id = "B"), aPhraseProgress(id = "Z")),
            activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
            carry = CarryQueue(listOf(PhraseId("Z"))),
        )
        source.transact { PracticeChange(initial, Unit) }

        // When: se cierra con el aviso de una no certificada y se obtiene la tanda dos veces
        val closed = close(expected = 1)
        val first = obtain()
        val stateAfterFirst = source.read()
        val second = obtain()

        // Then
        closed shouldBe CloseBatchResult.Closed
        first.shouldBeInstanceOf<ActiveBatchResult.Created>()
        first.batch.entries.map { it.id.value } shouldContainExactly listOf("Z", "B", "C")
        second.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        second.batch.entries.map { it.id.value } shouldContainExactly listOf("Z", "B", "C")
        source.read() shouldBe stateAfterFirst
    }
}
