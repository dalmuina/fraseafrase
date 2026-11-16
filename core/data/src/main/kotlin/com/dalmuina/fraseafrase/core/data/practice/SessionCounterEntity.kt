package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Contador de sesiones generadas, independiente por idioma destino (ADR-0013, ADR-0017, ADR-0019, design D7/
 * "PracticeDatabase v4"). La PK es `target_locale`; su ausencia para un idioma destino equivale a 0.
 */
@Entity(tableName = "session_counter")
data class SessionCounterEntity(
    @PrimaryKey
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    @ColumnInfo(name = "session_count")
    val sessionCount: Int,
)
