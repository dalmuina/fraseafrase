package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Tanda activa: una sola fila con id 1 (ADR-0013, design D5). Su existencia indica que hay tanda activa. */
@Entity(tableName = "active_batch")
data class ActiveBatchEntity(
    @PrimaryKey val id: Int,
    val capacity: Int,
)
