package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import javax.inject.Inject

/**
 * Devuelve la tanda activa o compone y guarda la siguiente (ADR-0012, ADR-0013). El catálogo y los ajustes se leen
 * antes de la transacción, porque son inmutables en esta app; la composición y su escritura van en una sola
 * transacción con el estado del usuario.
 */
class ObtainActiveBatch @Inject constructor(
    private val composeNextBatch: ComposeNextBatch,
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
    private val practiceRepository: PracticeRepository,
) {

    suspend operator fun invoke(): ActiveBatchResult {
        val catalog = when (val loaded = catalogRepository.phrases()) {
            is CatalogLoadResult.Loaded -> loaded.phrases
            is CatalogLoadResult.Failed -> return ActiveBatchResult.CatalogUnavailable(loaded.error)
        }
        val settings = settingsRepository.settings()
        return practiceRepository.transact { snapshot ->
            snapshot.batch()?.let { PracticeChange(snapshot, ActiveBatchResult.Existing(it)) }
                ?: compose(snapshot, catalog, settings.cefrLevel, settings.batchSize)
        }
    }

    private fun compose(
        snapshot: PracticeSnapshot,
        catalog: List<CatalogPhrase>,
        cefrLevel: CefrLevel,
        size: BatchSize,
    ): PracticeChange<ActiveBatchResult> =
        when (val composed = composeNextBatch(snapshot.carry, snapshot.progress, catalog, cefrLevel, size)) {
            is BatchComposition.NothingAvailable -> PracticeChange(snapshot, ActiveBatchResult.NothingAvailable)
            is BatchComposition.Composed -> PracticeChange(
                next = snapshot.storing(composed.batch).copy(carry = composed.leftover),
                result = ActiveBatchResult.Created(composed.batch),
            )
        }
}
