package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomPracticeDataSourceReadTest {

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

    // Scenario: Progreso sobrevive al reinicio (spec: practice-persistence)
    @Test
    fun `given a saved phrase progress when reading then the phrase keeps its level and due session`() = runTest {
        // Given
        database.phraseProgressDao().insert(
            PhraseProgressEntity(
                phraseId = "A",
                level = 2,
                streak = 0,
                answeredInBatch = false,
                dueAtSession = 13,
                batchPosition = null,
            ),
        )

        // When
        val snapshot = source.read()

        // Then
        val phrase = snapshot.progress.single()
        phrase.id.value shouldBe "A"
        phrase.level.value shouldBe 2
        phrase.dueAtSession shouldBe 13
    }

    // Scenario: Sin contador guardado el valor por defecto es 0 (spec: practice-persistence)
    @Test
    fun `given no session counter row when reading then the session count is 0`() = runTest {
        // When
        val snapshot = source.read()

        // Then
        snapshot.sessionCount shouldBe 0
    }

    // Scenario: Orden de la tanda activa (spec: practice-persistence)
    @Test
    fun `given an active batch saved in positions when reading then the order follows the positions`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "B", batchPosition = 1))
        dao.insert(entry(id = "A", batchPosition = 0))
        dao.insert(entry(id = "C", batchPosition = 2))
        database.activeBatchDao().insert(ActiveBatchEntity(id = 1, capacity = 10))

        // When
        val batch = source.read().batch()

        // Then
        batch.shouldNotBeNull()
        batch.entries.map { it.id.value } shouldContainExactly listOf("A", "B", "C")
        batch.capacity shouldBe BatchSize(10)
    }

    // Scenario: Sin tanda activa (spec: practice-persistence)
    @Test
    fun `given no active batch row when reading then the snapshot reports no active batch`() = runTest {
        // Given
        database.phraseProgressDao().insert(entry(id = "A", level = 1, dueAtSession = 1))

        // When
        val snapshot = source.read()

        // Then
        snapshot.activeBatch.shouldBeNull()
    }

    // Scenario: Lectura coherente del estado (spec: practice-persistence). Las lecturas se hacen tras cada escritura
    // completa: nunca se observa una mezcla. La prueba de lectura concurrente con un cierre llega con la tarea 8.8.
    @Test
    fun `given a completed state change when reading then every read sees either the old or the new state`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "A"))
        dao.insert(entry(id = "B"))
        val before = source.read()

        // When
        dao.update(entry(id = "A", level = 1, dueAtSession = 1))
        val after = source.read()
        val observed = (1..20).map { source.read() }

        // Then
        observed.forEach { snapshot -> (snapshot == before || snapshot == after) shouldBe true }
    }

    private fun entry(
        id: String,
        level: Int = 0,
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
