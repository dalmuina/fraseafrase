package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import java.time.LocalDate

/** Mapeos entre las filas de Room y los tipos del dominio (ADR-0010: los mapeos viven en :core:data). */
internal fun PhraseProgressEntity.toDomain(): PhraseProgress = PhraseProgress(
    id = PhraseId(phraseId),
    level = Level(level),
    streak = Streak(streak),
    answeredInBatch = answeredInBatch,
    nextReview = nextReview?.let(LocalDate::parse),
)

internal fun PhraseProgress.toEntity(batchPosition: Int?, carryPosition: Int?): PhraseProgressEntity =
    PhraseProgressEntity(
        phraseId = id.value,
        level = level.value,
        streak = streak.value,
        answeredInBatch = answeredInBatch,
        nextReview = nextReview?.toString(),
        batchPosition = batchPosition,
        carryPosition = carryPosition,
    )

internal fun ActiveBatchEntity.capacity(): BatchSize = BatchSize(capacity)
