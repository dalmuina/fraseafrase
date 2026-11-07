package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import javax.inject.Inject

/**
 * Genera la sesión siguiente en un único paso (ADR-0012, ADR-0017, design D4 de `repeat-by-session-counter`):
 * incrementa el contador de sesiones generadas y compone inmediatamente la tanda siguiente por cuotas, a
 * partir del progreso ya resuelto (cada frase se resuelve al instante de su propia autoevaluación). No recibe
 * ningún aviso previo: al no haber expectativa que comprobar, no hay resultado de tipo "obsoleto". Sin tanda
 * activa, compone directamente.
 */
class GenerateSession @Inject constructor(
    private val practiceRepository: PracticeRepository,
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
            val nextSessionCount = snapshot.sessionCount + 1
            val cleared = snapshot.copy(activeBatch = null, sessionCount = nextSessionCount)
            composeNextBatchInto(composeNextBatch, cleared, catalog, settings, nextSessionCount)
        }
    }
}
