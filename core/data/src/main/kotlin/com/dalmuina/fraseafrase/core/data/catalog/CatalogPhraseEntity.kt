package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * Fila de catálogo sembrada desde un documento JSON (ADR-0013, ADR-0019, design "Catálogo: nueva
 * CatalogDatabase..."). La PK incluye `sourceLocale` desde ahora para no necesitar una migración el día que un
 * idioma destino tenga más de un origen. [position] conserva el orden de catálogo del documento original: una
 * consulta SQL no lo garantiza por sí sola.
 */
@Entity(
    tableName = "catalog_phrase",
    primaryKeys = ["id", "target_locale", "source_locale"],
)
data class CatalogPhraseEntity(
    val id: String,
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    @ColumnInfo(name = "source_locale")
    val sourceLocale: String,
    val position: Int,
    val level: String,
    val topic: String,
    val target: String,
    val source: String,
)
