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

    // Scenario: El icono muestra el idioma activo en Progreso y Ajustes (language-indicator, en-GB)
    @Test
    fun givenEnGbInProgressAndSettingsWhenPaintingThenEnGbIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "en-GB")
            }
        }

        composeTestRule.onNodeWithContentDescription("en-GB").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, es-ES)
    @Test
    fun givenEsEsWhenPaintingThenEsEsIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "es-ES")
            }
        }

        composeTestRule.onNodeWithContentDescription("es-ES").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, it-IT)
    @Test
    fun givenItItWhenPaintingThenItItIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "it-IT")
            }
        }

        composeTestRule.onNodeWithContentDescription("it-IT").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, en-GB)
    @Test
    fun givenEnGbWhenPaintingThenEnGbIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "en-GB")
            }
        }

        composeTestRule.onNodeWithContentDescription("en-GB").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, de-DE)
    @Test
    fun givenDeDeWhenPaintingThenDeDeIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "de-DE")
            }
        }

        composeTestRule.onNodeWithContentDescription("de-DE").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, fr-FR)
    @Test
    fun givenFrFrWhenPaintingThenFrFrIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "fr-FR")
            }
        }

        composeTestRule.onNodeWithContentDescription("fr-FR").assertExists()
    }

    // Scenario: El icono muestra el idioma activo en Practicar (language-indicator, ru-RU)
    @Test
    fun givenRuRuWhenPaintingThenRuRuIconIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "ru-RU")
            }
        }

        composeTestRule.onNodeWithContentDescription("ru-RU").assertExists()
    }

    // Scenario: pt-PT y en-GB no comparten representación (language-indicator)
    @Test
    fun givenPtPtAndEnGbWhenPaintingThenIconsDiffer() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageBadge(languageCode = "pt-PT", modifier = Modifier.testTag("pt-pt-badge"))
                DsLanguageBadge(languageCode = "en-GB", modifier = Modifier.testTag("en-gb-badge"))
            }
        }

        val ptPtTag = composeTestRule.onNodeWithTag("pt-pt-badge")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
        val enGbTag = composeTestRule.onNodeWithTag("en-gb-badge")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]

        assertNotEquals(ptPtTag, enGbTag)
    }
}
