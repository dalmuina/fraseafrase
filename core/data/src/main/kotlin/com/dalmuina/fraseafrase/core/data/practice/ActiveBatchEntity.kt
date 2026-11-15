package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Tanda activa: como máximo una fila por idioma destino (ADR-0013, ADR-0019, design D5/"PracticeDatabase v4"). La PK
 * es `target_locale`: su existencia para un idioma destino indica que ese idioma destino tiene tanda activa, y la
 * propia PK impide una segunda fila para el mismo idioma destino, sin necesitar un trigger de singleton. [hits],
 * [misses] y [levelUps] persisten el [com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome] de esa tanda
 * (design `redesign-app-ui`, "Contador de la tanda completada"); por defecto 0, como al componer una tanda nueva.
 */
@Entity(tableName = "active_batch")
data class ActiveBatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    val capacity: Int,
    val hits: Int = 0,
    val misses: Int = 0,
    val levelUps: Int = 0,
)
