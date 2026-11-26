package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Relleno por tramos de la tanda nueva (ADR-0027): 1) falladas por volver; 2) repetidas hasta el mínimo;
 * 3) pendientes; 4) repaso adelantado; 5) resto de frases con progreso; 6) más nuevas; 7) frases en descanso.
 * Dentro de cada tramo se sigue la prioridad `nivel + (dueAtSession - sesión actual)` de menor a mayor, con
 * desempate por catálogo (ADR-0025, ADR-0026).
 *
 * [inLevel] es el progreso del nivel CEFR elegido.
 */
internal class BatchTranches(
    private val inLevel: List<PhraseProgress>,
    private val currentSession: Int,
    private val size: Int,
    catalogIndex: Map<PhraseId, Int>,
) {

    /** Frases con progreso de la tanda, en orden de tramo, y nuevas que añade el tramo 6. */
    data class Fill(val notNew: List<PhraseProgress>, val extraNew: Int)

    private val byPriority: List<PhraseProgress> = inLevel.sortedWith(
        compareBy<PhraseProgress> { it.level.value + (it.dueAtSession - currentSession) }
            .thenBy { catalogIndex.getValue(it.id) },
    )

    private val minRepeated = (size + MIN_REPEATED_DIVISOR - 1) / MIN_REPEATED_DIVISOR
    private val maxRepeated = size * MAX_REPEATED_NUMERATOR / MAX_REPEATED_DENOMINATOR
    private val newLimit = size / NEW_LIMIT_DIVISOR

    /** Nuevas de los tramos 1 a 4. */
    fun newCount(available: Int, firstBatchOfLevel: Boolean): Int {
        if (firstBatchOfLevel) return minOf(size, available)
        val learning = inLevel.count { it.level.value == 0 }
        return if (learning < size + newLimit) minOf(available, newLimit) else 0
    }

    /** Recorre los tramos 1 a 7 reservando [newCount] posiciones para las nuevas, de [available] candidatas. */
    fun fill(newCount: Int, available: Int): Fill {
        val taken = linkedSetOf<PhraseProgress>()
        val reviewLimit = size - newCount

        addMissed(taken, reviewLimit)
        addRepeatedUpToMinimum(taken, reviewLimit)
        addWithinRepeatedMaximum(taken, reviewLimit) { it.dueAtSession <= currentSession }
        addWithinRepeatedMaximum(taken, reviewLimit) { it.dueAtSession > currentSession }
        add(taken, reviewLimit) { !it.isResting() }
        val extraNew = minOf(available - newCount, size - taken.size - newCount)
        add(taken, size - newCount - extraNew) { true }

        return Fill(taken.toList(), extraNew)
    }

    private fun addMissed(taken: MutableSet<PhraseProgress>, limit: Int) =
        addWithinRepeatedMaximum(taken, limit) { it.isDueBackAfterMiss() }

    private fun addRepeatedUpToMinimum(taken: MutableSet<PhraseProgress>, limit: Int) =
        add(taken, limit) { !it.isResting() && it.isRepeated() && taken.repeatedCount() < minRepeated }

    /** Tramos que no meten en descanso ni una repetida más una vez alcanzado el máximo. */
    private fun addWithinRepeatedMaximum(
        taken: MutableSet<PhraseProgress>,
        limit: Int,
        belongs: (PhraseProgress) -> Boolean,
    ) = add(taken, limit) {
        belongs(it) && !it.isResting() && (!it.isRepeated() || taken.repeatedCount() < maxRepeated)
    }

    /** Añade en orden de prioridad las frases admitidas hasta que [taken] llega a [limit]. */
    private fun add(taken: MutableSet<PhraseProgress>, limit: Int, admits: (PhraseProgress) -> Boolean) {
        for (phrase in byPriority) {
            if (taken.size >= limit) return
            if (phrase !in taken && admits(phrase)) taken.add(phrase)
        }
    }

    private fun Set<PhraseProgress>.repeatedCount(): Int = count { it.isRepeated() }

    private fun PhraseProgress.isRepeated(): Boolean = answerHistory?.lastAnsweredSession == currentSession - 1

    private fun PhraseProgress.isResting(): Boolean {
        val history = answerHistory ?: return false
        return isRepeated() && history.consecutiveSessions >= MAX_CONSECUTIVE_SESSIONS
    }

    /** Fallada por volver: su último resultado fue un fallo, hace como mucho dos sesiones. */
    private fun PhraseProgress.isDueBackAfterMiss(): Boolean {
        val history = answerHistory ?: return false
        return history.lastMissSession == history.lastAnsweredSession &&
            history.lastAnsweredSession >= currentSession - MISS_RETURN_WINDOW
    }

    companion object {
        /** Sesiones seguidas a partir de las cuales una frase repetida descansa. */
        private const val MAX_CONSECUTIVE_SESSIONS = 6

        /** Sesiones hacia atrás en las que un fallo da preferencia. */
        private const val MISS_RETURN_WINDOW = 2

        /** Las nuevas tras la primera tanda son como mucho N/5. */
        private const val NEW_LIMIT_DIVISOR = 5

        /** El mínimo de repetidas es N/4 redondeado hacia arriba. */
        private const val MIN_REPEATED_DIVISOR = 4

        /** Numerador de la fracción de N que limita las repetidas (2/5). */
        private const val MAX_REPEATED_NUMERATOR = 2

        /** Denominador de la fracción de [MAX_REPEATED_NUMERATOR]. */
        private const val MAX_REPEATED_DENOMINATOR = 5
    }
}
