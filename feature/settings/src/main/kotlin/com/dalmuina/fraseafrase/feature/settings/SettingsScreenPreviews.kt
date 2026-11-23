package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

private val PtPt = LanguageTag("pt-PT")
private val EnGb = LanguageTag("en-GB")

@PreviewLightDark
@Composable
private fun SettingsScreenLoadingPreview() {
    AppTheme {
        SettingsScreen(uiState = SettingsUiState.Loading, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenValidPreview() {
    AppTheme {
        SettingsScreen(
            uiState = SettingsUiState.Loaded(
                activeTargetLocale = PtPt,
                availableTargetLocales = listOf(PtPt, EnGb),
                cefrLevel = CefrLevel.B1,
                batchSizeText = "25",
                isBatchSizeValid = true,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsScreenInvalidBatchSizePreview() {
    AppTheme {
        SettingsScreen(
            uiState = SettingsUiState.Loaded(
                activeTargetLocale = PtPt,
                availableTargetLocales = listOf(PtPt, EnGb),
                cefrLevel = CefrLevel.A1,
                batchSizeText = "31",
                isBatchSizeValid = false,
            ),
            onAction = {},
        )
    }
}
