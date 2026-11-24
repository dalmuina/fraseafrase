package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.NothingAvailable
import javax.inject.Inject

/**
 * Compone la tanda siguiente (ADR-0012, ADR-0025): pendientes por prioridad y nuevas según el
 * aprendizaje, sin bloques fijos. Función pura: la sesión actual llega como argumento,
 * no de un reloj. Las precondiciones de entrada son errores de programación y lanzan
 * [IllegalArgumentException].
 */
class ComposeNextBatch @Inject constructor() {

    operator fun invoke(
        progress: List<PhraseProgress>,
        catalog: List<CatalogPhrase>,
        cefrLevel: CefrLevel,
        size: BatchSize,
        currentSession: Int,
    ): BatchComposition {
        requireConsistentInputs(progress, catalog)
        val catalogById = catalog.associateBy { it.id }
        val progressIds = progress.map { it.id }.toSet()
        val catalogIndex = catalog.withIndex().associate { it.value.id to it.index }

        val inLevel = progress.filter { catalogById.getValue(it.id).level == cefrLevel }
        val pending = inLevel
            .filter { it.dueAtSession <= currentSession }
            .sortedWith(
                compareBy<PhraseProgress> { it.level.value - (currentSession - it.dueAtSession) }
                    .thenBy { catalogIndex.getValue(it.id) },
            )
        val learningCount = inLevel.count { it.level.value == 0 }
        val newCandidates = catalog.filter { it.level == cefrLevel && it.id !in progressIds }

        val newCount = newCountOf(size.value, pending.size, learningCount, newCandidates.size)
        val taken = pending.take(size.value - newCount).map { it.restartedInBatch() }
        val new = newCandidates
            .take(newCount)
            .map { PhraseProgress(it.id, Level(0), Streak(0), answeredInBatch = false, dueAtSession = 0) }

        val entries = interleave(taken, new)
        if (entries.isEmpty()) return NothingAvailable
        return Composed(PracticeBatch(entries, size))
    }

    /** Número de nuevas según el aprendizaje (design D2.3); aritmética entera para evitar errores de Double. */
    private fun newCountOf(size: Int, pending: Int, learning: Int, available: Int): Int {
        val cap = size * LEARNING_CAP_NUMERATOR / LEARNING_CAP_DENOMINATOR
        val max40 = size * PENDING_CAP_NUMERATOR / PENDING_CAP_DENOMINATOR
        val ceiling = if (pending == 0) cap else max40
        val gap = maxOf(0, cap - learning)
        return minOf(available, ceiling, maxOf(gap, size - pending))
    }

    /**
     * Reparto de las nuevas (design D2.4): con `p` pendientes y `k` nuevas, la nueva `i` va en la posición
     * `(i + 1) * (p + k) / (k + 1)`. Sin pendientes, las nuevas van en orden de catálogo.
     */
    private fun interleave(pending: List<PhraseProgress>, new: List<PhraseProgress>): List<PhraseProgress> {
        if (pending.isEmpty()) return new
        val p = pending.size
        val k = new.size
        val newPositions = new.indices.associate { i -> (i + 1) * (p + k) / (k + 1) to new[i] }
        val pendingIterator = pending.iterator()
        return (0 until p + k).map { position -> newPositions[position] ?: pendingIterator.next() }
    }

    private fun requireConsistentInputs(progress: List<PhraseProgress>, catalog: List<CatalogPhrase>) {
        val progressIds = progress.map { it.id }.toSet()
        val catalogIds = catalog.map { it.id }.toSet()
        require(progressIds.size == progress.size) { "Progress does not allow duplicate ids" }
        require(catalogIds.size == catalog.size) { "The catalog does not allow duplicate ids" }
        require(progressIds.all { it in catalogIds }) { "Some progress belongs to phrases missing from the catalog" }
    }

    /**
     * `answeredInBatch` se reinicia a falso al entrar en una tanda recién compuesta (design D0); la racha
     * persiste entre tandas (ADR-0017) y no se toca aquí.
     */
    private fun PhraseProgress.restartedInBatch(): PhraseProgress = copy(answeredInBatch = false)

    companion object {
        /** Numerador de la fracción de N que limita nuevas sin pendientes y el grupo de nivel 0 (1/2). */
        private const val LEARNING_CAP_NUMERATOR = 1

        /** Denominador de la fracción de [LEARNING_CAP_NUMERATOR]. */
        private const val LEARNING_CAP_DENOMINATOR = 2

        /** Numerador de la fracción de N que limita las nuevas cuando hay pendientes (2/5). */
        private const val PENDING_CAP_NUMERATOR = 2

        /** Denominador de la fracción de [PENDING_CAP_NUMERATOR]. */
        private const val PENDING_CAP_DENOMINATOR = 5
    }
}
