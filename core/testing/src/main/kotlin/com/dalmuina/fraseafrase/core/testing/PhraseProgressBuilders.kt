package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/**
 * Crea un [PhraseProgress] de prueba. Los valores por defecto cumplen las invariantes.
 * Las combinaciones incoherentes no se corrigen aquí: fallan en el `require` de [PhraseProgress],
 * que es lo que se quiere comprobar en los tests.
 */
fun aPhraseProgress(
    id: String = "phrase-1",
    level: Level = Level(0),
    streak: Streak = Streak(0),
    answeredInBatch: Boolean = false,
    dueAtSession: Int = 0,
): PhraseProgress = PhraseProgress(
    id = PhraseId(id),
    level = level,
    streak = streak,
    answeredInBatch = answeredInBatch,
    dueAtSession = dueAtSession,
)
