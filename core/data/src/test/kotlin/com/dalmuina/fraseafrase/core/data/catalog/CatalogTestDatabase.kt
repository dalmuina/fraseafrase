package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Base en memoria para tests JVM locales, con el mismo driver SQLite que el dispositivo (ADR-0013, ADR-0019). */
internal fun inMemoryCatalogDatabase(): CatalogDatabase =
    Room.inMemoryDatabaseBuilder<CatalogDatabase> { CatalogDatabase_Impl() }
        .setDriver(BundledSQLiteDriver())
        .build()
