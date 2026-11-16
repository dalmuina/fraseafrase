package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// Caracterización de la migración destructiva v3->v4 (design "PracticeDatabase v4", ADR-0013/ADR-0019): tras subir de
// versión, las tablas quedan recreadas con la columna target_locale nueva y vacías, sin intentar preservar datos.
class PracticeMigrationTest {

    @TempDir
    lateinit var directory: Path

    @Test
    fun `given a v3 database with data when opening it as v4 then tables are empty with target_locale columns`() =
        runTest {
            // Given: una base en fichero con el esquema v3 y una fila de progreso ya guardada
            val path = directory.resolve("practice.db").toString()
            val raw = BundledSQLiteDriver().open(path)
            try {
                raw.execSQL(
                    "CREATE TABLE IF NOT EXISTS phrase_progress (`phrase_id` TEXT NOT NULL, " +
                        "`level` INTEGER NOT NULL, `streak` INTEGER NOT NULL, " +
                        "`answered_in_batch` INTEGER NOT NULL, `due_at_session` INTEGER NOT NULL, " +
                        "`batch_position` INTEGER, PRIMARY KEY(`phrase_id`))",
                )
                raw.execSQL("INSERT INTO phrase_progress VALUES ('A', 1, 0, 0, 13, NULL)")
                raw.execSQL(
                    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)",
                )
                raw.execSQL(
                    "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                        "VALUES(42, 'f5a4dccb2f81d7cf92769abb64d00076')",
                )
                // Sin esto, Room ve `user_version = 0` (base nunca inicializada por Room) e intenta un `onCreate`
                // que choca con las tablas v3 ya existentes, en vez de disparar la migración destructiva 3→4.
                raw.execSQL("PRAGMA user_version = 3")
            } finally {
                raw.close()
            }

            // When: se abre la base con el esquema actual (v4), con migración destructiva habilitada
            val database = Room.databaseBuilder<PracticeDatabase>(path) { PracticeDatabase_Impl() }
                .setDriver(BundledSQLiteDriver())
                .addCallback(PracticeSchema.callback)
                .fallbackToDestructiveMigration()
                .build()

            // Then: la tabla queda vacía, sin el dato previo, y con la columna target_locale
            val columns = database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA table_info(phrase_progress)") { statement ->
                    buildList { while (statement.step()) add(statement.getText(1)) }
                }
            }
            database.phraseProgressDao().all(targetLocale = "pt-PT").shouldBeEmpty()
            database.close()
            columns shouldContain "target_locale"
        }
}
