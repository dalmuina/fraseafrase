package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * Fila de frase sembrada desde un `phrases-<idioma>.json` (ADR-0019). No lleva `source`: el texto de origen se
 * obtiene uniendo esta tabla consigo misma por `id`, con el `target` de la fila del idioma de origen.
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
