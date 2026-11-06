package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/** Frase cerca de certificarse (racha 1, nivel 0): le faltan 2 aciertos para la insignia "Bronce". */
private val almostCertifiedItem = PracticeItem(
    id = PhraseId("A"),
    source = "Buenos días",
    target = "Bom dia",
    targetLocale = LanguageTag("pt-PT"),
    cefrLevel = CefrLevel.A1,
    streak = Streak(1),
    isKnown = false,
    level = Level(0),
)

/** Frase nueva, sin racha. */
private val newItem = PracticeItem(
    id = PhraseId("B"),
    source = "Gracias",
    target = "Obrigado",
    targetLocale = LanguageTag("pt-PT"),
    cefrLevel = CefrLevel.A1,
    streak = Streak(0),
    isKnown = false,
    level = Level(0),
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
                current = almostCertifiedItem,
                revealed = false,
                busy = false,
                completed = false,
                index = 1,
                total = 2,
                remainingHitsForNextLevel = 2,
                nextLevelBadgeLabel = "Bronce",
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
                completed = false,
                index = 2,
                total = 2,
                remainingHitsForNextLevel = 3,
                nextLevelBadgeLabel = "Bronce",
            ),
            onAction = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PracticeSessionScreenCompletedPreview() {
    AppTheme {
        PracticeSessionScreen(
            uiState = PracticeSessionUiState.Practicing(
                current = newItem,
                revealed = false,
                busy = false,
                completed = true,
                index = 2,
                total = 2,
                remainingHitsForNextLevel = 3,
                nextLevelBadgeLabel = "Bronce",
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
