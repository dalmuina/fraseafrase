package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Base de datos del estado del usuario (ADR-0019, ADR-0030). Versión 1, la de 1.0.0: ADR-0030 retoma la versión
 * inicial 1 que fijó el ADR-0013, ya sustituido, y los cambios de esquema de la fase pre-1.0.0 se fusionaron en ella
 * (design D1 de `reset-database-v1`). El esquema se exporta a `core/data/schemas`. Cualquier subida de versión
 * necesita su migración y sus tests (ADR-0030).
 */
@Database(
    entities = [PhraseProgressEntity::class, ActiveBatchEntity::class, SessionCounterEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PracticeDatabase : RoomDatabase() {

    abstract fun phraseProgressDao(): PhraseProgressDao

    abstract fun activeBatchDao(): ActiveBatchDao

    abstract fun sessionCounterDao(): SessionCounterDao
}
