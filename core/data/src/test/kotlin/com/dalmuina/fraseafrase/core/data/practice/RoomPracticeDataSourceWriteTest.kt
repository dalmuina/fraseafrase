package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Escrituras de transact (tareas 8.4 y 8.5): la escritura en dos fases debe evitar colisiones de UNIQUE, y un fallo a
// mitad no debe dejar estado parcial.
@OptIn(ExperimentalCoroutinesApi::class)
class RoomPracticeDataSourceWriteTest {

    private lateinit var database: PracticeDatabase
    private lateinit var source: RoomPracticeDataSource

    @BeforeEach
    fun openDatabase() {
        database = inMemoryPracticeDatabase()
        source = RoomPracticeDataSource(database, UnconfinedTestDispatcher())
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    // Scenario: Cierre con reasignación cruzada de posiciones (spec: practice-persistence)
    @Test
    fun `given a certified phrase leaving position 0 and a carried phrase entering it then both are saved`() = runTest {
        // Given: A ocupa la posición 0 de la tanda; X espera en la cola en la posición 0
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "A", level = 0, batchPosition = 0))
        dao.insert(entry(id = "B", level = 0, batchPosition = 1))
        dao.insert(entry(id = "X", level = 0, carryPosition = 0))
        database.activeBatchDao().insert(ActiveBatchEntity(id = 1, capacity = 10))

        // When: A sale certificada y X entra en la posición 0 de la tanda
        source.transact { snapshot ->
            PracticeChange(
                next = snapshot.copy(
                    progress = snapshot.progress.map { if (it.id.value == "A") certified(it) else it },
                    activeBatch = ActiveBatch(order = listOf(PhraseId("X"), PhraseId("B")), capacity = BatchSize(10)),
                    carry = CarryQueue(emptyList()),
                ),
                result = Unit,
            )
        }

        // Then
        source.read().batch()?.entries?.map { it.id.value } shouldContainExactly listOf("X", "B")
    }

    // Scenario: Intercambio de dos posiciones de tanda (spec: practice-persistence)
    @Test
    fun `given two batch positions swapped then both phrases keep their new positions`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "A", level = 0, batchPosition = 0))
        dao.insert(entry(id = "B", level = 0, batchPosition = 1))
        database.activeBatchDao().insert(ActiveBatchEntity(id = 1, capacity = 10))

        // When
        source.transact { snapshot ->
            PracticeChange(
                next = snapshot.copy(
                    activeBatch = ActiveBatch(order = listOf(PhraseId("B"), PhraseId("A")), capacity = BatchSize(10)),
                ),
                result = Unit,
            )
        }

        // Then
        source.read().batch()?.entries?.map { it.id.value } shouldContainExactly listOf("B", "A")
    }

    // Scenario: Fallo a mitad del cierre no deja estado parcial (spec: practice-persistence)
    @Test
    fun `given a failure in the middle of a close then the state is identical to the state before it`() = runTest {
        // Given: A y B en la tanda; un trigger de prueba hace fallar la escritura de B después de la de A
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "A", level = 0, batchPosition = 0))
        dao.insert(entry(id = "B", level = 0, batchPosition = 1))
        database.activeBatchDao().insert(ActiveBatchEntity(id = 1, capacity = 10))
        database.useWriterConnection { transactor ->
            transactor.executeSQL(
                "CREATE TRIGGER fail_on_b BEFORE UPDATE ON phrase_progress WHEN NEW.phrase_id = 'B' " +
                    "BEGIN SELECT RAISE(ABORT, 'injected failure'); END",
            )
        }
        val before = source.read()

        // When / Then: A se certifica y B pasa a la cola; la escritura de B falla y debe deshacer también la de A
        shouldThrow<SQLiteException> {
            source.transact { snapshot ->
                PracticeChange(
                    next = snapshot.copy(
                        progress = snapshot.progress.map { if (it.id.value == "A") certified(it) else it },
                        activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10)),
                        carry = CarryQueue(listOf(PhraseId("B"))),
                    ),
                    result = Unit,
                )
            }
        }

        source.read() shouldBe before
    }

    private fun certified(progress: PhraseProgress) =
        progress.copy(
            level = Level(1),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 11),
        )

    private fun entry(
        id: String,
        level: Int,
        batchPosition: Int? = null,
        carryPosition: Int? = null,
    ) = PhraseProgressEntity(
        phraseId = id,
        level = level,
        streak = 0,
        answeredInBatch = false,
        nextReview = if (level == 0) null else "2026-03-11",
        batchPosition = batchPosition,
        carryPosition = carryPosition,
    )
}
