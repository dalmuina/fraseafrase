package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Contador de sesiones generadas: una sola fila con id 1 (ADR-0013, ADR-0017, design D7). Su ausencia equivale a 0. */
@Entity(tableName = "session_counter")
data class SessionCounterEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "session_count")
    val sessionCount: Int,
)
