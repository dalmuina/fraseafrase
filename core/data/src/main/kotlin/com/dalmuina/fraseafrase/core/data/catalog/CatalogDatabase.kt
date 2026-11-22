package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.Database
import androidx.room3.RoomDatabase

/**
 * Base de datos del catálogo (ADR-0013, ADR-0019), independiente de `PracticeDatabase`: el versionado de un
 * contenido reseedable no debe afectar al de un estado crítico del usuario. El esquema se exporta a
 * `core/data/schemas`.
 */
@Database(
    entities = [CatalogPhraseEntity::class, CatalogSeedVersionEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class CatalogDatabase : RoomDatabase() {

    abstract fun catalogDao(): CatalogDao
}
