package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import javax.inject.Inject

/**
 * Genera la sesión siguiente en un único paso (ADR-0012, design D3 de
 * `replace-batch-closure-with-session-generation`): resuelve la tanda activa con la misma regla que cerrarla
 * (certifica, sube de nivel, arrastra) y compone inmediatamente la tanda siguiente por cuotas, todo en la misma
 * transacción de [PracticeRepository]. No recibe ningún aviso previo: al no haber expectativa que comprobar, no
 * hay resultado de tipo "obsoleto". Sin tanda activa, compone directamente.
 */
class GenerateSession @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val resolveBatchClosure: ResolveBatchClosure,
    private val composeNextBatch: ComposeNextBatch,
    private val catalogRepository: CatalogRepository,
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(): ActiveBatchResult {
        val catalog = when (val loaded = catalogRepository.phrases()) {
            is CatalogLoadResult.Loaded -> loaded.phrases
            is CatalogLoadResult.Failed -> return ActiveBatchResult.CatalogUnavailable(loaded.error)
        }
        val settings = settingsRepository.settings()
        return practiceRepository.transact { snapshot ->
            val advanced = snapshot.batch()?.let { advanceActiveBatch(snapshot, it, resolveBatchClosure) }
                ?: snapshot
            compose(advanced, catalog, settings.cefrLevel, settings.batchSize)
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
