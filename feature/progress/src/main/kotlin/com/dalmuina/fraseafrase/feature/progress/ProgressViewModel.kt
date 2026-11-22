package com.dalmuina.fraseafrase.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dalmuina.fraseafrase.core.domain.progress.ObtainProgressOverview
import com.dalmuina.fraseafrase.core.domain.progress.ProgressOverviewResult
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val obtainActiveLanguage: ObtainActiveLanguage,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var reloadJob: Job? = null

    init {
        launchReload()
    }

    fun onAction(action: ProgressAction) {
        when (action) {
            ProgressAction.Resume -> launchReload()
        }
    }

    /** Cancela la relectura anterior para que una lectura antigua no pise a una más reciente. */
    private fun launchReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        val targetLocale = obtainActiveLanguage()
        _uiState.value = when (val result = obtainProgressOverview(targetLocale)) {
            is ProgressOverviewResult.Available -> ProgressUiState.Content(
                totalAvailable = result.byLevel.values.sum(),
                cefrLevel = obtainUserSettings(targetLocale).cefrLevel,
                byLevel = result.byLevel,
                activeTargetLocale = targetLocale,
            )
            is ProgressOverviewResult.CatalogUnavailable -> ProgressUiState.CatalogError
        }
    }
}
