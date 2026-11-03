package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Punto de entrada del progreso para `:app` (ADR-0006): conecta el [ProgressViewModel] con la pantalla sin estado.
 */
@Composable
fun ProgressRoute(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        uiState = uiState,
        modifier = modifier,
    )
}
