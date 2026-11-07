package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Fila de progreso de una frase (ADR-0013, ADR-0017, design D5/D7). `batch_position` indica si la frase está en la
 * tanda activa; una frase fuera de la tanda activa duerme en el progreso hasta su [dueAtSession]. El UNIQUE de la
 * posición es un índice de Room; el resto de invariantes se aplican con triggers en [PracticeSchema].
 */
@Entity(
    tableName = "phrase_progress",
    indices = [
        Index(value = ["batch_position"], unique = true),
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
    @ColumnInfo(name = "due_at_session")
    val dueAtSession: Int,
    @ColumnInfo(name = "batch_position")
    val batchPosition: Int?,
)
