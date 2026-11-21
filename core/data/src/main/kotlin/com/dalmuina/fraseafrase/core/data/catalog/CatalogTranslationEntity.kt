package com.dalmuina.fraseafrase.core.data.catalog

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/**
 * Fila de traducción sembrada desde un documento de traducción (ADR-0022): el texto de origen de una frase
 * destino desde un `sourceLocale` concreto. Una combinación destino/origen se siembra y versiona de forma
 * independiente de cualquier otra, y de las frases destino de [CatalogPhraseEntity] (design.md - "Esquema de
 * Room").
 */
@Entity(
    tableName = "catalog_translation",
    primaryKeys = ["id", "target_locale", "source_locale"],
)
data class CatalogTranslationEntity(
    val id: String,
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    @ColumnInfo(name = "source_locale")
    val sourceLocale: String,
    val source: String,
)
