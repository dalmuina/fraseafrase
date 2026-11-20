package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import javax.inject.Inject

/**
 * Devuelve la tanda activa de [targetLocale], recomponiéndola de inmediato si sus ajustes aplicados
 * (`SettingsRepository`) difieren de los ajustes guardados vigentes, o componiendo la primera si no hay
 * ninguna (ADR-0012, ADR-0013, spec `practice-persistence`, design D2 de `remove-settings-save-button`). La
 * comparación se repite en cada obtención para que sobreviva al cierre de la app.
 */
class ObtainActiveBatch @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val practiceRepository: PracticeRepository,
    private val regenerateActiveBatchForLevel: RegenerateActiveBatchForLevel,
) {

    suspend operator fun invoke(targetLocale: LanguageTag): ActiveBatchResult {
        val settings = settingsRepository.settings(targetLocale)
        val existing = practiceRepository.read(targetLocale).batch()
        if (existing != null) {
            val applied = settingsRepository.appliedSettings(targetLocale)
            if (applied == null) settingsRepository.recordAppliedSettings(targetLocale, settings)
            if (applied == null || applied == settings) return ActiveBatchResult.Existing(existing)
        }
        val result = regenerateActiveBatchForLevel(targetLocale, settings.cefrLevel, settings.batchSize)
        if (result is ActiveBatchResult.Created) {
            settingsRepository.recordAppliedSettings(targetLocale, settings)
        }
        return result
    }
}
