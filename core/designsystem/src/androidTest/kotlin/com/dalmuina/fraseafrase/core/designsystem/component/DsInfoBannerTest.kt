package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
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

    // Scenario: dado un icono multicolor (como una insignia de rango) y `iconTint =
    // Color.Unspecified`, el componente no lo tiñe con `contentColor` y conserva sus propios
    // colores (tasks 12.7, design-tokens.md: "Insignias de rango... nunca con el tint por defecto
    // de Icon()")
    @Test
    fun givenUnspecifiedIconTintWhenPaintingThenIconKeepsItsOwnColor() {
        // Given: un icono sólido de un color fijo distinto del contentColor del tema y
        // `iconTint = Color.Unspecified`
        val iconOwnColor = Color(0xFF123456)
        composeTestRule.setContent {
            AppTheme {
                DsInfoBanner(
                    icon = solidColorIcon(iconOwnColor),
                    iconContentDescription = "Insignia de rango",
                    segments = listOf(DsInfoBannerSegment(text = "Texto")),
                    iconTint = Color.Unspecified,
                )
            }
        }

        // When: se captura el píxel central del icono (las coordenadas de los nodos están en dp;
        // `toPixelMap()` indexa en píxeles físicos, así que hay que escalar por la densidad)
        val iconBounds = composeTestRule.onNodeWithContentDescription("Insignia de rango")
            .getUnclippedBoundsInRoot()
        val density = composeTestRule.density.density
        val pixelMap = composeTestRule.onRoot().captureToImage().toPixelMap()
        val centerX = (((iconBounds.left + iconBounds.right) / 2).value * density).toInt()
        val centerY = (((iconBounds.top + iconBounds.bottom) / 2).value * density).toInt()
        val iconPixelColor = pixelMap[centerX, centerY]

        // Then: el píxel conserva el color propio del icono, no el `contentColor` del banner
        assertEquals(iconOwnColor, iconPixelColor)
    }
}

/** Icono de prueba totalmente relleno de [color], para verificar que no se tiñe. */
private fun solidColorIcon(color: Color): ImageVector =
    ImageVector.Builder(
        name = "SolidColorTestIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(fill = SolidColor(color)) {
        moveTo(0f, 0f)
        lineTo(24f, 0f)
        lineTo(24f, 24f)
        lineTo(0f, 24f)
        close()
    }.build()
