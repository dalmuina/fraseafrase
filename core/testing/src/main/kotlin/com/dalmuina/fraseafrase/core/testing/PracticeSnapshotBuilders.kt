package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.BatchOutcome
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress

/**
 * Crea un [PracticeSnapshot] de prueba, sin tanda activa por defecto. Conveniente para tests que necesitan un
 * [BatchOutcome] concreto sin construirlo respondiendo frases con `AnswerPhrase` (p. ej. presentación, spec
 * `practice-session`).
 */
fun aPracticeSnapshot(
    progress: List<PhraseProgress> = emptyList(),
    activeBatch: ActiveBatch? = null,
    sessionCount: Int = 0,
    batchOutcome: BatchOutcome = BatchOutcome(),
): PracticeSnapshot = PracticeSnapshot(
    progress = progress,
    activeBatch = activeBatch,
    sessionCount = sessionCount,
    batchOutcome = batchOutcome,
)
