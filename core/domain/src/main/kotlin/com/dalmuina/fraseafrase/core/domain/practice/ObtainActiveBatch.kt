package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
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
                ?: compose(snapshot, catalog, settings)
        }
    }

    private fun compose(
        snapshot: PracticeSnapshot,
        catalog: List<CatalogPhrase>,
        settings: UserSettings,
    ): PracticeChange<ActiveBatchResult> =
        composeNextBatchInto(composeNextBatch, snapshot, catalog, settings, snapshot.sessionCount)
}
