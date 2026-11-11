package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment

/**
 * Desglose de la tanda activa (ADR-0012, spec `practice-session`): número de autoevaluaciones con acierto, con
 * fallo y de frases cuyo nivel de consolidación subió en algún momento de la tanda. [levelUps] cuenta subidas
 * ocurridas, no el nivel final: una frase que sube y vuelve a bajar de nivel en la misma tanda sigue contando
 * como subida. Empieza en (0, 0, 0) al componer una tanda nueva.
 */
data class BatchOutcome(
    val hits: Int = 0,
    val misses: Int = 0,
    val levelUps: Int = 0,
) {

    /**
     * Registra una autoevaluación: suma 1 a [hits] o a [misses] según [assessment]; si fue un acierto y
     * [leveledUp] es `true`, suma también 1 a [levelUps].
     */
    fun record(assessment: SelfAssessment, leveledUp: Boolean): BatchOutcome = when (assessment) {
        SelfAssessment.HIT -> copy(hits = hits + 1, levelUps = levelUps + leveledUp.toIncrement())
        SelfAssessment.MISS -> copy(misses = misses + 1)
    }

    /** Deshace exactamente el último [record] aplicado con el mismo [assessment] y [leveledUp]. */
    fun revert(assessment: SelfAssessment, leveledUp: Boolean): BatchOutcome = when (assessment) {
        SelfAssessment.HIT -> copy(hits = hits - 1, levelUps = levelUps - leveledUp.toIncrement())
        SelfAssessment.MISS -> copy(misses = misses - 1)
    }

    private fun Boolean.toIncrement(): Int = if (this) 1 else 0
}
