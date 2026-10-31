package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Fila de progreso de una frase (ADR-0013, design D5). `batch_position` y `carry_position` indican dónde está la
 * frase: en la tanda activa, en la cola de arrastradas, o en ninguna (programada). Los UNIQUE de cada posición son
 * índices de Room; el resto de invariantes se aplican con triggers en [PracticeSchema].
 */
@Entity(
    tableName = "phrase_progress",
    indices = [
        Index(value = ["batch_position"], unique = true),
        Index(value = ["carry_position"], unique = true),
    ],
)
data class PhraseProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "phrase_id")
    val phraseId: String,
    val level: Int,
    val streak: Int,
    @ColumnInfo(name = "answered_in_batch")
    val answeredInBatch: Boolean,
    @ColumnInfo(name = "next_review")
    val nextReview: String?,
    @ColumnInfo(name = "batch_position")
    val batchPosition: Int?,
    @ColumnInfo(name = "carry_position")
    val carryPosition: Int?,
)
