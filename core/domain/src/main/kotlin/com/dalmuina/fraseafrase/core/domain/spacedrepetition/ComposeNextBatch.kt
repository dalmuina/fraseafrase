package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.NothingAvailable
import javax.inject.Inject

/**
 * Compone la tanda siguiente (ADR-0012, ADR-0027), hasta N frases: las frases con progreso entran por tramos
 * ([BatchTranches]) y las nuevas, limitadas tras la primera tanda del nivel, se reparten entre ellas. Solo
 * sale más pequeña si el nivel CEFR no tiene más frases. Función pura: la sesión actual llega como argumento,
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
        val newCandidates = catalog.filter { it.level == cefrLevel && it.id !in progressIds }

        val tranches = BatchTranches(inLevel, currentSession, size.value, catalogIndex)
        val newCount = tranches.newCount(newCandidates.size, firstBatchOfLevel = inLevel.isEmpty())
        val fill = tranches.fill(newCount, newCandidates.size)
        val new = newCandidates
            .take(newCount + fill.extraNew)
            .map { PhraseProgress(it.id, Level(0), Streak(0), answeredInBatch = false, dueAtSession = 0) }

        val entries = interleave(fill.notNew.map { it.restartedInBatch() }, new)
        if (entries.isEmpty()) return NothingAvailable
        return Composed(PracticeBatch(entries, size))
    }

    /**
     * Reparto de las nuevas (ADR-0025): con `p` no nuevas (en el orden de sus tramos) y `k` nuevas, la nueva `i`
     * va en la posición `(i + 1) * (p + k) / (k + 1)`. Sin no nuevas, van en orden de catálogo.
     */
    private fun interleave(notNew: List<PhraseProgress>, new: List<PhraseProgress>): List<PhraseProgress> {
        if (notNew.isEmpty()) return new
        val p = notNew.size
        val k = new.size
        val newPositions = new.indices.associate { i -> (i + 1) * (p + k) / (k + 1) to new[i] }
        val notNewIterator = notNew.iterator()
        return (0 until p + k).map { position -> newPositions[position] ?: notNewIterator.next() }
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
}
