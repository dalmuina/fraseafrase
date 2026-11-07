package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.NothingAvailable
import javax.inject.Inject

/**
 * Compone la tanda siguiente (ADR-0012, ADR-0017). Función pura: la sesión actual llega como argumento,
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

        val reinforceCandidates = progress
            .filter { it.dueAtSession <= currentSession && catalogById.getValue(it.id).level >= cefrLevel }
            .sortedBy { catalogIndex.getValue(it.id) }
        val masteredCandidates = progress
            .filter {
                it.level.value >= MASTERED_LEVEL &&
                    it.dueAtSession > currentSession &&
                    catalogById.getValue(it.id).level >= cefrLevel
            }
            .sortedBy { catalogIndex.getValue(it.id) }
        val newCandidates = catalog.filter { it.level == cefrLevel && it.id !in progressIds }

        val take = quotasOf(size).distribute(
            reinforceCandidates.size,
            newCandidates.size,
            masteredCandidates.size,
        )

        val reinforced = reinforceCandidates.take(take.reinforce)
        val mastered = masteredCandidates.take(take.mastered)
        val new = newCandidates
            .take(take.new)
            .map { PhraseProgress(it.id, Level(0), Streak(0), answeredInBatch = false, dueAtSession = 0) }

        val entries = (reinforced + mastered).map { it.restartedInBatch() } + new
        if (entries.isEmpty()) return NothingAvailable
        return Composed(PracticeBatch(entries, size))
    }

    /** Cuotas por bloque sobre el tamaño N de la tanda (ADR-0012, ADR-0017, design D1/D3). */
    private fun quotasOf(size: BatchSize): Quotas {
        val new = (size.value * NEW_RATIO).toInt()
        val reinforce = (size.value * REINFORCE_RATIO).toInt()
        val mastered = size.value - new - reinforce
        return Quotas(new = new, reinforce = reinforce, mastered = mastered)
    }

    /**
     * Reparto de hueco entre bloques cuando alguno no alcanza su cuota: primero se toma hasta la cuota de
     * cada bloque con sus propios candidatos; el hueco restante se reparte en el orden a reforzar, nuevas,
     * dominadas, entre los bloques con candidatos sobrantes.
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
        /** Nivel de consolidación mínimo para considerar una frase dominada (design D1). */
        private const val MASTERED_LEVEL = 4

        /** Proporción de N para la cuota de nuevas (design D1). */
        private const val NEW_RATIO = 0.20

        /** Proporción de N para la cuota de a reforzar; dominadas se lleva el resto (design D1). */
        private const val REINFORCE_RATIO = 0.55
    }
}
