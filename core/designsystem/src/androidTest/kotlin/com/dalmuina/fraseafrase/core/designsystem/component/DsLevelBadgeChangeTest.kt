package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.dalmuina.fraseafrase.core.designsystem.motion.Motion
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/** Margen sobre [Motion.durationMediumMillis] para asegurar que la transición ya ha terminado. */
private const val EXTRA_MILLIS = 50L

/**
 * Tests de UI de [DsLevelBadgeChange] (ADR-0007, new-badge-animation design D4). Verifican la
 * presencia o ausencia del halo según el énfasis, y que la transición entre [fromLevel]/[toLevel]
 * ocurre de verdad (crossfade), no solo que el componente se pinta de forma estática.
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
                DsLevelBadgeChange(fromLevel = 1, toLevel = 2, emphasized = true)
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
                DsLevelBadgeChange(fromLevel = 2, toLevel = 1, emphasized = false)
            }
        }

        // Then: el halo no está presente en el árbol
        composeTestRule.onNodeWithTag("ds-level-badge-change-halo").assertDoesNotExist()
    }

    // Scenario: Un acierto que certifica muestra el indicador (spec: practice-session)
    @Test
    fun givenLevelChangeWhenTransitionStartsThenFromLevelIsShownAndToLevelIsNot() {
        // Given: el reloj de composición está pausado, antes de que transcurra la transición
        composeTestRule.mainClock.autoAdvance = false

        // When: se pinta el cambio de insignia de fromLevel a toLevel
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadgeChange(fromLevel = 1, toLevel = 2, emphasized = true)
            }
        }

        // Then: en t≈0 se ve la etiqueta de fromLevel y no la de toLevel
        composeTestRule.onNodeWithText(levelBadgeLabel(1)!!).assertExists()
        composeTestRule.onNodeWithText(levelBadgeLabel(2)!!).assertDoesNotExist()
    }

    // Scenario: Un acierto que certifica muestra el indicador (spec: practice-session)
    @Test
    fun givenLevelChangeWhenTransitionEndsThenToLevelIsShownAndFromLevelIsNot() {
        // Given: el reloj de composición está pausado, con el cambio de insignia pintado
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadgeChange(fromLevel = 1, toLevel = 2, emphasized = true)
            }
        }

        // When: transcurre más tiempo que Motion.durationMediumMillis
        composeTestRule.mainClock.advanceTimeBy(Motion().durationMediumMillis.toLong() + EXTRA_MILLIS)
        composeTestRule.waitForIdle()

        // Then: se ve la etiqueta de toLevel y ya no la de fromLevel
        composeTestRule.onNodeWithText(levelBadgeLabel(2)!!).assertExists()
        composeTestRule.onNodeWithText(levelBadgeLabel(1)!!).assertDoesNotExist()
    }
}
