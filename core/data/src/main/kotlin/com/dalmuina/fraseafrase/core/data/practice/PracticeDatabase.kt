package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Base de datos del estado del usuario (ADR-0013, ADR-0017). Versión 3: el esquema se exporta a `core/data/schemas`.
 * La versión 1 a la 2 es una migración destructiva (design D7 de `repeat-by-session-counter`): retira la cola de
 * arrastradas y `next_review`, los sustituye por `due_at_session` y añade el contador de sesiones generadas, sin
 * usuarios reales a los que afecte. La versión 2 a la 3 sigue el mismo precedente (design `redesign-app-ui`,
 * "Contador de la tanda completada"): añade a `active_batch` las columnas de
 * [com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome], sin migración real.
 */
@Database(
    entities = [PhraseProgressEntity::class, ActiveBatchEntity::class, SessionCounterEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class PracticeDatabase : RoomDatabase() {

    abstract fun phraseProgressDao(): PhraseProgressDao

    abstract fun activeBatchDao(): ActiveBatchDao

    abstract fun sessionCounterDao(): SessionCounterDao
}
