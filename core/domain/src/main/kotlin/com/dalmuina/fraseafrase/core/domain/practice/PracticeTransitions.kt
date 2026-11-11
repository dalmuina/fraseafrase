package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch

/**
 * Compone la tanda siguiente por cuotas a partir de [snapshot] y la guarda, o deja [snapshot] sin cambios si no
 * hay frases disponibles (ADR-0012, ADR-0017). Transición pura compartida por [GenerateSession],
 * [RegenerateActiveBatchForLevel] y [ObtainActiveBatch].
 */
internal fun composeNextBatchInto(
    composeNextBatch: ComposeNextBatch,
    snapshot: PracticeSnapshot,
    catalog: List<CatalogPhrase>,
    settings: UserSettings,
    currentSession: Int,
): PracticeChange<ActiveBatchResult> {
    val composed = composeNextBatch(
        snapshot.progress,
        catalog,
        settings.cefrLevel,
        settings.batchSize,
        currentSession,
    )
    return when (composed) {
        is BatchComposition.NothingAvailable -> PracticeChange(snapshot, ActiveBatchResult.NothingAvailable)
        is BatchComposition.Composed -> PracticeChange(
            next = snapshot.storing(composed.batch).copy(batchOutcome = BatchOutcome()),
            result = ActiveBatchResult.Created(composed.batch),
        )
    }
}
