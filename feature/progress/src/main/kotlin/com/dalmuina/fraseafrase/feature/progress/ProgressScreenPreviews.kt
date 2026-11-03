package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

@PreviewLightDark
@Composable
private fun ProgressScreenLoadingPreview() {
    AppTheme {
        ProgressScreen(uiState = ProgressUiState.Loading)
    }
}

@PreviewLightDark
@Composable
private fun ProgressScreenWithActiveBatchPreview() {
    AppTheme {
        ProgressScreen(
            uiState = ProgressUiState.Content(
                certifiedInBatch = CertifiedCount(certified = 2, total = 5),
                byLevel = mapOf(
                    Level(0) to 6,
                    Level(1) to 3,
                    Level(2) to 2,
                    Level(3) to 1,
                    Level(4) to 0,
                    Level(5) to 1,
                ),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProgressScreenWithoutActiveBatchPreview() {
    AppTheme {
        ProgressScreen(
            uiState = ProgressUiState.Content(
                certifiedInBatch = null,
                byLevel = mapOf(
                    Level(0) to 6,
                    Level(1) to 0,
                    Level(2) to 0,
                    Level(3) to 0,
                    Level(4) to 0,
                    Level(5) to 0,
                ),
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProgressScreenCatalogErrorPreview() {
    AppTheme {
        ProgressScreen(uiState = ProgressUiState.CatalogError)
    }
}
