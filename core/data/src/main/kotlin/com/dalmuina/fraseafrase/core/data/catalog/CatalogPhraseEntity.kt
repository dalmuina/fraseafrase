package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * Fila de frase destino sembrada desde un documento de frases destino (ADR-0019, ADR-0022). No lleva `source`: el
 * texto de origen vive en [CatalogTranslationEntity], en una tabla independiente por combinación destino/origen.
 * [position] conserva el orden de catálogo del documento original: una consulta SQL no lo garantiza por sí sola.
 */
@Entity(
    tableName = "catalog_phrase",
    primaryKeys = ["id", "target_locale"],
)
data class CatalogPhraseEntity(
    val id: String,
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    val position: Int,
    val level: String,
    val topic: String,
    val target: String,
)
