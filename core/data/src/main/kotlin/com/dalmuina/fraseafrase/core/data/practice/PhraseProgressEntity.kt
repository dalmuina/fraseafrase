package com.dalmuina.fraseafrase.core.data.practice

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

/**
 * Fila de progreso de una frase, independiente por idioma destino (ADR-0013, ADR-0017, ADR-0019, design
 * D5/D7/"PracticeDatabase v4"). `batch_position` indica si la frase está en la tanda activa; una frase fuera de la
 * tanda activa duerme en el progreso hasta su [dueAtSession]. La PK es `(phrase_id, target_locale)`: la misma frase
 * tiene progreso distinto en cada idioma destino. El UNIQUE de la posición es un índice de Room sobre
 * `(target_locale, batch_position)`: la misma posición es válida en idiomas destino distintos; el resto de
 * invariantes se aplican con triggers en [PracticeSchema].
 */
@Entity(
    tableName = "phrase_progress",
    primaryKeys = ["phrase_id", "target_locale"],
    indices = [
        Index(value = ["target_locale", "batch_position"], unique = true),
    ],
)
data class PhraseProgressEntity(
    @ColumnInfo(name = "phrase_id")
    val phraseId: String,
    @ColumnInfo(name = "target_locale")
    val targetLocale: String,
    val level: Int,
    val streak: Int,
    @ColumnInfo(name = "answered_in_batch")
    val answeredInBatch: Boolean,
    @ColumnInfo(name = "due_at_session")
    val dueAtSession: Int,
    @ColumnInfo(name = "batch_position")
    val batchPosition: Int?,
)
