package com.dalmuina.fraseafrase.core.domain.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import javax.inject.Inject

/**
 * Lee el resumen de progreso (ADR-0012): certificadas de la tanda activa y total de frases por
 * nivel. Combina [CatalogRepository.phrases] y [PracticeRepository.read]; no modifica ninguno de
 * los dos.
 */
class ObtainProgressOverview @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val practiceRepository: PracticeRepository,
) {

    suspend operator fun invoke(): ProgressOverviewResult = when (val loaded = catalogRepository.phrases()) {
        is CatalogLoadResult.Failed -> ProgressOverviewResult.CatalogUnavailable(loaded.error)
        is CatalogLoadResult.Loaded -> available(loaded)
    }

    private suspend fun available(loaded: CatalogLoadResult.Loaded): ProgressOverviewResult.Available {
        val snapshot = practiceRepository.read()
        val batchProgress = snapshot.batch()?.let { batch ->
            BatchProgress(certified = batch.entries.count { it.isCertified }, total = batch.entries.size)
        }
        return ProgressOverviewResult.Available(
            batchProgress = batchProgress,
            byLevel = byLevel(loaded.phrases, snapshot),
        )
    }

    private fun byLevel(phrases: List<CatalogPhrase>, snapshot: PracticeSnapshot): Map<Level, Int> {
        val progressById = snapshot.progress.associateBy { it.id }
        val levels = phrases.map { progressById[it.id]?.level ?: Level(0) }
        return (0..Level.MAX_LEVEL).associate { value -> Level(value) to levels.count { it == Level(value) } }
    }
}
