package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import java.time.LocalDate

/**
 * Frase dentro de la tanda activa (ADR-0012). Inmutable y con invariantes validadas al construirla:
 * - un nivel 0 no tiene próximo repaso, y un nivel de 1 a 5 sí lo tiene;
 * - una racha mayor que 0 exige que la frase haya sido respondida en la tanda.
 *
 * Un estado incoherente es un error de programación, por eso se lanza [IllegalArgumentException].
 */
data class PhraseProgress(
    val id: PhraseId,
    val level: Level,
    val streak: Streak,
    val answeredInBatch: Boolean,
    val nextReview: LocalDate?,
) {

    init {
        require(level.value > 0 || nextReview == null) {
            "A level 0 phrase cannot have a next review"
        }
        require(level.value == 0 || nextReview != null) {
            "A level ${level.value} phrase must have a next review"
        }
        require(streak.value == 0 || answeredInBatch) {
            "A streak greater than 0 requires the phrase to have been answered in the batch"
        }
    }

    /** Certificada si su racha alcanza [CERTIFICATION_STREAK] al cerrar la tanda. */
    val isCertified: Boolean
        get() = streak.value >= CERTIFICATION_STREAK

    /**
     * Pendiente en la fecha [today] si su próximo repaso es igual o anterior a [today].
     * Una frase de nivel 0 nunca está pendiente: la invariante nivel 0 ⇔ sin próximo repaso lo garantiza.
     * Es una función pura: la fecha llega como dato, nunca del reloj.
     */
    fun isPendingOn(today: LocalDate): Boolean = nextReview?.let { it <= today } ?: false

    /**
     * Registra la autoevaluación de esta frase dentro de la tanda: un acierto suma 1 a la racha y un
     * fallo la reinicia; en ambos casos la frase queda respondida en la tanda (ADR-0012).
     * Un fallo en una frase de nivel 1 o superior también baja su nivel en 1 y recalcula su próximo
     * repaso a partir de [today], sin esperar al cierre de la tanda (ADR-0015).
     * Es una función pura: la fecha llega como dato, nunca del reloj.
     */
    fun record(assessment: SelfAssessment, today: LocalDate): PhraseProgress {
        val updatedStreak = when (assessment) {
            SelfAssessment.HIT -> streak.afterHit()
            SelfAssessment.MISS -> streak.afterMiss()
        }
        val updatedLevel = if (assessment == SelfAssessment.MISS && level.value >= 1) {
            Level(level.value - 1)
        } else {
            level
        }
        val updatedNextReview = if (updatedLevel == level) {
            nextReview
        } else {
            ReviewSchedule.nextReviewAfter(updatedLevel, today)
        }
        return copy(
            level = updatedLevel,
            streak = updatedStreak,
            answeredInBatch = true,
            nextReview = updatedNextReview,
        )
    }

    companion object {
        /** Racha mínima dentro de la tanda para certificar una frase (ADR-0012). */
        const val CERTIFICATION_STREAK = 3
    }
}
