package com.dalmuina.fraseafrase.core.data.practice

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

/**
 * Invariantes del estado de práctica que Room no expresa con sus anotaciones (design D5, spec practice-persistence).
 * Room no tiene CHECK en sus anotaciones, así que cada invariante es un trigger BEFORE INSERT y BEFORE UPDATE que
 * aborta la sentencia. Los triggers se crean al crear la base. Los UNIQUE de posición sí son índices de Room.
 */
internal object PracticeSchema {

    private const val LEVEL_RANGE_MESSAGE = "level must be 0..5"
    private const val ONE_POSITION_MESSAGE = "a phrase cannot be in the batch and in the carry queue"
    private const val LEVEL_ZERO_MESSAGE = "a level 0 phrase must be in the batch or in the carry queue"
    private const val REVIEW_MESSAGE = "next_review must be null exactly when level is 0"
    private const val SINGLETON_MESSAGE = "active_batch has a single row with id 1"
    private const val CAPACITY_MESSAGE = "capacity must be 10..30"

    val triggers: List<String> = listOf(
        guard(
            name = "phrase_progress_level_range",
            table = "phrase_progress",
            condition = "NEW.level < 0 OR NEW.level > 5",
            message = LEVEL_RANGE_MESSAGE,
        ),
        guard(
            name = "phrase_progress_one_position",
            table = "phrase_progress",
            condition = "NEW.batch_position IS NOT NULL AND NEW.carry_position IS NOT NULL",
            message = ONE_POSITION_MESSAGE,
        ),
        guard(
            name = "phrase_progress_level_zero_placed",
            table = "phrase_progress",
            condition = "NEW.level = 0 AND NEW.batch_position IS NULL AND NEW.carry_position IS NULL",
            message = LEVEL_ZERO_MESSAGE,
        ),
        guard(
            name = "phrase_progress_review",
            table = "phrase_progress",
            condition = "(NEW.level = 0) <> (NEW.next_review IS NULL)",
            message = REVIEW_MESSAGE,
        ),
        guard(
            name = "active_batch_singleton",
            table = "active_batch",
            condition = "NEW.id <> 1",
            message = SINGLETON_MESSAGE,
        ),
        guard(
            name = "active_batch_capacity",
            table = "active_batch",
            condition = "NEW.capacity NOT BETWEEN 10 AND 30",
            message = CAPACITY_MESSAGE,
        ),
    ).flatten()

    /** Crea las invariantes al crear la base. */
    val callback: RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override suspend fun onCreate(connection: SQLiteConnection) {
            triggers.forEach { connection.execSQL(it) }
        }
    }

    /** Un trigger de inserción y otro de actualización que abortan cuando [condition] se cumple. */
    private fun guard(name: String, table: String, condition: String, message: String): List<String> =
        listOf("INSERT", "UPDATE").map { event ->
            "CREATE TRIGGER IF NOT EXISTS ${name}_${event.lowercase()} BEFORE $event ON $table " +
                "WHEN $condition BEGIN SELECT RAISE(ABORT, '$message'); END"
        }
}

/** Base en memoria para tests JVM locales, con el mismo driver SQLite que el dispositivo (ADR-0013). */
internal fun inMemoryPracticeDatabase(): PracticeDatabase =
    Room.inMemoryDatabaseBuilder<PracticeDatabase> { PracticeDatabase_Impl() }
        .setDriver(BundledSQLiteDriver())
        .addCallback(PracticeSchema.callback)
        .build()

/** Base de producción en el almacenamiento de la app. La crea `:app` mediante Hilt. */
fun createPracticeDatabase(context: Context): PracticeDatabase =
    Room.databaseBuilder(context, PracticeDatabase::class.java, "practice.db")
        .setDriver(BundledSQLiteDriver())
        .addCallback(PracticeSchema.callback)
        .build()
