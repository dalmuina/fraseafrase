package com.dalmuina.fraseafrase.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.domain.progress.ObtainProgressOverview
import com.dalmuina.fraseafrase.core.domain.progress.ProgressOverviewResult
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Presentación del progreso (ADR-0006). Lee el resumen al iniciar y solo depende de casos de uso.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val obtainProgressOverview: ObtainProgressOverview,
    private val obtainUserSettings: ObtainUserSettings,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    /** No hay ninguna acción todavía (ver [ProgressAction]). */
    @Suppress("UNUSED_PARAMETER")
    fun onAction(action: ProgressAction) = Unit

    private suspend fun reload() {
        _uiState.value = when (val result = obtainProgressOverview()) {
            is ProgressOverviewResult.Available -> ProgressUiState.Content(
                totalAvailable = result.byLevel.values.sum(),
                cefrLevel = obtainUserSettings().cefrLevel,
                byLevel = result.byLevel,
            )
            is ProgressOverviewResult.CatalogUnavailable -> ProgressUiState.CatalogError
        }
    }
}
