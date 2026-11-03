package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada de los ajustes para `:app` (ADR-0006): conecta el [SettingsViewModel] con la pantalla sin
 * estado. [onBack] es un callback de UI que vuelve a la práctica, independiente de haber guardado (spec
 * `settings-screen`, "Vuelta a la práctica desde los ajustes"); no pasa por el ViewModel.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}
