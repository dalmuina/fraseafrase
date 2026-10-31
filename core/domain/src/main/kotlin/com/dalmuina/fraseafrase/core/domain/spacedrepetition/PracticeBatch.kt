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
            "La tanda no admite ids duplicados"
        }
        require(entries.size <= capacity.value) {
            "La tanda tiene ${entries.size} frases y su tamaño es ${capacity.value}"
        }
    }

    /**
     * Registra la autoevaluación de la frase [id] y la mueve al final de la tanda.
     * Responder una frase que no está en la tanda es un error de programación.
     */
    fun answer(id: PhraseId, assessment: SelfAssessment): PracticeBatch {
        require(entries.any { it.id == id }) { "La frase ${id.value} no está en la tanda" }
        val answered = entries.first { it.id == id }.record(assessment)
        return copy(entries = entries.filterNot { it.id == id } + answered)
    }

    /**
     * Número de frases no certificadas, que pasarían a la siguiente tanda al cerrar (aviso previo).
     * Es una función pura: no modifica la tanda, sus rachas ni el progreso. Una frase nunca respondida
     * también cuenta, porque no está certificada.
     */
    fun uncertifiedCount(): Int = entries.count { !it.isCertified }
}
