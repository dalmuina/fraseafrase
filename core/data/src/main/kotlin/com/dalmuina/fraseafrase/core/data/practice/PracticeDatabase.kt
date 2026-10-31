package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Database
import androidx.room3.RoomDatabase

/** Base de datos del estado del usuario (ADR-0013). Versión 1: el esquema se exporta a `core/data/schemas`. */
@Database(
    entities = [PhraseProgressEntity::class, ActiveBatchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PracticeDatabase : RoomDatabase() {

    abstract fun phraseProgressDao(): PhraseProgressDao

    abstract fun activeBatchDao(): ActiveBatchDao
}
