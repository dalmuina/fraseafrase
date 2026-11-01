package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada de la práctica para `:app` (ADR-0006): conecta el [PracticeSessionViewModel] con la pantalla sin
 * estado. Solo observa el estado y reenvía las acciones; no contiene lógica.
 */
@Composable
fun PracticeSessionRoute(
    modifier: Modifier = Modifier,
    viewModel: PracticeSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PracticeSessionScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}
