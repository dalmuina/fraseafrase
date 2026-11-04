package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Base en memoria para tests JVM locales, con el mismo driver SQLite que el dispositivo (ADR-0013). */
internal fun inMemoryPracticeDatabase(): PracticeDatabase =
    Room.inMemoryDatabaseBuilder<PracticeDatabase> { PracticeDatabase_Impl() }
        .setDriver(BundledSQLiteDriver())
        .addCallback(PracticeSchema.callback)
        .build()
