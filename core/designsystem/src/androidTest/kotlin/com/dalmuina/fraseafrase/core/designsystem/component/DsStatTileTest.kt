package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
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

    // Scenario: sin indicar color, DsStatTile sigue usando primaryContainer, por compatibilidad
    // con los usos existentes (fix-redesign-ui-mismatches, design D4, tasks 1.3/1.4)
    @Test
    fun givenNoContainerColorWhenPaintingThenPrimaryContainerIsUsed() {
        // Given: se captura el color primaryContainer esperado del tema actual
        var expectedColor: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedColor = MaterialTheme.colorScheme.primaryContainer
                // When: se pinta la tarjeta sin indicar un color de contenedor
                DsStatTile(value = "12", label = "Aciertos", size = DsStatTileSize.Compact)
            }
        }

        // Then: el fondo del Surface resultante coincide con primaryContainer
        val pixelMap = composeTestRule.onRoot().captureToImage().toPixelMap()
        val centerColor = pixelMap[pixelMap.width / 2, pixelMap.height / 2]
        assertEquals(expectedColor, centerColor)
    }

    // Scenario: indicando tertiaryContainer, el Surface resultante usa ese color
    // (fix-redesign-ui-mismatches, design D4, tasks 1.3/1.4)
    @Test
    fun givenTertiaryContainerColorWhenPaintingThenSurfaceUsesThatColor() {
        // Given: se captura el color tertiaryContainer esperado del tema actual
        var expectedColor: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedColor = MaterialTheme.colorScheme.tertiaryContainer
                // When: se pinta la tarjeta indicando tertiaryContainer/onTertiaryContainer
                DsStatTile(
                    value = "12",
                    label = "Aciertos",
                    size = DsStatTileSize.Compact,
                    containerColor = expectedColor,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Then: el fondo del Surface resultante coincide con tertiaryContainer
        val pixelMap = composeTestRule.onRoot().captureToImage().toPixelMap()
        val centerColor = pixelMap[pixelMap.width / 2, pixelMap.height / 2]
        assertEquals(expectedColor, centerColor)
    }
}
