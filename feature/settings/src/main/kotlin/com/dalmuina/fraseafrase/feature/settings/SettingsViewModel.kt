package com.dalmuina.fraseafrase.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainAvailableLanguages
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SelectActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.SelectLanguageResult
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentación de los ajustes (ADR-0006). Lee los ajustes al iniciar y solo depende de casos de uso. Desde
 * `remove-settings-save-button` (design D3), cada edición válida se autoguarda de inmediato: decidir si hay
 * que recomponer la tanda activa deja de ser responsabilidad de esta pantalla, ocurre al obtener la tanda
 * activa (`practice-persistence`).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val obtainUserSettings: ObtainUserSettings,
    private val saveUserSettings: SaveUserSettings,
    private val obtainActiveLanguage: ObtainActiveLanguage,
    private val obtainAvailableLanguages: ObtainAvailableLanguages,
    private val selectActiveLanguage: SelectActiveLanguage,
    private val obtainSourceLanguage: ObtainSourceLanguage,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectLanguage -> selectLanguage(action.targetLocale)
            is SettingsAction.SelectCefrLevel -> selectCefrLevel(action.level)
            is SettingsAction.ChangeBatchSizeText -> changeBatchSizeText(action.text)
        }
    }

    /**
     * Cambia el idioma destino mostrado, carga sus propios ajustes y lo activa de inmediato (spec: "Elegir un
     * idioma destino lo activa de inmediato"), sin esperar a que el usuario salga de la pantalla.
     */
    private fun selectLanguage(targetLocale: LanguageTag) {
        viewModelScope.launch {
            val settings = obtainUserSettings(targetLocale)
            if (selectActiveLanguage(targetLocale) == SelectLanguageResult.RejectedSourceLocale) {
                // The chosen language became the source language: refresh the options, keep the active target.
                reload()
                return@launch
            }
            val current = _uiState.value as? SettingsUiState.Loaded ?: return@launch
            _uiState.value = current.copy(
                activeTargetLocale = targetLocale,
                cefrLevel = settings.cefrLevel,
                batchSizeText = settings.batchSize.value.toString(),
                isBatchSizeValid = true,
            )
        }
    }

    /** Elige un nivel CEFR y lo autoguarda de inmediato para el idioma destino mostrado (spec: "Guardado de los
     * ajustes editados"). */
    private fun selectCefrLevel(level: CefrLevel) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(cefrLevel = level)
        saveIfValid(state.activeTargetLocale, level, state.batchSizeText)
    }

    /** Cambia el texto en edición del tamaño de tanda y, si es válido, lo autoguarda de inmediato. */
    private fun changeBatchSizeText(text: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(batchSizeText = text, isBatchSizeValid = text.isValidBatchSize())
        saveIfValid(state.activeTargetLocale, state.cefrLevel, text)
    }

    private fun saveIfValid(targetLocale: LanguageTag, cefrLevel: CefrLevel, batchSizeText: String) {
        val batchSize = batchSizeText.toIntOrNull() ?: return
        if (!batchSizeText.isValidBatchSize()) return
        viewModelScope.launch {
            saveUserSettings(targetLocale, UserSettings(cefrLevel, BatchSize(batchSize)))
        }
    }

    private fun String.isValidBatchSize(): Boolean =
        toIntOrNull()?.let { it in BatchSize.MIN..BatchSize.MAX } ?: false

    private suspend fun reload() {
        val activeTargetLocale = obtainActiveLanguage()
        val sourceLocale = obtainSourceLanguage()
        val availableTargetLocales = obtainAvailableLanguages().filter { it != sourceLocale }
        val settings = obtainUserSettings(activeTargetLocale)
        _uiState.value = SettingsUiState.Loaded(
            activeTargetLocale = activeTargetLocale,
            availableTargetLocales = availableTargetLocales,
            cefrLevel = settings.cefrLevel,
            batchSizeText = settings.batchSize.value.toString(),
            isBatchSizeValid = true,
        )
    }
}
