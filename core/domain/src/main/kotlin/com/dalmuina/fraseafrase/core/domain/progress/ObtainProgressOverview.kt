package com.dalmuina.fraseafrase.core.domain.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import javax.inject.Inject

/**
 * Lee el resumen de progreso (ADR-0018): el total disponible de frases del catálogo del nivel CEFR
 * vigente, ese mismo nivel CEFR y el total por nivel de consolidación; ninguno de los tres depende
 * de si hay tanda activa. Combina [CatalogRepository.phrases], [PracticeRepository.read] y
 * [ObtainUserSettings]; no modifica ninguno de los tres.
 */
class ObtainProgressOverview @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val practiceRepository: PracticeRepository,
    private val obtainUserSettings: ObtainUserSettings,
) {

    suspend operator fun invoke(): ProgressOverviewResult = when (val loaded = catalogRepository.phrases()) {
        is CatalogLoadResult.Failed -> ProgressOverviewResult.CatalogUnavailable(loaded.error)
        is CatalogLoadResult.Loaded -> available(loaded)
    }

    private suspend fun available(loaded: CatalogLoadResult.Loaded): ProgressOverviewResult.Available {
        val snapshot = practiceRepository.read()
        val settings = obtainUserSettings()
        val phrasesAtCefrLevel = loaded.phrases.filter { it.level == settings.cefrLevel }
        val byLevel = byLevel(phrasesAtCefrLevel, snapshot)
        return ProgressOverviewResult.Available(
            totalAvailable = byLevel.values.sum(),
            cefrLevel = settings.cefrLevel,
            byLevel = byLevel,
        )
    }

    private fun byLevel(phrases: List<CatalogPhrase>, snapshot: PracticeSnapshot): Map<Level, Int> {
        val progressById = snapshot.progress.associateBy { it.id }
        val levels = phrases.map { progressById[it.id]?.level ?: Level(0) }
        return (0..Level.MAX_LEVEL).associate { value -> Level(value) to levels.count { it == Level(value) } }
    }
}
