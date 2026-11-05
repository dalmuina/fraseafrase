package com.dalmuina.fraseafrase.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.ForceCloseActiveBatch
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
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
    private val forceCloseActiveBatch: ForceCloseActiveBatch,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Última base conocida de lo persistido, para detectar cambios al guardar (design D2). Se inicializa en
     * [reload] y se actualiza tras cada guardado con éxito, para que un segundo guardado en la misma vida del
     * `ViewModel` compare contra lo que de verdad está persistido en ese momento.
     */
    private var persistedSettings: UserSettings? = null

    init {
        viewModelScope.launch { reload() }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectCefrLevel -> selectCefrLevel(action.level)
            is SettingsAction.ChangeBatchSizeText -> changeBatchSizeText(action.text)
            SettingsAction.Save -> save()
            SettingsAction.ConsumeSaveConfirmation -> consumeSaveConfirmation()
            SettingsAction.ConsumeNavigateToPractice -> consumeNavigateToPractice()
        }
    }

    /**
     * Guarda los ajustes mostrados. Solo tiene efecto con [SettingsUiState.Loaded.isBatchSizeValid] a true: es la
     * única condición de la spec para guardar, y la UI deshabilita la acción en caso contrario, pero el ViewModel
     * no asume que la UI siempre lo impide. Si el nivel CEFR o la N cambiaron respecto a lo persistido, cierra la
     * tanda activa sin aviso (design D3) y pide navegar a práctica; en cualquier caso pide confirmar el guardado.
     */
    private fun save() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        val batchSize = state.batchSizeText.toIntOrNull()
        if (!state.isBatchSizeValid || batchSize == null) return
        val newSettings = UserSettings(state.cefrLevel, BatchSize(batchSize))
        viewModelScope.launch {
            saveUserSettings(newSettings)
            val hasChanged = newSettings != persistedSettings
            if (hasChanged) {
                forceCloseActiveBatch()
            }
            persistedSettings = newSettings
            val current = _uiState.value as? SettingsUiState.Loaded ?: return@launch
            _uiState.value = current.copy(
                saveConfirmationPending = true,
                navigateToPracticePending = hasChanged,
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

    private suspend fun reload() {
        val settings = obtainUserSettings()
        persistedSettings = settings
        _uiState.value = SettingsUiState.Loaded(
            cefrLevel = settings.cefrLevel,
            batchSizeText = settings.batchSize.value.toString(),
            isBatchSizeValid = true,
        )
    }
}
