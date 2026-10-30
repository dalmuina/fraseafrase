package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import javax.inject.Inject

/**
 * Registra la autoevaluación de una frase dentro de la tanda activa: actualiza la racha según el
 * resultado y marca la frase como respondida en la tanda (ADR-0012). No depende del reloj.
 */
class RecordSelfAssessment @Inject constructor() {

    operator fun invoke(progress: PhraseProgress, assessment: SelfAssessment): PhraseProgress {
        val streak = when (assessment) {
            SelfAssessment.HIT -> progress.streak.afterHit()
            SelfAssessment.MISS -> progress.streak.afterMiss()
        }
        return progress.copy(streak = streak, answeredInBatch = true)
    }
}
