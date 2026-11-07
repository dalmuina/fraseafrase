package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Escrituras de transact (spec practice-persistence, "Orden de la tanda activa" y "Composición atómica de la tanda
// siguiente"): la escritura en dos fases debe evitar colisiones de UNIQUE de batch_position, y un fallo a mitad no
// debe dejar estado parcial.
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

    // Scenario: Fallo a mitad de la operación no deja estado parcial (spec: practice-persistence)
    @Test
    fun `given a failure in the middle of generating a session then the state is identical to the state before it`() =
        runTest {
            // Given: A, B y C en la tanda activa, con sessionCount 5; un trigger de prueba hace fallar la escritura
            // final de B cuando sale de la tanda (su posición queda nula), que ocurre después de la de A
            val dao = database.phraseProgressDao()
            dao.insert(entry(id = "A", level = 0, batchPosition = 0))
            dao.insert(entry(id = "B", level = 0, batchPosition = 1))
            dao.insert(entry(id = "C", level = 0, batchPosition = 2))
            database.activeBatchDao().insert(ActiveBatchEntity(id = 1, capacity = 10))
            database.sessionCounterDao().insert(SessionCounterEntity(id = 1, sessionCount = 5))
            database.useWriterConnection { transactor ->
                transactor.executeSQL(
                    "CREATE TRIGGER fail_on_b BEFORE UPDATE ON phrase_progress " +
                        "WHEN NEW.phrase_id = 'B' AND NEW.batch_position IS NULL " +
                        "BEGIN SELECT RAISE(ABORT, 'injected failure'); END",
                )
            }
            val before = source.read()

            // When / Then: se genera sesión, lo que vacía la tanda y la vuelve a componer solo con A, e incrementa
            // el contador; la escritura final de B falla y debe deshacer también la de A y la del contador
            shouldThrow<SQLiteException> {
                source.transact { snapshot ->
                    PracticeChange(
                        next = snapshot.copy(
                            activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10)),
                            sessionCount = snapshot.sessionCount + 1,
                        ),
                        result = Unit,
                    )
                }
            }

            source.read() shouldBe before
        }

    private fun entry(
        id: String,
        level: Int,
        batchPosition: Int? = null,
        dueAtSession: Int = 0,
    ) = PhraseProgressEntity(
        phraseId = id,
        level = level,
        streak = 0,
        answeredInBatch = false,
        dueAtSession = dueAtSession,
        batchPosition = batchPosition,
    )
}
