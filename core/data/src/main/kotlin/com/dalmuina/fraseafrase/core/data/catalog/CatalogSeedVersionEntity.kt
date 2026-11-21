package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * Registra qué `schemaVersion` del documento JSON de cada combinación (destino, origen) ya se sembró (ADR-0019,
 * ADR-0022). [sourceLocale] usa [NO_SOURCE_LOCALE] para la fila del documento de frases destino, que no tiene un
 * origen propio, de modo que una sola tabla versiona ambos tipos de documento (design.md - "Esquema de Room").
 * La ausencia de una fila para una combinación equivale a "nunca sembrada": la primera siembra siempre se ejecuta.
 */
@Entity(tableName = "catalog_seed_version", primaryKeys = ["target_locale", "source_locale"])
data class CatalogSeedVersionEntity(
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    @ColumnInfo(name = "source_locale")
    val sourceLocale: String = NO_SOURCE_LOCALE,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
) {

    companion object {
        /** Marca de [sourceLocale] para la fila de un documento de frases destino, que no tiene origen propio. */
        const val NO_SOURCE_LOCALE = ""
    }
}
