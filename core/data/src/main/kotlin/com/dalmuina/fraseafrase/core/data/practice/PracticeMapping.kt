package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.AnswerHistory
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/** Mapeos entre las filas de Room y los tipos del dominio (ADR-0010: los mapeos viven en :core:data). */
internal fun PhraseProgressEntity.toDomain(): PhraseProgress = PhraseProgress(
    id = PhraseId(phraseId),
    level = Level(level),
    streak = Streak(streak),
    answeredInBatch = answeredInBatch,
    dueAtSession = dueAtSession,
    answerHistory = answerHistory(),
)

/** Sin sesión respondida no hay registro; una fila que aun así cuente sesiones o fallos está corrupta. */
private fun PhraseProgressEntity.answerHistory(): AnswerHistory? {
    if (lastAnsweredSession == null) {
        require(consecutiveSessions == 0 && lastMissSession == null) {
            "Phrase $phraseId has an answer history without a last answered session"
        }
        return null
    }
    return AnswerHistory(
        lastAnsweredSession = lastAnsweredSession,
        consecutiveSessions = consecutiveSessions,
        lastMissSession = lastMissSession,
    )
}

internal fun PhraseProgress.toEntity(targetLocale: String, batchPosition: Int?): PhraseProgressEntity =
    PhraseProgressEntity(
        phraseId = id.value,
        targetLocale = targetLocale,
        level = level.value,
        streak = streak.value,
        answeredInBatch = answeredInBatch,
        dueAtSession = dueAtSession,
        batchPosition = batchPosition,
        lastAnsweredSession = answerHistory?.lastAnsweredSession,
        consecutiveSessions = answerHistory?.consecutiveSessions ?: 0,
        lastMissSession = answerHistory?.lastMissSession,
    )

internal fun ActiveBatchEntity.capacity(): BatchSize = BatchSize(capacity)

internal fun ActiveBatchEntity.toBatchOutcome(): BatchOutcome =
    BatchOutcome(hits = hits, misses = misses, levelUps = levelUps)
