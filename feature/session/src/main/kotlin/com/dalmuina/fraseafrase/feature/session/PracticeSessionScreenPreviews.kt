package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/** Frase sabida (racha 3): se muestra la marca de sabida. */
private val knownItem = PracticeItem(
    id = PhraseId("A"),
    source = "Buenos días",
    target = "Bom dia",
    streak = Streak(3),
    isKnown = true,
)

/** Frase nueva, sin racha ni marca. */
private val newItem = PracticeItem(
    id = PhraseId("B"),
    source = "Gracias",
    target = "Obrigado",
    streak = Streak(0),
    isKnown = false,
)

@PreviewLightDark
@Composable
private fun PracticeSessionScreenLoadingPreview() {
    AppTheme {
        PracticeSessionScreen(uiState = PracticeSessionUiState.Loading, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun PracticeSessionScreenHiddenPreview() {
    AppTheme {
        PracticeSessionScreen(
            uiState = PracticeSessionUiState.Practicing(
                current = knownItem,
                revealed = false,
                busy = false,
                closeWarning = null,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PracticeSessionScreenRevealedPreview() {
    AppTheme {
        PracticeSessionScreen(
            uiState = PracticeSessionUiState.Practicing(
                current = newItem,
                revealed = true,
                busy = false,
                closeWarning = null,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PracticeSessionScreenCloseWarningPreview() {
    AppTheme {
        PracticeSessionScreen(
            uiState = PracticeSessionUiState.Practicing(
                current = newItem,
                revealed = true,
                busy = false,
                closeWarning = 3,
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PracticeSessionScreenNothingToPracticePreview() {
    AppTheme {
        PracticeSessionScreen(uiState = PracticeSessionUiState.NothingToPractice, onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun PracticeSessionScreenCatalogErrorPreview() {
    AppTheme {
        PracticeSessionScreen(uiState = PracticeSessionUiState.CatalogError, onAction = {})
    }
}
