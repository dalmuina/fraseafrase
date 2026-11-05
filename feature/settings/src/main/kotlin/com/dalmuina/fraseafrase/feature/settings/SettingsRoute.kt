package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada de los ajustes para `:app` (ADR-0006): conecta el [SettingsViewModel] con la pantalla sin
 * estado. Observa el estado y reenvía las acciones, y actúa sobre las dos señales de un solo uso de
 * [SettingsUiState.Loaded] (design D1, D5 y D6 (revisada) de `add-settings-apply-flow`): llama a
 * [onShowSaveConfirmation] con el texto de confirmación y, si corresponde, a [onNavigateToPractice]. Ninguno de los
 * dos callbacks tiene efecto por defecto: `:app` decide cómo mostrar la confirmación (un `Snackbar` que sobreviva al
 * cambio de pestaña) y a dónde navegar, para que esta feature no conozca otras (design D5). El envío de las
 * acciones de consumo no espera a que termine ningún callback, para que la cancelación de esta composición al
 * navegar no se lleve por delante la confirmación (design D6 revisada).
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToPractice: () -> Unit = {},
    onShowSaveConfirmation: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveConfirmationText = stringResource(R.string.settings_save_confirmation)

    val loadedState = uiState as? SettingsUiState.Loaded
    LaunchedEffect(uiState) {
        if (loadedState?.saveConfirmationPending == true) {
            onShowSaveConfirmation(saveConfirmationText)
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
    )
}
