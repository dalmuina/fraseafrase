package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Registra la huella SHA-256 del documento JSON ya sembrado de cada idioma (ADR-0028). La ausencia de una fila para
 * un idioma equivale a "nunca sembrado": la primera siembra siempre se ejecuta.
 */
@Entity(tableName = "catalog_seed_version")
data class CatalogSeedVersionEntity(
    @PrimaryKey
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    @ColumnInfo(name = "content_hash")
    val contentHash: String,
)
