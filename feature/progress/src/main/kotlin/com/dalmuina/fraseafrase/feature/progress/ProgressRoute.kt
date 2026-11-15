package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada del progreso para `:app` (ADR-0006): conecta el [ProgressViewModel] con la pantalla sin estado.
 * [onNavigateToSettings] reenvía la navegación a Ajustes al tocar el icono de idioma de la cabecera (spec:
 * language-indicator, "Tocar el icono navega a Ajustes"); no tiene efecto por defecto, para que esta feature no
 * conozca otras (design D5 de `add-settings-apply-flow`).
 */
@Composable
fun ProgressRoute(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        uiState = uiState,
        modifier = modifier,
        onNavigateToSettings = onNavigateToSettings,
    )
}
