package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome
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
)

internal fun PhraseProgress.toEntity(batchPosition: Int?): PhraseProgressEntity =
    PhraseProgressEntity(
        phraseId = id.value,
        level = level.value,
        streak = streak.value,
        answeredInBatch = answeredInBatch,
        dueAtSession = dueAtSession,
        batchPosition = batchPosition,
    )

internal fun ActiveBatchEntity.capacity(): BatchSize = BatchSize(capacity)

internal fun ActiveBatchEntity.toBatchOutcome(): BatchOutcome =
    BatchOutcome(hits = hits, misses = misses, levelUps = levelUps)
