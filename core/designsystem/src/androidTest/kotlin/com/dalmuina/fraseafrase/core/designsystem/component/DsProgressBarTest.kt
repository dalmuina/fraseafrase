package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test

private const val PROGRESS_BAR_TEST_TAG = "ds-progress-bar"

/**
 * Tests de UI de [DsProgressBar] (ADR-0007, redesign-app-ui). Verifican que la proporción
 * [DsProgressBar.progress] se refleja en la semántica del indicador de progreso.
 */
class DsProgressBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: DsProgressBar renderiza la proporción progress recibida (design - Componentes
    // Ds* nuevos, tasks 5.3), caso parcial
    @Test
    fun givenPartialProgressWhenPaintingThenRangeInfoReflectsIt() {
        // Given/When: se pinta la barra con una proporción parcial
        composeTestRule.setContent {
            AppTheme {
                DsProgressBar(
                    progress = 0.4f,
                    modifier = Modifier.testTag(PROGRESS_BAR_TEST_TAG)
                )
            }
        }

        // Then: la semántica del indicador refleja esa proporción, de 0f a 1f
        composeTestRule.onNodeWithTag(PROGRESS_BAR_TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 0.4f, range = 0f..1f))
    }

    // Scenario: DsProgressBar renderiza la proporción progress recibida (design - Componentes
    // Ds* nuevos, tasks 5.3), caso completo
    @Test
    fun givenCompleteProgressWhenPaintingThenRangeInfoReflectsIt() {
        // Given/When: se pinta la barra con una proporción completa
        composeTestRule.setContent {
            AppTheme {
                DsProgressBar(
                    progress = 1f,
                    modifier = Modifier.testTag(PROGRESS_BAR_TEST_TAG)
                )
            }
        }

        // Then: la semántica del indicador refleja esa proporción, de 0f a 1f
        composeTestRule.onNodeWithTag(PROGRESS_BAR_TEST_TAG)
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 1f, range = 0f..1f))
    }
}
