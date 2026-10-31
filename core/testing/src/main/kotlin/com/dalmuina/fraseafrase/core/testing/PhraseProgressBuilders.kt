package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import java.time.LocalDate

/**
 * Crea un [PhraseProgress] de prueba. Los valores por defecto cumplen las invariantes.
 * Las combinaciones incoherentes no se corrigen aquí: fallan en el `require` de [PhraseProgress],
 * que es lo que se quiere comprobar en los tests.
 */
fun aPhraseProgress(
    id: String = "phrase-1",
    level: Int = 0,
    streak: Int = 0,
    answeredInBatch: Boolean = false,
    nextReview: LocalDate? = null,
): PhraseProgress = PhraseProgress(
    id = PhraseId(id),
    level = Level(level),
    streak = Streak(streak),
    answeredInBatch = answeredInBatch,
    nextReview = nextReview,
)
