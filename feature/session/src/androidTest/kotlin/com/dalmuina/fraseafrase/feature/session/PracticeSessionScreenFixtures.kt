package com.dalmuina.fraseafrase.feature.session

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/**
 * Builders y helper de contenido compartidos entre [PracticeSessionScreenTest] y
 * [PracticeSessionRevertButtonScreenTest] (ADR-0007). Viven en un archivo propio, separado de las clases de
 * test, para que ambas puedan usarlos sin duplicarlos ni exponerlos como miembros `private` de una sola
 * clase (misma razón por la que `PracticeSessionFixtures.kt` separa los builders de los tests unitarios de
 * `PracticeSessionViewModelTest`/`PracticeSessionRevertTest`).
 */
internal fun ComposeContentTestRule.setScreen(uiState: PracticeSessionUiState) {
    setContent {
        AppTheme {
            PracticeSessionScreen(uiState = uiState, onAction = {})
        }
    }
}

internal fun practicing(
    current: PracticeItem = item(),
    revealed: Boolean = false,
    busy: Boolean = false,
    completed: Boolean = false,
    badgeChange: BadgeChange? = null,
): PracticeSessionUiState.Practicing = PracticeSessionUiState.Practicing(
    current = current,
    revealed = revealed,
    busy = busy,
    completed = completed,
    index = 1,
    total = 1,
    remainingHitsForNextLevel = 0,
    nextLevelBadgeLabel = "Bronce",
    badgeChange = badgeChange,
)

/** Frase de presentación. */
internal fun item(
    source: String = "Buenos días",
    target: String = "Bom dia",
    cefrLevel: CefrLevel = CefrLevel.A1,
    streak: Int = 0,
): PracticeItem = PracticeItem(
    id = PhraseId("A"),
    source = source,
    target = target,
    targetLocale = LanguageTag("pt-PT"),
    cefrLevel = cefrLevel,
    streak = Streak(streak),
    level = Level(0),
)
