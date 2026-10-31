package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Room
import androidx.room3.Transactor
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// Triggers de invariantes (design D5, revisión "Triggers en onOpen"; ADR-0013). Room no conoce los triggers, así que
// se comprueba directamente en sqlite_master, y que se recrean al abrir de nuevo la base.
class PracticeTriggersTest {

    @TempDir
    lateinit var directory: Path

    // Scenario: los doce triggers existen tras abrir la base (design D5, invariantes en triggers)
    @Test
    fun `given a freshly opened database when reading sqlite_master then the twelve triggers exist`() = runTest {
        // Given
        val database = inMemoryPracticeDatabase()

        // When
        val names = database.useReaderConnection { triggerNames(it) }
        database.close()

        // Then
        names shouldContainExactlyInAnyOrder expectedTriggerNames
    }

    // Scenario: los triggers se regeneran en cada apertura, también si se han perdido (design D5, onOpen)
    @Test
    fun `given the triggers were lost when reopening the database then they are created again`() = runTest {
        // Given: una base en fichero, abierta una vez y con los triggers borrados fuera de Room
        val path = directory.resolve("practice.db").toString()
        val first = fileDatabase(path)
        first.useReaderConnection { triggerNames(it) }
        first.close()
        val raw = BundledSQLiteDriver().open(path)
        try {
            expectedTriggerNames.forEach { raw.execSQL("DROP TRIGGER $it") }
        } finally {
            raw.close()
        }

        // When: se abre de nuevo la base
        val reopened = fileDatabase(path)
        val names = reopened.useReaderConnection { triggerNames(it) }
        reopened.close()

        // Then
        names shouldContainExactlyInAnyOrder expectedTriggerNames
    }

    private fun fileDatabase(path: String): PracticeDatabase =
        Room.databaseBuilder<PracticeDatabase>(path) { PracticeDatabase_Impl() }
            .setDriver(BundledSQLiteDriver())
            .addCallback(PracticeSchema.callback)
            .build()

    private suspend fun triggerNames(transactor: Transactor): List<String> =
        transactor.usePrepared("SELECT name FROM sqlite_master WHERE type = 'trigger'") { statement ->
            buildList { while (statement.step()) add(statement.getText(0)) }
        }

    private companion object {
        val expectedTriggerNames = listOf(
            "phrase_progress_level_range",
            "phrase_progress_one_position",
            "phrase_progress_level_zero_placed",
            "phrase_progress_review",
            "active_batch_singleton",
            "active_batch_capacity",
        ).flatMap { listOf("${it}_insert", "${it}_update") }
    }
}
