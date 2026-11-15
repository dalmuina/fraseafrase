package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Invariantes del esquema de práctica (spec practice-persistence, design D5). Room no expresa CHECK en sus
// anotaciones: las invariantes se aplican con triggers creados en onOpen, y los UNIQUE, con índices de Room.
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

    // Scenario: Posición repetida se rechaza (spec: practice-persistence)
    @Test
    fun `given two phrases with the same batch position when saving then throws SQLiteException`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(progress(id = "A", level = 0, batchPosition = 0))

        // When / Then
        shouldThrow<SQLiteException> { dao.insert(progress(id = "B", level = 0, batchPosition = 0)) }
    }

    // Scenario: La misma posición es válida en idiomas destino distintos (spec: practice-persistence)
    @Test
    fun `given a phrase at position 0 for pt-PT when saving a phrase at position 0 for pt-BR then is accepted`() =
        runTest {
            // Given
            val dao = database.phraseProgressDao()
            dao.insert(progress(id = "A", level = 0, batchPosition = 0, targetLocale = "pt-PT"))

            // When
            dao.insert(progress(id = "A", level = 0, batchPosition = 0, targetLocale = "pt-BR"))

            // Then
            dao.all(targetLocale = "pt-BR").map { it.phraseId } shouldContainExactly listOf("A")
        }

    // Scenario: Capacidad fuera de rango se rechaza (spec: practice-persistence)
    @Test
    fun `given a batch capacity of 31 when saving then throws SQLiteException`() = runTest {
        // Given
        val capacity31 = ActiveBatchEntity(targetLocale = "pt-PT", capacity = 31)

        // When / Then
        shouldThrow<SQLiteException> { database.activeBatchDao().insert(capacity31) }
    }

    // Scenario: Tanda activa única (spec: practice-persistence) - la PK target_locale impide una segunda fila para
    // el mismo idioma destino
    @Test
    fun `given an active batch row for pt-PT when saving another row for pt-PT then throws SQLiteException`() =
        runTest {
            // Given
            database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-PT", capacity = 10))

            // When / Then
            shouldThrow<SQLiteException> {
                database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-PT", capacity = 20))
            }
        }

    // Scenario: La tanda activa de un idioma destino no afecta a la de otro (spec: practice-persistence)
    @Test
    fun `given an active batch row for pt-PT when saving a row for pt-BR then both are accepted`() = runTest {
        // Given
        database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-PT", capacity = 10))

        // When
        database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-BR", capacity = 20))

        // Then
        database.activeBatchDao().byLocale("pt-PT").shouldNotBeNull()
        database.activeBatchDao().byLocale("pt-BR").shouldNotBeNull()
    }

    // Caracterización del esquema (tarea 8.6): estados válidos que deben aceptarse
    @Test
    fun `given a new phrase placed in the active batch when saving then is accepted`() = runTest {
        // Given
        val newPhrase = progress(id = "A", level = 0, batchPosition = 0)

        // When
        database.phraseProgressDao().insert(newPhrase)

        // Then
        database.phraseProgressDao().all(targetLocale = "pt-PT").map { it.phraseId } shouldContainExactly listOf("A")
    }

    @Test
    fun `given a level 3 phrase outside the active batch when saving then is accepted`() = runTest {
        // Given
        val dormant = progress(id = "A", level = 3, dueAtSession = 7)

        // When
        database.phraseProgressDao().insert(dormant)

        // Then
        database.phraseProgressDao().all(targetLocale = "pt-PT").map { it.phraseId } shouldContainExactly listOf("A")
    }

    // Cada guarda también se dispara en UPDATE (design D5, revisión de triggers). Se inserta una fila válida y se
    // actualiza a una inválida con SQL directo, porque un UPDATE de Room va por clave primaria.
    @Test
    fun `given a valid phrase when updating its level to 6 then throws SQLiteException`() = runTest {
        // Given
        database.phraseProgressDao().insert(progress(id = "A", level = 1))

        // When / Then
        shouldThrow<SQLiteException> {
            executeSql("UPDATE phrase_progress SET level = 6 WHERE phrase_id = 'A'")
        }
    }

    @Test
    fun `given an active batch when raising its capacity to 31 then throws SQLiteException`() = runTest {
        // Given
        database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-PT", capacity = 10))

        // When / Then
        shouldThrow<SQLiteException> {
            executeSql("UPDATE active_batch SET capacity = 31 WHERE target_locale = 'pt-PT'")
        }
    }

    private suspend fun executeSql(sql: String) {
        database.useWriterConnection { transactor -> transactor.executeSQL(sql) }
    }

    private fun progress(
        id: String,
        level: Int,
        batchPosition: Int? = null,
        dueAtSession: Int = 0,
        targetLocale: String = "pt-PT",
    ) = PhraseProgressEntity(
        phraseId = id,
        targetLocale = targetLocale,
        level = level,
        streak = 0,
        answeredInBatch = false,
        dueAtSession = dueAtSession,
        batchPosition = batchPosition,
    )
}
