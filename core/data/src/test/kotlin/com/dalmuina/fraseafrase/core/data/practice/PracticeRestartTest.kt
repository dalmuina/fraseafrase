package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import com.dalmuina.fraseafrase.core.testing.anAnswerHistory
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// Escenarios "Progreso sobrevive al reinicio" y "El contador sobrevive al reinicio" de practice-persistence: el
// progreso y el contador de sesiones se leen igual tras cerrar y reabrir la base en disco.
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeRestartTest {

    @TempDir
    lateinit var directory: Path

    // Scenario: Progreso sobrevive al reinicio (spec: practice-persistence)
    @Test
    fun `given progress saved in a file database when reopening it then level and due session are kept`() = runTest {
        // Given: una frase de nivel 1 con vencimiento en la sesión 13 guardada en la base en disco
        val path = directory.resolve("practice.db").toString()
        val first = fileDatabase(path)
        first.phraseProgressDao().insert(
            PhraseProgressEntity(
                phraseId = "A",
                targetLocale = "pt-PT",
                level = 1,
                streak = 0,
                answeredInBatch = false,
                dueAtSession = 13,
                batchPosition = null,
            ),
        )
        first.close()

        // When: se reabre la base y se lee el estado
        val reopened = fileDatabase(path)
        val progress = RoomPracticeDataSource(reopened, UnconfinedTestDispatcher())
            .read(LanguageTag("pt-PT"))
            .progress
        reopened.close()

        // Then
        val phrase = progress.single { it.id == PhraseId("A") }
        phrase.level.value shouldBe 1
        phrase.dueAtSession shouldBe 13
    }

    // Scenario: El contador sobrevive al reinicio (spec: practice-persistence)
    @Test
    fun `given a saved session counter in a file database when reopening it then its value is kept`() = runTest {
        // Given: un contador de sesiones generadas igual a 7 guardado en la base en disco
        val path = directory.resolve("practice.db").toString()
        val first = fileDatabase(path)
        first.sessionCounterDao().insert(SessionCounterEntity(targetLocale = "pt-PT", sessionCount = 7))
        first.close()

        // When: se reabre la base y se lee el estado
        val reopened = fileDatabase(path)
        val sessionCount = RoomPracticeDataSource(reopened, UnconfinedTestDispatcher())
            .read(LanguageTag("pt-PT"))
            .sessionCount
        reopened.close()

        // Then
        sessionCount shouldBe 7
    }

    // Scenario: El registro de respuestas sobrevive al reinicio (spec: practice-persistence)
    @Test
    fun `given answer histories saved in a file database when reopening it then they are kept`() = runTest {
        // Given: una frase con registro de respuestas y otra sin él guardadas en la base en disco
        val path = directory.resolve("practice.db").toString()
        val ptPt = LanguageTag("pt-PT")
        val history = anAnswerHistory(lastAnsweredSession = 12, consecutiveSessions = 3, lastMissSession = 11)
        val first = fileDatabase(path)
        RoomPracticeDataSource(first, UnconfinedTestDispatcher()).transact(ptPt) { snapshot ->
            val phrases = listOf(aPhraseProgress(id = "A").copy(answerHistory = history), aPhraseProgress(id = "B"))
            PracticeChange(next = snapshot.copy(progress = phrases), result = Unit)
        }
        first.close()

        // When: se reabre la base y se lee el estado
        val reopened = fileDatabase(path)
        val progress = RoomPracticeDataSource(reopened, UnconfinedTestDispatcher()).read(ptPt).progress
        reopened.close()

        // Then
        progress.single { it.id == PhraseId("A") }.answerHistory shouldBe history
        progress.single { it.id == PhraseId("B") }.answerHistory.shouldBeNull()
    }

    private fun fileDatabase(path: String): PracticeDatabase =
        Room.databaseBuilder<PracticeDatabase>(path) { PracticeDatabase_Impl() }
            .setDriver(BundledSQLiteDriver())
            .addCallback(PracticeSchema.callback)
            .build()
}
