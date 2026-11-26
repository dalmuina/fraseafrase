package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.useWriterConnection
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import io.kotest.assertions.throwables.shouldThrow
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
    private val ptPt = LanguageTag("pt-PT")
    private val enGb = LanguageTag("en-GB")

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
        database.phraseProgressDao().insert(entry(id = "A", level = 2, dueAtSession = 13))

        // When
        val snapshot = source.read(ptPt)

        // Then
        val phrase = snapshot.progress.single()
        phrase.id.value shouldBe "A"
        phrase.level.value shouldBe 2
        phrase.dueAtSession shouldBe 13
    }

    // Scenario: El progreso de dos idiomas destino para la misma frase no se mezcla (spec: practice-persistence)
    @Test
    fun `given the same phrase with different levels per locale when reading each locale then levels do not mix`() =
        runTest {
            // Given
            database.phraseProgressDao().insert(entry(id = "a1-saludos-001", level = 3, targetLocale = "pt-PT"))
            database.phraseProgressDao().insert(entry(id = "a1-saludos-001", level = 0, targetLocale = "en-GB"))

            // When
            val ptPtLevel = source.read(ptPt).progress.single().level.value
            val enGbLevel = source.read(enGb).progress.single().level.value

            // Then
            ptPtLevel shouldBe 3
            enGbLevel shouldBe 0
        }

    // Scenario: Sin contador guardado el valor por defecto es 0 (spec: practice-persistence)
    @Test
    fun `given no session counter row when reading then the session count is 0`() = runTest {
        // When
        val snapshot = source.read(ptPt)

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
        database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-PT", capacity = 10))

        // When
        val batch = source.read(ptPt).batch()

        // Then
        batch.shouldNotBeNull()
        batch.entries.map { it.id.value } shouldContainExactly listOf("A", "B", "C")
        batch.capacity shouldBe BatchSize(10)
    }

    // Scenario: La tanda activa de un idioma destino no afecta a la de otro (spec: practice-persistence)
    @Test
    fun `given an active batch for pt-PT when reading en-GB then it reports no active batch`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "A", batchPosition = 0, targetLocale = "pt-PT"))
        database.activeBatchDao().insert(ActiveBatchEntity(targetLocale = "pt-PT", capacity = 10))

        // When
        val snapshot = source.read(enGb)

        // Then
        snapshot.activeBatch.shouldBeNull()
    }

    // Scenario: Sin tanda activa (spec: practice-persistence)
    @Test
    fun `given no active batch row when reading then the snapshot reports no active batch`() = runTest {
        // Given
        database.phraseProgressDao().insert(entry(id = "A", level = 1, dueAtSession = 1))

        // When
        val snapshot = source.read(ptPt)

        // Then
        snapshot.activeBatch.shouldBeNull()
    }

    // Scenario: columnas nuevas de BatchOutcome en una base recién creada (design: "Contador de la tanda
    // completada", precedente de fallbackToDestructiveMigration() de PracticeDatabase)
    @Test
    fun `given a freshly created active batch when reading then the batch outcome is zero`() = runTest {
        // Given
        database.activeBatchDao().insert(
            ActiveBatchEntity(targetLocale = "pt-PT", capacity = 10, hits = 0, misses = 0, levelUps = 0),
        )

        // When
        val snapshot = source.read(ptPt)

        // Then
        snapshot.batchOutcome shouldBe BatchOutcome()
    }

    // Scenario: Lectura coherente del estado (spec: practice-persistence). Las lecturas se hacen tras cada escritura
    // completa: nunca se observa una mezcla. La prueba de lectura concurrente con un cierre llega con la tarea 8.8.
    @Test
    fun `given a completed state change when reading then every read sees either the old or the new state`() = runTest {
        // Given
        val dao = database.phraseProgressDao()
        dao.insert(entry(id = "A"))
        dao.insert(entry(id = "B"))
        val before = source.read(ptPt)

        // When
        dao.update(entry(id = "A", level = 1, dueAtSession = 1))
        val after = source.read(ptPt)
        val observed = (1..20).map { source.read(ptPt) }

        // Then
        observed.forEach { snapshot -> (snapshot == before || snapshot == after) shouldBe true }
    }

    // Scenario: Sesiones seguidas sin sesión respondida se rechazan (spec: spaced-repetition)
    @Test
    fun `given consecutive sessions without an answered session when reading then the row is rejected`() = runTest {
        // Given
        database.phraseProgressDao().insert(entry(id = "A"))
        execute("UPDATE phrase_progress SET consecutive_sessions = 2")

        // When / Then
        shouldThrow<IllegalArgumentException> { source.read(ptPt) }
    }

    // Invariante de la sesión fallada sin sesión respondida (design D4 de add-anti-stall-rotation)
    @Test
    fun `given a miss session without an answered session when reading then the row is rejected`() = runTest {
        // Given
        database.phraseProgressDao().insert(entry(id = "A"))
        execute("UPDATE phrase_progress SET last_miss_session = 4")

        // When / Then
        shouldThrow<IllegalArgumentException> { source.read(ptPt) }
    }

    private suspend fun execute(sql: String) {
        database.useWriterConnection { connection -> connection.usePrepared(sql) { it.step() } }
    }

    private fun entry(
        id: String,
        level: Int = 0,
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
