package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsLevelBadgeChange] (ADR-0007, add-level-change-animation design D4). Verifican
 * la presencia o ausencia del halo según el énfasis, sin comprobar su aspecto exacto.
 */
class DsLevelBadgeChangeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: La subida se muestra con halo (spec: practice-session, design D4)
    @Test
    fun givenEmphasizedWhenPaintingThenHaloIsShown() {
        // Given/When: se pinta el cambio de insignia enfatizado (subida)
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadgeChange(level = 1, emphasized = true)
            }
        }

        // Then: el halo está presente en el árbol
        composeTestRule.onNodeWithTag("ds-level-badge-change-halo").assertExists()
    }

    // Scenario: La bajada se muestra sin halo (spec: practice-session, design D4)
    @Test
    fun givenNotEmphasizedWhenPaintingThenHaloIsAbsent() {
        // Given/When: se pinta el cambio de insignia sin énfasis (bajada)
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadgeChange(level = 1, emphasized = false)
            }
        }

        // Then: el halo no está presente en el árbol
        composeTestRule.onNodeWithTag("ds-level-badge-change-halo").assertDoesNotExist()
    }
}
