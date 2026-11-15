package com.dalmuina.fraseafrase.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.RegenerateActiveBatchForLevel
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainAvailableLanguages
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SelectActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentación de los ajustes (ADR-0006). Lee los ajustes al iniciar y solo depende de casos de uso.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val obtainUserSettings: ObtainUserSettings,
    private val saveUserSettings: SaveUserSettings,
    private val obtainActiveLanguage: ObtainActiveLanguage,
    private val obtainAvailableLanguages: ObtainAvailableLanguages,
    private val selectActiveLanguage: SelectActiveLanguage,
    private val regenerateActiveBatchForLevel: RegenerateActiveBatchForLevel,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Última base conocida de lo persistido por idioma destino, para detectar cambios al guardar (design D2,
     * extendido con el idioma destino en `add-multi-language-catalog`). Se rellena en [loadSettingsFor] y se
     * actualiza tras cada guardado con éxito, para que un segundo guardado del mismo idioma destino en la
     * misma vida del `ViewModel` compare contra lo que de verdad está persistido en ese momento.
     */
    private val persistedSettingsByLocale = mutableMapOf<LanguageTag, UserSettings>()

    init {
        viewModelScope.launch { reload() }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectLanguage -> selectLanguage(action.targetLocale)
            is SettingsAction.SelectCefrLevel -> selectCefrLevel(action.level)
            is SettingsAction.ChangeBatchSizeText -> changeBatchSizeText(action.text)
            SettingsAction.Save -> save()
            SettingsAction.ConsumeSaveConfirmation -> consumeSaveConfirmation()
            SettingsAction.ConsumeNavigateToPractice -> consumeNavigateToPractice()
        }
    }

    /**
     * Guarda los ajustes mostrados para el idioma destino elegido en el desplegable. Solo tiene efecto con
     * [SettingsUiState.Loaded.isBatchSizeValid] a true: es la única condición de la spec para guardar, y la UI
     * deshabilita la acción en caso contrario, pero el ViewModel no asume que la UI siempre lo impide. Cambia
     * el idioma destino activo al elegido ([SelectActiveLanguage]) y, si el nivel CEFR o N cambiaron respecto a
     * lo persistido de ese idioma destino (cualquiera de los dos, o ambos), recompone su tanda activa sin aviso
     * y sin tocar el progreso de ninguna frase (design D4/D5), sin afectar a ningún otro idioma destino. En ese
     * caso pide navegar a práctica; en cualquier caso pide confirmar el guardado.
     */
    private fun save() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        val batchSize = state.batchSizeText.toIntOrNull()
        if (!state.isBatchSizeValid || batchSize == null) return
        val targetLocale = state.activeTargetLocale
        val newSettings = UserSettings(state.cefrLevel, BatchSize(batchSize))
        viewModelScope.launch {
            saveUserSettings(targetLocale, newSettings)
            selectActiveLanguage(targetLocale)
            val previouslyPersisted = persistedSettingsByLocale[targetLocale]
            val hasChanged = previouslyPersisted != null && previouslyPersisted != newSettings
            if (hasChanged) {
                regenerateActiveBatchForLevel(targetLocale, newSettings.cefrLevel, newSettings.batchSize)
            }
            persistedSettingsByLocale[targetLocale] = newSettings
            val current = _uiState.value as? SettingsUiState.Loaded ?: return@launch
            _uiState.value = current.copy(
                saveConfirmationPending = true,
                navigateToPracticePending = hasChanged,
            )
        }
    }

    /** Cambia el idioma destino mostrado y carga sus propios ajustes, sin persistir nada todavía. */
    private fun selectLanguage(targetLocale: LanguageTag) {
        viewModelScope.launch {
            val settings = loadSettingsFor(targetLocale)
            val current = _uiState.value as? SettingsUiState.Loaded ?: return@launch
            _uiState.value = current.copy(
                activeTargetLocale = targetLocale,
                cefrLevel = settings.cefrLevel,
                batchSizeText = settings.batchSize.value.toString(),
                isBatchSizeValid = true,
            )
        }
    }

    private fun selectCefrLevel(level: CefrLevel) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(cefrLevel = level)
    }

    private fun changeBatchSizeText(text: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(batchSizeText = text, isBatchSizeValid = text.isValidBatchSize())
    }

    private fun consumeSaveConfirmation() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(saveConfirmationPending = false)
    }

    private fun consumeNavigateToPractice() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(navigateToPracticePending = false)
    }

    private fun String.isValidBatchSize(): Boolean = toIntOrNull()?.let { it in BatchSize.MIN..BatchSize.MAX } ?: false

    private suspend fun loadSettingsFor(targetLocale: LanguageTag): UserSettings {
        val settings = obtainUserSettings(targetLocale)
        persistedSettingsByLocale[targetLocale] = settings
        return settings
    }

    private suspend fun reload() {
        val activeTargetLocale = obtainActiveLanguage()
        val availableTargetLocales = obtainAvailableLanguages()
        val settings = loadSettingsFor(activeTargetLocale)
        _uiState.value = SettingsUiState.Loaded(
            activeTargetLocale = activeTargetLocale,
            availableTargetLocales = availableTargetLocales,
            cefrLevel = settings.cefrLevel,
            batchSizeText = settings.batchSize.value.toString(),
            isBatchSizeValid = true,
        )
    }
}
