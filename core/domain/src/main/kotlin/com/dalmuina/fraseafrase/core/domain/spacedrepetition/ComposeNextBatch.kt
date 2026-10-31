package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.NothingAvailable
import com.dalmuina.fraseafrase.core.domain.time.Clock
import javax.inject.Inject

/**
 * Compone la tanda siguiente (ADR-0012). Función pura: el tiempo llega por [Clock] y el resto por argumentos.
 * Las precondiciones de entrada son errores de programación y lanzan [IllegalArgumentException].
 */
class ComposeNextBatch @Inject constructor(private val clock: Clock) {

    operator fun invoke(
        carry: CarryQueue,
        progress: List<PhraseProgress>,
        catalog: List<CatalogPhrase>,
        cefrLevel: CefrLevel,
        size: BatchSize,
    ): BatchComposition {
        requireConsistentInputs(carry, progress, catalog)
        val today = clock.today()
        val byId = progress.associateBy { it.id }
        val catalogIndex = catalog.withIndex().associate { it.value.id to it.index }

        val fromQueue = carry.ids.take(size.value).map { byId.getValue(it) }
        val leftover = carry.ids.drop(size.value)
        val queued = carry.ids.toSet()
        val pending = progress
            .filter { it.isPendingOn(today) && it.id !in queued }
            .sortedWith(compareBy({ it.level.value }, { it.nextReview }, { catalogIndex[it.id] }))
            .take(size.value - fromQueue.size)
        val freeSlots = size.value - fromQueue.size - pending.size
        val new = catalog
            .filter { it.level == cefrLevel && it.id !in byId }
            .take(freeSlots)
            .map { PhraseProgress(it.id, Level(0), Streak(0), answeredInBatch = false, nextReview = null) }

        val entries = (fromQueue + pending).map { it.restartedInBatch() } + new
        if (entries.isEmpty()) return NothingAvailable
        return Composed(PracticeBatch(entries, size), CarryQueue(leftover))
    }

    private fun requireConsistentInputs(
        carry: CarryQueue,
        progress: List<PhraseProgress>,
        catalog: List<CatalogPhrase>,
    ) {
        val progressIds = progress.map { it.id }.toSet()
        val catalogIds = catalog.map { it.id }.toSet()
        require(progressIds.size == progress.size) { "El progreso no admite ids duplicados" }
        require(catalogIds.size == catalog.size) { "El catálogo no admite ids duplicados" }
        require(progressIds.all { it in catalogIds }) { "Hay progreso de frases que no están en el catálogo" }
        require(carry.ids.all { it in progressIds }) { "Hay frases arrastradas sin progreso" }
    }

    /** Racha inicial de cada tanda nueva: 0 y sin respuesta en la tanda (ADR-0012). Guarda de robustez. */
    private fun PhraseProgress.restartedInBatch(): PhraseProgress =
        copy(streak = Streak(0), answeredInBatch = false)
}
