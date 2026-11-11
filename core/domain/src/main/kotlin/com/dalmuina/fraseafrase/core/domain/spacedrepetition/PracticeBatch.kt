package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Tanda activa (ADR-0012): lista circular de frases. Responder una frase la mueve al final de la lista.
 * [capacity] es el tamaño N con el que se creó la tanda y no cambia aunque después cambie el ajuste.
 *
 * Invariantes: cada frase aparece una sola vez y la tanda no supera [capacity]. Su violación es un error
 * de programación y lanza [IllegalArgumentException].
 */
data class PracticeBatch(
    val entries: List<PhraseProgress>,
    val capacity: BatchSize,
) {

    init {
        require(entries.map { it.id }.distinct().size == entries.size) {
            "The batch does not allow duplicate ids"
        }
        require(entries.size <= capacity.value) {
            "The batch has ${entries.size} phrases but its size is ${capacity.value}"
        }
    }

    /**
     * Registra la autoevaluación de la frase [id] en la sesión generada [currentSession] y la mueve al final
     * de la tanda. Responder una frase que no está en la tanda es un error de programación.
     */
    fun answer(id: PhraseId, assessment: SelfAssessment, currentSession: Int): PracticeBatch {
        require(entries.any { it.id == id }) { "Phrase ${id.value} is not in the batch" }
        val answered = entries.first { it.id == id }.record(assessment, currentSession)
        return copy(entries = entries.filterNot { it.id == id } + answered)
    }

    /**
     * Revierte la autoevaluación más reciente de toda la tanda, que siempre deja a la frase respondida en la última
     * posición ([answer]). Exige que la última entrada sea [expectedId]: revertir cualquier otra es un error de
     * programación, porque ya no es la autoevaluación más reciente. Devuelve [progressBefore] a la posición que
     * ocupaba antes, delante del resto de entradas, sin alterar su orden relativo.
     */
    fun revertLastAnswer(expectedId: PhraseId, progressBefore: PhraseProgress): PracticeBatch {
        require(entries.last().id == expectedId) {
            "Phrase ${expectedId.value} is not the most recent answer in the batch"
        }
        return copy(entries = listOf(progressBefore) + entries.dropLast(1))
    }
}
