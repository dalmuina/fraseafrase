package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsInfoBanner] (ADR-0007, fix-redesign-ui-mismatches, design D2). Verifican que
 * el icono, el texto completo (con sus tramos en negrita) y el color de fondo indicado son los que
 * ve el usuario.
 */
class DsInfoBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: dado un icono, un texto con tramos en negrita y un color de contenedor, el
    // componente pinta el icono, el texto completo y respeta el color de fondo indicado
    // (design D2, tasks 2.1)
    @Test
    fun givenIconBoldSegmentsAndContainerColorWhenPaintingThenIconTextAndColorAreShown() {
        // Given: un icono con descripción accesible, un texto con dos tramos en negrita y el color
        // de fondo secondaryContainer del tema actual
        var expectedColor: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedColor = MaterialTheme.colorScheme.secondaryContainer
                // When: se pinta el banner con ese icono, esos tramos y ese color de fondo
                DsInfoBanner(
                    icon = Icons.Default.MilitaryTech,
                    iconContentDescription = "Insignia del siguiente nivel",
                    segments = listOf(
                        DsInfoBannerSegment(text = "Te faltan "),
                        DsInfoBannerSegment(text = "3 aciertos", emphasized = true),
                        DsInfoBannerSegment(text = " para la insignia "),
                        DsInfoBannerSegment(text = "Plata", emphasized = true),
                    ),
                    containerColor = expectedColor,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        // Then: el icono y el texto completo son legibles por el árbol semántico
        composeTestRule.onNodeWithContentDescription("Insignia del siguiente nivel").assertExists()
        composeTestRule.onNodeWithText("Te faltan 3 aciertos para la insignia Plata").assertExists()

        // Then: el fondo pintado coincide con el color de contenedor indicado (se muestrea el
        // borde superior central, dentro del padding y lejos de las esquinas redondeadas y del
        // icono/texto)
        val pixelMap = composeTestRule.onRoot().captureToImage().toPixelMap()
        val backgroundColor = pixelMap[pixelMap.width / 2, 2]
        assertEquals(expectedColor, backgroundColor)
    }
}
