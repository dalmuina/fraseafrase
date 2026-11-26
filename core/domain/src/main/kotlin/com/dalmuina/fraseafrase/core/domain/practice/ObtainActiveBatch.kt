package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import javax.inject.Inject

/**
 * Devuelve la tanda activa de [targetLocale], recomponiéndola de inmediato si sus ajustes aplicados
 * (`SettingsRepository`) difieren de los ajustes guardados vigentes, o componiendo la primera si no hay
 * ninguna (ADR-0026, ADR-0019, spec `practice-persistence`, design D2 de `remove-settings-save-button`). La
 * comparación se repite en cada obtención para que sobreviva al cierre de la app.
 *
 * La recomposición se condiciona a la tanda leída aquí: si al escribir ya es otra, se devuelve la vigente sin
 * recomponerla (ver [RegenerateActiveBatchForLevel]).
 *
 * Cuando no recompone, vuelve a leer la tanda después de consultar los ajustes aplicados, porque otra obtención
 * puede haberla recompuesto entre ambas lecturas; devuelve [ActiveBatchResult.NothingAvailable] si ya no hay
 * ninguna.
 */
class ObtainActiveBatch @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val practiceRepository: PracticeRepository,
    private val regenerateActiveBatchForLevel: RegenerateActiveBatchForLevel,
) {

    suspend operator fun invoke(targetLocale: LanguageTag): ActiveBatchResult {
        val settings = settingsRepository.settings(targetLocale)
        val observed = practiceRepository.read(targetLocale)
        if (observed.batch() != null) {
            val applied = settingsRepository.appliedSettings(targetLocale)
            if (applied == null) settingsRepository.recordAppliedSettings(targetLocale, settings)
            if (applied == null || applied == settings) {
                return practiceRepository.read(targetLocale).currentBatchResult()
            }
        }
        val result = regenerateActiveBatchForLevel(
            targetLocale,
            settings.cefrLevel,
            settings.batchSize,
            expected = observed.activeBatch,
        )
        if (result is ActiveBatchResult.Created) {
            settingsRepository.recordAppliedSettings(targetLocale, settings)
        }
        return result
    }
}
