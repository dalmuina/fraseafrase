package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada de los ajustes para `:app` (ADR-0006): conecta el [SettingsViewModel] con la pantalla sin
 * estado. Observa el estado y reenvía las acciones, y actúa sobre las dos señales de un solo uso de
 * [SettingsUiState.Loaded] (design D1, D5 y D6 de `add-settings-apply-flow`): muestra la confirmación de guardado
 * en un `Snackbar` propio y, si corresponde, invoca [onNavigateToPractice]. [onNavigateToPractice] no tiene efecto
 * por defecto: `:app` es quien decide qué hacer al navegar (design D5), para que esta feature no conozca otras.
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToPractice: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveConfirmationText = stringResource(R.string.settings_save_confirmation)

    val loadedState = uiState as? SettingsUiState.Loaded
    LaunchedEffect(uiState) {
        if (loadedState?.saveConfirmationPending == true) {
            snackbarHostState.showSnackbar(saveConfirmationText)
            viewModel.onAction(SettingsAction.ConsumeSaveConfirmation)
        }
    }
    LaunchedEffect(uiState) {
        if (loadedState?.navigateToPracticePending == true) {
            onNavigateToPractice()
            viewModel.onAction(SettingsAction.ConsumeNavigateToPractice)
        }
    }

    SettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
