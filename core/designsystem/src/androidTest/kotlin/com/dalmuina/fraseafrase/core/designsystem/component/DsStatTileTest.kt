package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsStatTile] (ADR-0007, redesign-app-ui). Verifican que el valor y la etiqueta
 * recibidos son legibles por el árbol semántico de Compose, en ambas variantes de tamaño.
 */
class DsStatTileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: DsStatTile renderiza el valor y la etiqueta recibidos, en variante compact
    // (design - Componentes Ds* nuevos, tasks 5.1)
    @Test
    fun givenCompactSizeWhenPaintingThenValueAndLabelAreShown() {
        // Given/When: se pinta la tarjeta de recuento en variante Compact (valor por defecto)
        composeTestRule.setContent {
            AppTheme {
                DsStatTile(value = "12", label = "Aciertos", size = DsStatTileSize.Compact)
            }
        }

        // Then: el valor y la etiqueta son legibles por el árbol semántico
        composeTestRule.onNodeWithText("12").assertExists()
        composeTestRule.onNodeWithText("Aciertos").assertExists()
    }

    // Scenario: DsStatTile renderiza el valor y la etiqueta recibidos, en variante large
    // (design - Componentes Ds* nuevos, tasks 5.1)
    @Test
    fun givenLargeSizeWhenPaintingThenValueAndLabelAreShown() {
        // Given/When: se pinta la tarjeta de recuento en variante Large
        composeTestRule.setContent {
            AppTheme {
                DsStatTile(value = "248", label = "Frases disponibles", size = DsStatTileSize.Large)
            }
        }

        // Then: el valor y la etiqueta son legibles por el árbol semántico
        composeTestRule.onNodeWithText("248").assertExists()
        composeTestRule.onNodeWithText("Frases disponibles").assertExists()
    }
}
