package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Frase dentro de la tanda activa (ADR-0012, ADR-0017). Inmutable y con invariantes validadas al
 * construirla: el nivel y la racha son no negativos (impuesto por sus propios tipos) y [dueAtSession]
 * (la sesión generada a partir de la cual vuelve a estar disponible) no puede ser negativo.
 *
 * Un estado incoherente es un error de programación, por eso se lanza [IllegalArgumentException].
 */
data class PhraseProgress(
    val id: PhraseId,
    val level: Level,
    val streak: Streak,
    val answeredInBatch: Boolean,
    val dueAtSession: Int,
) {

    init {
        require(dueAtSession >= 0) { "dueAtSession must not be negative, received $dueAtSession" }
    }

    /** Certificada si su racha alcanza [CERTIFICATION_STREAK]. */
    val isCertified: Boolean
        get() = streak.value >= CERTIFICATION_STREAK

    /**
     * Pendiente en la sesión [currentSession] si [dueAtSession] es igual o anterior a ella, sin
     * excepción de nivel: una frase de nivel 0 recién fallada también puede estar pendiente.
     * Es una función pura: la sesión llega como dato, nunca de un contador externo.
     */
    fun isPendingAtSession(currentSession: Int): Boolean = dueAtSession <= currentSession

    /**
     * Registra la autoevaluación de esta frase en la sesión generada [currentSession] y resuelve al
     * instante (ADR-0017): un acierto que lleva la racha a [CERTIFICATION_STREAK] certifica la frase
     * (sube de nivel, racha a 0, enfriamiento del nuevo nivel); un acierto que no certifica solo fija
     * la sesión siguiente; un fallo, en cualquier nivel, baja el nivel en 1 si era mayor que 0, reinicia
     * la racha y fija la sesión siguiente. Es una función pura: la sesión llega como dato.
     */
    fun record(assessment: SelfAssessment, currentSession: Int): PhraseProgress = when (assessment) {
        SelfAssessment.HIT -> recordHit(currentSession)
        SelfAssessment.MISS -> recordMiss(currentSession)
    }

    private fun recordHit(currentSession: Int): PhraseProgress {
        val updatedStreak = streak.afterHit()
        if (updatedStreak.value < CERTIFICATION_STREAK) {
            return copy(streak = updatedStreak, answeredInBatch = true, dueAtSession = currentSession + 1)
        }
        val updatedLevel = Level(minOf(level.value + 1, Level.MAX_LEVEL))
        return copy(
            level = updatedLevel,
            streak = Streak(0),
            answeredInBatch = true,
            dueAtSession = currentSession + ReviewSchedule.cooldownSessionsFor(updatedLevel),
        )
    }

    private fun recordMiss(currentSession: Int): PhraseProgress {
        val updatedLevel = if (level.value >= 1) Level(level.value - 1) else level
        return copy(
            level = updatedLevel,
            streak = streak.afterMiss(),
            answeredInBatch = true,
            dueAtSession = currentSession + 1,
        )
    }

    companion object {
        /** Racha mínima para certificar una frase (ADR-0012). */
        const val CERTIFICATION_STREAK = 3
    }
}
