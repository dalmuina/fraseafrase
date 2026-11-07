package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import javax.inject.Inject

/**
 * Recompone la tanda activa al cambiar los ajustes de práctica (nivel CEFR y/o tamaño N) (ADR-0012, ADR-0017,
 * design D4/D5 de `repeat-by-session-counter`): vacía la tanda activa sin resolución (sin certificar ni
 * cambiar racha, nivel de consolidación ni `dueAtSession` de ninguna frase) y compone de inmediato la tanda
 * siguiente por cuotas con [cefrLevel] y [batchSize] recién guardados, sin incrementar el contador de sesiones
 * generadas. Sin tanda activa, compone directamente con los nuevos valores.
 */
class RegenerateActiveBatchForLevel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val composeNextBatch: ComposeNextBatch,
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(cefrLevel: CefrLevel, batchSize: BatchSize): ActiveBatchResult {
        val catalog = when (val loaded = catalogRepository.phrases()) {
            is CatalogLoadResult.Loaded -> loaded.phrases
            is CatalogLoadResult.Failed -> return ActiveBatchResult.CatalogUnavailable(loaded.error)
        }
        val settings = UserSettings(cefrLevel, batchSize)
        return practiceRepository.transact { snapshot ->
            val cleared = snapshot.copy(activeBatch = null)
            composeNextBatchInto(composeNextBatch, cleared, catalog, settings, snapshot.sessionCount)
        }
    }
}
