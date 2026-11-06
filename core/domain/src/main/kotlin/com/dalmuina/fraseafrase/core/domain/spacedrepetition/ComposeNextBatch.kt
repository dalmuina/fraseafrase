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
        val queued = carry.ids.toSet()

        val carried = carry.ids.map { byId.getValue(it) }
        val pending = progress
            .filter { it.isPendingOn(today) && it.id !in queued }
            .sortedWith(compareBy({ it.level.value }, { it.nextReview }, { catalogIndex[it.id] }))
        val reinforceCandidates = carried + pending
        val newCandidates = catalog.filter { it.level == cefrLevel && it.id !in byId }
        val masteredCandidates = progress.filter {
            it.level.value >= MASTERED_LEVEL && it.id !in queued && !it.isPendingOn(today)
        }

        val take = quotasOf(size).distribute(
            reinforceCandidates.size,
            newCandidates.size,
            masteredCandidates.size,
        )

        val fromQueue = reinforceCandidates.take(take.reinforce)
        val leftover = carry.ids.drop(take.reinforce)
        val mastered = masteredCandidates.take(take.mastered)
        val new = newCandidates
            .take(take.new)
            .map { PhraseProgress(it.id, Level(0), Streak(0), answeredInBatch = false, nextReview = null) }

        val entries = (fromQueue + mastered).map { it.restartedInBatch() } + new
        if (entries.isEmpty()) return NothingAvailable
        return Composed(PracticeBatch(entries, size), CarryQueue(leftover))
    }

    /** Cuotas por bloque sobre el tamaño N de la tanda (ADR-0012, design D1). */
    private fun quotasOf(size: BatchSize): Quotas {
        val reinforce = (size.value * QUOTA_RATIO).toInt()
        val new = (size.value * QUOTA_RATIO).toInt()
        val mastered = size.value - reinforce - new
        return Quotas(new = new, reinforce = reinforce, mastered = mastered)
    }

    /**
     * Reparto de hueco entre bloques cuando alguno no alcanza su cuota (design D2): primero se toma hasta la
     * cuota de cada bloque con sus propios candidatos; el hueco restante se reparte en el orden a reforzar,
     * nuevas, dominadas, entre los bloques con candidatos sobrantes.
     */
    private data class Quotas(val new: Int, val reinforce: Int, val mastered: Int) {

        fun distribute(reinforceAvailable: Int, newAvailable: Int, masteredAvailable: Int): Take {
            val reinforceBase = minOf(reinforce, reinforceAvailable)
            val newBase = minOf(new, newAvailable)
            val masteredBase = minOf(mastered, masteredAvailable)
            var hole = (reinforce + new + mastered) - (reinforceBase + newBase + masteredBase)

            val reinforceExtra = minOf(hole, reinforceAvailable - reinforceBase)
            hole -= reinforceExtra
            val newExtra = minOf(hole, newAvailable - newBase)
            hole -= newExtra
            val masteredExtra = minOf(hole, masteredAvailable - masteredBase)

            return Take(
                reinforce = reinforceBase + reinforceExtra,
                new = newBase + newExtra,
                mastered = masteredBase + masteredExtra,
            )
        }
    }

    private data class Take(val reinforce: Int, val new: Int, val mastered: Int)

    private fun requireConsistentInputs(
        carry: CarryQueue,
        progress: List<PhraseProgress>,
        catalog: List<CatalogPhrase>,
    ) {
        val progressIds = progress.map { it.id }.toSet()
        val catalogIds = catalog.map { it.id }.toSet()
        require(progressIds.size == progress.size) { "Progress does not allow duplicate ids" }
        require(catalogIds.size == catalog.size) { "The catalog does not allow duplicate ids" }
        require(progressIds.all { it in catalogIds }) { "Some progress belongs to phrases missing from the catalog" }
        require(carry.ids.all { it in progressIds }) { "Some carried phrases have no progress" }
    }

    /** Racha inicial de cada tanda nueva: 0 y sin respuesta en la tanda (ADR-0012). Guarda de robustez. */
    private fun PhraseProgress.restartedInBatch(): PhraseProgress =
        copy(streak = Streak(0), answeredInBatch = false)

    companion object {
        /** Nivel de consolidación mínimo para considerar una frase dominada (design D1). */
        private const val MASTERED_LEVEL = 4

        /** Proporción de N para las cuotas de nuevas y a reforzar; dominadas se lleva el resto (design D1). */
        private const val QUOTA_RATIO = 0.4
    }
}
