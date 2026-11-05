package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.time.Clock
import javax.inject.Inject

/**
 * Registra la autoevaluación de una frase dentro de la tanda activa: actualiza la racha según el
 * resultado y marca la frase como respondida en la tanda (ADR-0012).
 */
class RecordSelfAssessment @Inject constructor(private val clock: Clock) {

    operator fun invoke(progress: PhraseProgress, assessment: SelfAssessment): PhraseProgress =
        progress.record(assessment, clock.today())
}
