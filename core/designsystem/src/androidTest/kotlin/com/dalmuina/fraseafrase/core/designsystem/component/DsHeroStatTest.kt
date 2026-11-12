package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsHeroStat] (ADR-0007, fix-redesign-ui-mismatches, design D3). Verifican que el
 * valor, la etiqueta y la línea secundaria quedan alineados a la izquierda sobre el color de
 * fondo indicado.
 */
class DsHeroStatTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: dados un valor, una etiqueta, una línea secundaria y un color de fondo, el
    // componente los pinta alineados a la izquierda con ese fondo (design D3, tasks 3.1)
    @Test
    fun givenValueLabelSecondaryLineAndContainerColorWhenPaintingThenContentIsLeftAlignedWithThatBackground() {
        // Given: un valor, una etiqueta y una línea secundaria, con el color primary del tema
        // actual como fondo
        var expectedColor: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedColor = MaterialTheme.colorScheme.primary
                // When: se pinta la tarjeta destacada con ese valor, etiqueta, línea secundaria
                // y color de fondo
                DsHeroStat(
                    value = "128",
                    label = "Frases disponibles",
                    secondaryLine = "Nivel actual: B1",
                    containerColor = expectedColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        // Then: el valor, la etiqueta y la línea secundaria son legibles por el árbol semántico
        composeTestRule.onNodeWithText("128").assertExists()
        composeTestRule.onNodeWithText("Frases disponibles").assertExists()
        composeTestRule.onNodeWithText("Nivel actual: B1").assertExists()

        // Then: las tres líneas comparten el mismo borde izquierdo (alineación a la izquierda)
        val valueLeft = composeTestRule.onNodeWithText("128").getUnclippedBoundsInRoot().left
        val labelLeft = composeTestRule.onNodeWithText("Frases disponibles")
            .getUnclippedBoundsInRoot().left
        val secondaryLeft = composeTestRule.onNodeWithText("Nivel actual: B1")
            .getUnclippedBoundsInRoot().left
        assertEquals(valueLeft, labelLeft)
        assertEquals(valueLeft, secondaryLeft)

        // Then: el fondo pintado coincide con el color de contenedor indicado (se muestrea el
        // borde izquierdo a media altura, lejos de las esquinas redondeadas)
        val pixelMap = composeTestRule.onRoot().captureToImage().toPixelMap()
        val backgroundColor = pixelMap[2, pixelMap.height / 2]
        assertEquals(expectedColor, backgroundColor)
    }
}
