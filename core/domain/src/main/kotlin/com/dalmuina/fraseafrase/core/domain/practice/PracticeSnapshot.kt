package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PracticeBatch

/**
 * Instantánea del estado del usuario (ADR-0012, ADR-0017). [progress] tiene una entrada por cada frase con
 * progreso o con posición en la tanda activa. [activeBatch] indica si hay tanda activa y en qué orden;
 * [sessionCount] es el contador de sesiones generadas. [batchOutcome] es el desglose de aciertos, fallos y
 * subidas de nivel de la tanda activa (spec `practice-session`); se reinicia a cero al componer la tanda
 * siguiente.
 */
data class PracticeSnapshot(
    val progress: List<PhraseProgress>,
    val activeBatch: ActiveBatch?,
    val sessionCount: Int,
    val batchOutcome: BatchOutcome = BatchOutcome(),
) {

    /** La tanda activa con sus entradas en el orden guardado, o null si no hay tanda activa. */
    fun batch(): PracticeBatch? = activeBatch?.let { active ->
        val byId = progress.associateBy { it.id }
        PracticeBatch(entries = active.order.map { byId.getValue(it) }, capacity = active.capacity)
    }

    /**
     * Instantánea con [batch] como tanda activa: su orden de entradas y su capacidad, y el progreso de cada entrada
     * actualizado. Las frases que no están en la tanda conservan su progreso.
     */
    fun storing(batch: PracticeBatch): PracticeSnapshot {
        val updated = batch.entries.associateBy { it.id }
        return copy(
            progress = progress.map {
                updated[it.id] ?: it
            } + batch.entries.filter { entry -> progress.none { it.id == entry.id } },
            activeBatch = ActiveBatch(order = batch.entries.map { it.id }, capacity = batch.capacity),
        )
    }
}

/** Tanda activa como orden de ids y capacidad. Los datos de cada frase viven en el progreso (sin duplicarlos). */
data class ActiveBatch(
    val order: List<PhraseId>,
    val capacity: BatchSize,
)

/** Resultado de una transición: la instantánea siguiente y el valor que devuelve el caso de uso. */
data class PracticeChange<T>(val next: PracticeSnapshot, val result: T)
