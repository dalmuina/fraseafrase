package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level.Companion.MAX_LEVEL
import com.dalmuina.fraseafrase.core.domain.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.min

/**
 * Resuelve cada frase de la tanda al cerrarla (ADR-0012). Preserva el orden de entrada.
 */
class ResolveBatchClosure @Inject constructor(private val clock: Clock) {

    operator fun invoke(batch: List<PhraseProgress>): List<BatchClosureOutcome> {
        val closingDate = clock.today()
        return batch.map { progress ->
            if (progress.isCertified) {
                BatchClosureOutcome.Certified(certifiedAt(progress, closingDate))
            } else {
                BatchClosureOutcome.Carried(carried(progress))
            }
        }
    }

    private fun certifiedAt(progress: PhraseProgress, closingDate: LocalDate): PhraseProgress {
        val level = Level(min(progress.level.value + 1, MAX_LEVEL))
        return progress.copy(
            level = level,
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = ReviewSchedule.nextReviewAfter(level, closingDate),
        )
    }

    private fun carried(progress: PhraseProgress): PhraseProgress {
        val leveled = if (progress.answeredInBatch) {
            progress.copy(level = Level(0), nextReview = null)
        } else {
            progress
        }
        return leveled.copy(streak = Streak(0), answeredInBatch = false)
    }
}
