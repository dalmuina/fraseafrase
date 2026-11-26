package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainCatalogPhrasesForActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import javax.inject.Inject

/**
 * Recompone la tanda activa al cambiar los ajustes de práctica (nivel CEFR y/o tamaño N) (ADR-0019, ADR-0026):
 * vacía la tanda activa sin resolución (sin certificar ni cambiar racha, nivel de consolidación ni `dueAtSession`
 * de ninguna frase) y compone de inmediato la tanda siguiente con [cefrLevel] y [batchSize] recién
 * guardados, sin incrementar el contador de sesiones generadas. Sin tanda activa, compone directamente con los
 * nuevos valores.
 *
 * Solo compone si, dentro de la transacción, la tanda activa sigue siendo `expected`, la que el llamador observó
 * al decidir recomponer (`null` si no había). En caso contrario no cambia nada y devuelve la vigente como
 * [ActiveBatchResult.Existing], o [ActiveBatchResult.NothingAvailable] si ya no hay ninguna.
 */
class RegenerateActiveBatchForLevel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val composeNextBatch: ComposeNextBatch,
    private val obtainCatalogPhrases: ObtainCatalogPhrasesForActiveLanguage,
) {

    suspend operator fun invoke(
        targetLocale: LanguageTag,
        cefrLevel: CefrLevel,
        batchSize: BatchSize,
        expected: ActiveBatch?,
    ): ActiveBatchResult {
        val catalog = when (val loaded = obtainCatalogPhrases(targetLocale)) {
            is CatalogLoadResult.Loaded -> loaded.phrases
            is CatalogLoadResult.Failed -> return ActiveBatchResult.CatalogUnavailable(loaded.error)
        }
        val settings = UserSettings(cefrLevel, batchSize)
        return practiceRepository.transact(targetLocale) { snapshot ->
            if (snapshot.activeBatch != expected) {
                PracticeChange(snapshot, snapshot.currentBatchResult())
            } else {
                val cleared = snapshot.copy(activeBatch = null)
                composeNextBatchInto(composeNextBatch, cleared, catalog, settings, snapshot.sessionCount)
            }
        }
    }
}
