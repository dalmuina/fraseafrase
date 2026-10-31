package com.dalmuina.fraseafrase.core.data.practice

import androidx.sqlite.SQLiteException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Invariantes del esquema de práctica (spec practice-persistence, design D5). Room no expresa CHECK en sus
// anotaciones: las invariantes se aplican con triggers creados en onCreate, y los UNIQUE, con índices de Room.
class PracticeSchemaTest {

    private lateinit var database: PracticeDatabase

    @BeforeEach
    fun openDatabase() {
        database = inMemoryPracticeDatabase()
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    // Scenario: Frase con ambas posiciones se rechaza (spec: practice-persistence)
    @Test
    fun `given a phrase with both batch and carry positions when saving then throws SQLiteException`() = runTest {
        // Given
        val both = progress(id = "A", level = 0, batchPosition = 0, carryPosition = 0)

        // When / Then
        shouldThrow<SQLiteException> { database.phraseProgressDao().insert(both) }
    }

    // Scenario: Posición repetida se rechaza (spec: practice-persistence)
    @Test
    fun `given two phrases with the same batch position when saving then throws SQLiteException`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(progress(id = "A", level = 0, batchPosition = 0))

        // When / Then
        shouldThrow<SQLiteException> { dao.insert(progress(id = "B", level = 0, batchPosition = 0)) }
    }

    // Scenario: Frase de nivel 0 fuera de tanda y cola se rechaza (spec: practice-persistence)
    @Test
    fun `given a level 0 phrase outside the batch and the queue when saving then throws SQLiteException`() = runTest {
        // Given
        val unplaced = progress(id = "A", level = 0)

        // When / Then
        shouldThrow<SQLiteException> { database.phraseProgressDao().insert(unplaced) }
    }

    @Test
    fun `given a level 1 phrase without next review when saving then throws SQLiteException`() = runTest {
        // Given
        val withoutReview = progress(id = "A", level = 1, carryPosition = 0, nextReview = null)

        // When / Then
        shouldThrow<SQLiteException> { database.phraseProgressDao().insert(withoutReview) }
    }

    // Scenario: Capacidad fuera de rango se rechaza (spec: practice-persistence)
    @Test
    fun `given a batch capacity of 31 when saving then throws SQLiteException`() = runTest {
        // Given
        val capacity31 = ActiveBatchEntity(id = 1, capacity = 31)

        // When / Then
        shouldThrow<SQLiteException> { database.activeBatchDao().insert(capacity31) }
    }

    // Scenario: Sin tanda activa / tanda activa única (spec: practice-persistence)
    @Test
    fun `given an active batch row with id 2 when saving then throws SQLiteException`() = runTest {
        // Given
        val secondRow = ActiveBatchEntity(id = 2, capacity = 20)

        // When / Then
        shouldThrow<SQLiteException> { database.activeBatchDao().insert(secondRow) }
    }

    // Caracterización del esquema (tarea 8.6): estados válidos que deben aceptarse
    @Test
    fun `given a new phrase placed in the active batch when saving then is accepted`() = runTest {
        // Given
        val newPhrase = progress(id = "A", level = 0, batchPosition = 0)

        // When
        database.phraseProgressDao().insert(newPhrase)

        // Then
        database.phraseProgressDao().all().map { it.phraseId } shouldContainExactly listOf("A")
    }

    @Test
    fun `given a level 3 phrase left in the carry queue when saving then is accepted`() = runTest {
        // Given
        val carried = progress(id = "A", level = 3, carryPosition = 0, nextReview = "2026-03-20")

        // When
        database.phraseProgressDao().insert(carried)

        // Then
        database.phraseProgressDao().all().map { it.phraseId } shouldContainExactly listOf("A")
    }

    private fun progress(
        id: String,
        level: Int,
        batchPosition: Int? = null,
        carryPosition: Int? = null,
        nextReview: String? = if (level == 0) null else "2026-03-11",
    ) = PhraseProgressEntity(
        phraseId = id,
        level = level,
        streak = 0,
        answeredInBatch = false,
        nextReview = nextReview,
        batchPosition = batchPosition,
        carryPosition = carryPosition,
    )
}
