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
 * Recompone la tanda activa al cambiar el nivel CEFR elegido (ADR-0012, design D4 de
 * `replace-batch-closure-with-session-generation`): mueve las entradas de la tanda activa a la cola de
 * arrastradas tal cual, sin certificar ni cambiar nivel de consolidación ni próximo repaso de ninguna frase
 * (sin resolución de cierre), y compone de inmediato la tanda siguiente por cuotas con el nuevo nivel CEFR. Sin
 * tanda activa, compone directamente con el nuevo nivel.
 */
class RegenerateActiveBatchForLevel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val composeNextBatch: ComposeNextBatch,
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(cefrLevel: CefrLevel): ActiveBatchResult {
        val catalog = when (val loaded = catalogRepository.phrases()) {
            is CatalogLoadResult.Loaded -> loaded.phrases
            is CatalogLoadResult.Failed -> return ActiveBatchResult.CatalogUnavailable(loaded.error)
        }
        val size = settingsRepository.settings().batchSize
        return practiceRepository.transact { snapshot ->
            val advanced = snapshot.batch()?.let { advanceActiveBatch(snapshot, it, resolveBatchClosure = null) }
                ?: snapshot
            compose(advanced, catalog, cefrLevel, size)
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
