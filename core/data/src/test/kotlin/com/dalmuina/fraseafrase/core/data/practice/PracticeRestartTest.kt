package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.LocalDate

// Escenario "Progreso sobrevive al reinicio" de practice-persistence: el progreso se lee igual tras cerrar y reabrir
// la base en disco.
class PracticeRestartTest {

    @TempDir
    lateinit var directory: Path

    // Scenario: Progreso sobrevive al reinicio (spec: practice-persistence)
    @Test
    fun `given progress saved in a file database when reopening it then level and review date are kept`() = runTest {
        // Given: una frase de nivel 1 con repaso el 11-03 guardada en la base en disco
        val path = directory.resolve("practice.db").toString()
        val first = fileDatabase(path)
        first.phraseProgressDao().insert(
            PhraseProgressEntity(
                phraseId = "A",
                level = 1,
                streak = 0,
                answeredInBatch = false,
                nextReview = "2026-03-11",
                batchPosition = null,
                carryPosition = null,
            ),
        )
        first.close()

        // When: se reabre la base y se lee el estado
        val reopened = fileDatabase(path)
        val progress = RoomPracticeDataSource(reopened, UnconfinedTestDispatcher()).read().progress
        reopened.close()

        // Then
        val phrase = progress.single { it.id == PhraseId("A") }
        phrase.level.value shouldBe 1
        phrase.nextReview shouldBe LocalDate.of(2026, 3, 11)
    }

    private fun fileDatabase(path: String): PracticeDatabase =
        Room.databaseBuilder<PracticeDatabase>(path) { PracticeDatabase_Impl() }
            .setDriver(BundledSQLiteDriver())
            .addCallback(PracticeSchema.callback)
            .build()
}
