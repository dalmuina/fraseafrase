package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PracticeBatch
import javax.inject.Inject

/**
 * Lee la sesión de práctica: la tanda activa con el texto de cada frase (ADR-0012). Compone la tanda si no existe,
 * a través de [ObtainActiveBatch], y une cada entrada con su frase del catálogo por id, en el orden de la tanda.
 */
class ObtainPracticeSession @Inject constructor(
    private val obtainActiveBatch: ObtainActiveBatch,
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(): PracticeSessionResult = when (val active = obtainActiveBatch()) {
        is ActiveBatchResult.Existing -> withText(active.batch)
        is ActiveBatchResult.Created -> withText(active.batch)
        ActiveBatchResult.NothingAvailable -> PracticeSessionResult.NothingToPractice
        is ActiveBatchResult.CatalogUnavailable -> PracticeSessionResult.CatalogFailed(active.error)
    }

    private suspend fun withText(batch: PracticeBatch): PracticeSessionResult =
        when (val loaded = catalogRepository.phrases()) {
            is CatalogLoadResult.Loaded -> PracticeSessionResult.Ready(
                items = items(batch, loaded.phrases),
                uncertifiedCount = batch.uncertifiedCount(),
            )
            is CatalogLoadResult.Failed -> PracticeSessionResult.CatalogFailed(loaded.error)
        }

    private fun items(batch: PracticeBatch, phrases: List<CatalogPhrase>): List<PracticeItem> {
        val byId = phrases.associateBy { it.id }
        return batch.entries.map { entry ->
            val phrase = requireNotNull(byId[entry.id]) {
                "Phrase ${entry.id.value} is in the batch but not in the catalog"
            }
            PracticeItem(
                id = entry.id,
                source = phrase.source,
                target = phrase.target,
                streak = entry.streak,
                isKnown = entry.streak.value >= PracticeItem.KNOWN_STREAK,
            )
        }
    }
}
