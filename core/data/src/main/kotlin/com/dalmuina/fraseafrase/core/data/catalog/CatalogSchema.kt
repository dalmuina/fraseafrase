package com.dalmuina.fraseafrase.core.data.catalog

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Base de producción en el almacenamiento de la app (design "Catálogo: nueva `CatalogDatabase`...", ADR-0019). La
 * crea `:app` mediante Hilt. No necesita invariantes propias (a diferencia de `PracticeDatabase`): Room no expresa
 * aquí ninguna regla que sus anotaciones no cubran ya.
 */
fun createCatalogDatabase(context: Context): CatalogDatabase =
    Room.databaseBuilder(context, CatalogDatabase::class.java, "catalog.db")
        .setDriver(BundledSQLiteDriver())
        // Proyecto pre-1.0.0, sin usuarios en producción: una futura subida de versión se recrea en vez de
        // migrarse (mismo precedente que `createPracticeDatabase`).
        .fallbackToDestructiveMigration()
        .build()
