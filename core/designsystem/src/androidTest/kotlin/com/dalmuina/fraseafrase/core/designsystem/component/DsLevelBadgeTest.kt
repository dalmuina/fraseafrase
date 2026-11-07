package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsLevelBadge] (ADR-0007). Verifican lo que ve el usuario: etiqueta y presencia o
 * ausencia de icono por nivel.
 */
class DsLevelBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: Nivel 0 no muestra insignia (ahora muestra "Sin rango", spec: level-badges)
    @Test
    fun givenLevel0WhenPaintingThenSinRangoIsShown() {
        // Given/When: se pinta la insignia de nivel 0
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 0)
            }
        }

        // Then: se muestra la etiqueta "Sin rango" y ninguna otra
        composeTestRule.onNodeWithText("Sin rango").assertExists()
        composeTestRule.onNodeWithText("Bronce").assertDoesNotExist()
        composeTestRule.onNodeWithText("Plata").assertDoesNotExist()
        composeTestRule.onNodeWithText("Oro").assertDoesNotExist()
        composeTestRule.onNodeWithText("Platino").assertDoesNotExist()
        composeTestRule.onNodeWithText("Diamante").assertDoesNotExist()
    }

    // Scenario: Nivel 1 muestra Bronce (spec: level-badges)
    @Test
    fun givenLevel1WhenPaintingThenBronceIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 1)
            }
        }

        composeTestRule.onNodeWithText("Bronce").assertExists()
    }

    // Scenario: Nivel 2 muestra Plata (spec: level-badges)
    @Test
    fun givenLevel2WhenPaintingThenPlataIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 2)
            }
        }

        composeTestRule.onNodeWithText("Plata").assertExists()
    }

    // Scenario: Nivel 3 muestra Oro (spec: level-badges)
    @Test
    fun givenLevel3WhenPaintingThenOroIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 3)
            }
        }

        composeTestRule.onNodeWithText("Oro").assertExists()
    }

    // Scenario: Nivel 4 muestra Platino (spec: level-badges)
    @Test
    fun givenLevel4WhenPaintingThenPlatinoIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 4)
            }
        }

        composeTestRule.onNodeWithText("Platino").assertExists()
    }

    // Scenario: Nivel 5 muestra Diamante (spec: level-badges)
    @Test
    fun givenLevel5WhenPaintingThenDiamanteIsShown() {
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 5)
            }
        }

        composeTestRule.onNodeWithText("Diamante").assertExists()
    }

    // Scenario: Oro y Plata no comparten representación (spec: level-badges)
    @Test
    fun givenLevel2AndLevel3WhenPaintingThenIconsDiffer() {
        // Given/When: se pintan las insignias de nivel 2 (Plata) y nivel 3 (Oro)
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 2, modifier = Modifier.testTag("silver"))
                DsLevelBadge(level = 3, modifier = Modifier.testTag("gold"))
            }
        }

        // Then: sus iconos usan un identificador de icono distinto entre sí
        val silverIconTag = composeTestRule.onNodeWithTag("silver")
            .onChildren()
            .onFirst()
            .fetchSemanticsNode()
            .config[SemanticsProperties.TestTag]
        val goldIconTag = composeTestRule.onNodeWithTag("gold")
            .onChildren()
            .onFirst()
            .fetchSemanticsNode()
            .config[SemanticsProperties.TestTag]

        assertNotEquals(silverIconTag, goldIconTag)
    }

    // Scenario: Sin rango y Bronce no comparten representación (spec: level-badges)
    @Test
    fun givenLevel0AndLevel1WhenPaintingThenIconsDiffer() {
        // Given/When: se pintan las insignias de nivel 0 (Sin rango) y nivel 1 (Bronce)
        composeTestRule.setContent {
            AppTheme {
                DsLevelBadge(level = 0, modifier = Modifier.testTag("sin-rango"))
                DsLevelBadge(level = 1, modifier = Modifier.testTag("bronce"))
            }
        }

        // Then: sus iconos usan un identificador de icono distinto entre sí
        val sinRangoIconTag = composeTestRule.onNodeWithTag("sin-rango")
            .onChildren()
            .onFirst()
            .fetchSemanticsNode()
            .config[SemanticsProperties.TestTag]
        val bronceIconTag = composeTestRule.onNodeWithTag("bronce")
            .onChildren()
            .onFirst()
            .fetchSemanticsNode()
            .config[SemanticsProperties.TestTag]

        assertNotEquals(sinRangoIconTag, bronceIconTag)
    }
}
