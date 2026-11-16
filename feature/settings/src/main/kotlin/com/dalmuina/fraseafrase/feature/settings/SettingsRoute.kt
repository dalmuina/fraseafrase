package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada de los ajustes para `:app` (ADR-0006): conecta el [SettingsViewModel] con la pantalla sin
 * estado. Observa el estado y reenvía las acciones. Desde `remove-settings-save-button` (design D3), cada
 * edición válida se autoguarda de inmediato dentro del propio `ViewModel`: no hay ninguna señal de un solo uso
 * que consumir ni navegación a práctica que disparar desde aquí (el usuario navega pulsando la pestaña de
 * práctica, `app-navigation`). [onNavigateToSettings] reenvía la navegación al tocar el icono de idioma de la
 * propia cabecera (spec: language-indicator, "Tocar el icono estando ya en Ajustes no hace nada"); no tiene
 * efecto por defecto.
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
        onNavigateToSettings = onNavigateToSettings,
    )
}
