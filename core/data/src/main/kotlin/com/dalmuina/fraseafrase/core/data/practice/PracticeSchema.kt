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
 * aborta la sentencia. Los triggers se crean en cada apertura con `IF NOT EXISTS`, no solo al crear la base: una
 * migración que recrea tablas los elimina sin aviso y Room no los conoce (design D5, revisión). Los UNIQUE de posición
 * sí son índices de Room. La PK `target_locale` de `active_batch` garantiza como máximo una fila por idioma destino,
 * sin trigger de singleton (design "PracticeDatabase v4" de `add-multi-language-catalog`).
 */
internal object PracticeSchema {

    private const val LEVEL_RANGE_MESSAGE = "level must be 0..5"
    private const val CAPACITY_MESSAGE = "capacity must be 10..30"

    val triggers: List<String> = listOf(
        guard(
            name = "phrase_progress_level_range",
            table = "phrase_progress",
            condition = "NEW.level < 0 OR NEW.level > 5",
            message = LEVEL_RANGE_MESSAGE,
        ),
        guard(
            name = "active_batch_capacity",
            table = "active_batch",
            condition = "NEW.capacity NOT BETWEEN 10 AND 30",
            message = CAPACITY_MESSAGE,
        ),
    ).flatten()

    /** Crea las invariantes en cada apertura de la base. `IF NOT EXISTS` las deja intactas si ya existen. */
    val callback: RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override suspend fun onOpen(connection: SQLiteConnection) {
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

/**
 * Base de producción en el almacenamiento de la app. La crea `:app` mediante Hilt. Sin fallback destructivo de
 * migración (ADR-0030): una versión sin camino de migración hace fallar la apertura en vez de borrar el progreso
 * del usuario.
 */
fun createPracticeDatabase(context: Context): PracticeDatabase =
    Room.databaseBuilder(context, PracticeDatabase::class.java, "practice.db")
        .setDriver(BundledSQLiteDriver())
        .addCallback(PracticeSchema.callback)
        .build()
