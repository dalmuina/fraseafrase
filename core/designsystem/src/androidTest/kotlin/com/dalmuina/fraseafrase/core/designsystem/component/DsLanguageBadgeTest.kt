package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsLanguageBadge] (ADR-0007, add-multi-language-catalog). Verifican que el
 * icono circular mostrado identifica el idioma destino indicado por su código, sin depender de
 * `LanguageTag` (`:core:domain`), según ADR-0010.
 */
class DsLanguageBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, pt-PT)
    @Test
    fun givenPtPtWhenPaintingThenPtPtIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "pt-PT")
            }
        }

        composeTestRule.onNodeWithContentDescription("pt-PT").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Progreso y Ajustes (language-indicator, pt-BR)
    @Test
    fun givenPtBrWhenPaintingThenPtBrIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "pt-BR")
            }
        }

        composeTestRule.onNodeWithContentDescription("pt-BR").assertExists()
    }

    // Scenario: pt-PT y pt-BR no comparten representación (language-indicator)
    @Test
    fun givenPtPtAndPtBrWhenPaintingThenIconsDiffer() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "pt-PT", modifier = Modifier.testTag("pt-pt-badge"))
                DsLanguageBadge(languageCode = "pt-BR", modifier = Modifier.testTag("pt-br-badge"))
            }
        }

        val ptPtTag = composeTestRule.onNodeWithTag("pt-pt-badge")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
        val ptBrTag = composeTestRule.onNodeWithTag("pt-br-badge")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]

        assertNotEquals(ptPtTag, ptBrTag)
    }
}
