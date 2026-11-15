package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Registra qué `schemaVersion` del documento JSON de cada idioma destino ya se sembró (ADR-0019). Su ausencia para
 * un idioma destino equivale a "nunca sembrado": la primera siembra siempre se ejecuta.
 */
@Entity(tableName = "catalog_seed_version")
data class CatalogSeedVersionEntity(
    @PrimaryKey
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
)
