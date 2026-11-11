package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada de la práctica para `:app` (ADR-0006): conecta el [PracticeSessionViewModel] con la pantalla sin
 * estado. Vuelve a leer la sesión cada vez que entra en composición (design D4 de `add-settings-apply-flow`): como
 * no hay `NavHost`, esta pantalla se desmonta y remonta al cambiar de pestaña, así que `LaunchedEffect(Unit)` se
 * ejecuta de nuevo cada vez que el usuario vuelve a ella. [onNavigateToProgress] reenvía la navegación al destino
 * Progreso que ya existe en `:app` desde el enlace del mensaje de tanda completada (spec: practice-session, "El
 * mensaje ofrece ir a Progreso sin generar sesión"); no tiene efecto por defecto, igual que los callbacks de
 * navegación de otras `Route` (design D5 de `add-settings-apply-flow`), para que esta feature no conozca otras.
 */
@Composable
fun PracticeSessionRoute(
    modifier: Modifier = Modifier,
    viewModel: PracticeSessionViewModel = hiltViewModel(),
    onNavigateToProgress: () -> Unit = {},
) {
    LaunchedEffect(Unit) { viewModel.onAction(PracticeSessionAction.Resume) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PracticeSessionScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
        onNavigateToProgress = onNavigateToProgress,
    )
}
