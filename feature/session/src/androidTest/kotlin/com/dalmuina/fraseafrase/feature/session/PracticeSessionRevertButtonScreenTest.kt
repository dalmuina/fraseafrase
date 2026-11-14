package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import com.dalmuina.fraseafrase.core.designsystem.R as DesignSystemR

/**
 * Tests de UI del botón de revertir de [PracticeSessionScreen] (ADR-0007; spec: revert-selection,
 * practice-session). Vive en una clase propia, separada de [PracticeSessionScreenTest], para no superar el
 * límite de tamaño de clase de detekt (mismo motivo que separa `PracticeSessionRevertTest` de
 * `PracticeSessionViewModelTest` en los tests unitarios). Comparte los builders de estado con
 * [PracticeSessionScreenTest] a través de `PracticeSessionScreenFixtures.kt`.
 */
class PracticeSessionRevertButtonScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        /** Tag del botón flotante para deshacer la última autoevaluación (spec: revert-selection). */
        private const val REVERT_LAST_ANSWER_BUTTON_TAG = "practice_revert_last_answer_button"

        /** Tag del icono dentro de [REVERT_LAST_ANSWER_BUTTON_TAG] (design D6, fix-redesign-ui-mismatches). */
        private const val REVERT_LAST_ANSWER_ICON_TAG = "practice_revert_last_answer_icon"

        /**
         * Tag de la caja de pantalla completa de [PracticeSessionScreen] (bajo el `topBar`, sin el padding
         * adicional de la zona de contenido), usada como referencia de los bordes de "la pantalla" en
         * [givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonIsOffsetFromScreenEdgesPerMock] en vez
         * de la raíz de la composición: la raíz incluye los insets del sistema (barra de gestos) que
         * `Scaffold` ya descuenta de su contenido, y que desplazarían la medida sin relación con los tokens
         * de posición del FAB.
         */
        private const val CONTENT_AREA_TAG = "practice_session_content_area"

        /**
         * Umbral de proporción de píxeles distintos para considerar dos iconos "el mismo" en
         * [givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonShowsAnUndoIcon]: deja margen sobre el
         * ~3 % de ruido de antialiasing medido con el mismo vector en el emulador de desarrollo, y hasta
         * ~12 % medido en el emulador de CI (API 34 sin Google APIs, antialiasing más agresivo), muy por
         * debajo del ~21 % medido con un vector distinto (`ArrowBack`) en cualquiera de los dos.
         */
        private const val SAME_ICON_DIFF_RATIO_TOLERANCE = 0.17f

        /**
         * Offset horizontal del FAB respecto al borde izquierdo de la pantalla (`design/mock/design-tokens.md`,
         * "Botón flotante de volver": 20dp desde la izquierda; adopt-reference-mock-ui, tarea 6.1). El offset
         * inferior ya no es un valor fijo del mock: pasa a ser `AppTheme.dimensions.navFloatingMargin`, la
         * distancia al borde superior de la barra de navegación flotante (bottom-bar-navigation, design D3),
         * leído dentro de [AppTheme] en el propio test en vez de duplicarse aquí como literal.
         */
        private val REVERT_FAB_LEFT_OFFSET = 20.dp

        /** Margen de redondeo de densidad al comparar posiciones exactas en dp (ver [REVERT_FAB_LEFT_OFFSET]). */
        private val OFFSET_TOLERANCE_DP = 1.dp
    }

    // Scenario: El botón aparece tras autoevaluar (spec: practice-session)
    @Test
    fun givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonIsDisplayedAndClickable() {
        // Given: una tanda activa con una autoevaluación reciente que se puede revertir
        var receivedAction: PracticeSessionAction? = null
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = practicing(current = item()).copy(canRevertLastAnswer = true),
                    onAction = { action -> receivedAction = action },
                )
            }
        }

        // When: el usuario pulsa el botón de revertir, en la esquina inferior izquierda
        composeTestRule.onNodeWithTag(REVERT_LAST_ANSWER_BUTTON_TAG).apply {
            assertIsDisplayed()
            performClick()
        }

        // Then: se despacha RevertLastAnswer
        assertTrue(receivedAction == PracticeSessionAction.RevertLastAnswer)
    }

    // Scenario: El botón desaparece al revelar la frase siguiente (spec: practice-session; design D6,
    // fix-redesign-ui-mismatches; icono y color de contenido ajustados al mock por adopt-reference-mock-ui,
    // tarea 6.1), comprobado aquí a nivel de pantalla sobre el icono del botón: debe ser `ic_undo_previous`
    // con el color de contenido `fabContent`, no un icono o tinte distintos. Compara los píxeles del icono
    // realmente pintado dentro del botón con los de un icono de referencia con el mismo vector y el mismo
    // tinte, pintado en el mismo árbol, para distinguirlo de forma determinista de cualquier otro icono o
    // color.
    @Test
    fun givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonShowsAnUndoIcon() {
        // Given: el botón de revertir visible, superpuesto a un icono de referencia con el vector e icono
        // esperados (`ic_undo_previous`, color `fabContent`)
        val expectedUndoIconTag = "expected_undo_icon_reference"
        composeTestRule.setContent {
            AppTheme {
                Box {
                    PracticeSessionScreen(
                        uiState = practicing(current = item()).copy(canRevertLastAnswer = true),
                        onAction = {},
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_undo_previous),
                        contentDescription = null,
                        tint = AppColors.FabContent,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            // Mismo fondo que el `FloatingActionButton` (containerColor = secondary) detrás del
                            // icono real: la captura incluye lo compuesto detrás del icono, no solo su propio
                            // trazo, así que sin este fondo los píxeles de los dos iconos nunca coincidirían.
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag(expectedUndoIconTag),
                    )
                }
            }
        }

        // When: se capturan los píxeles del icono del botón y los del icono de referencia
        val actualIconPixels = composeTestRule.onNodeWithTag(REVERT_LAST_ANSWER_ICON_TAG, useUnmergedTree = true)
            .captureToImage()
            .toPixelMap()
        val expectedIconPixels = composeTestRule.onNodeWithTag(expectedUndoIconTag)
            .captureToImage()
            .toPixelMap()

        // Then: son el mismo icono. Se admite un margen de ruido de antialiasing entre el render dentro del
        // `FloatingActionButton` y el de referencia (empíricamente ~3 % de los píxeles con el mismo vector,
        // frente a ~21 % con un vector distinto, p. ej. ArrowBack): bastante margen para distinguir un icono
        // distinto sin que la prueba sea inestable por diferencias de antialiasing.
        assertEquals(expectedIconPixels.width, actualIconPixels.width)
        assertEquals(expectedIconPixels.height, actualIconPixels.height)
        val totalPixels = expectedIconPixels.buffer.size
        val diffPixels = expectedIconPixels.buffer.indices.count {
            expectedIconPixels.buffer[it] != actualIconPixels.buffer[it]
        }
        val diffRatio = diffPixels.toFloat() / totalPixels
        assertTrue(
            "diffRatio=$diffRatio ($diffPixels of $totalPixels), expected below $SAME_ICON_DIFF_RATIO_TOLERANCE",
            diffRatio < SAME_ICON_DIFF_RATIO_TOLERANCE,
        )
    }

    // Scenario: El botón no aparece antes de la primera autoevaluación / no aparece mientras se registra una
    // nueva autoevaluación (spec: practice-session)
    @Test
    fun givenCanRevertLastAnswerFalse_whenScreenShown_thenRevertButtonDoesNotExist() {
        // Given: no hay ninguna autoevaluación pendiente de revertir
        composeTestRule.setScreen(practicing(current = item()).copy(canRevertLastAnswer = false))

        // When: la pantalla se muestra (estado dado)

        // Then: el botón de revertir no existe en el árbol semántico
        composeTestRule.onNodeWithTag(REVERT_LAST_ANSWER_BUTTON_TAG).assertDoesNotExist()
    }

    // Sin escenario de spec propio (adopt-reference-mock-ui, tarea 6.1; offset inferior redefinido por
    // bottom-bar-navigation, design D3): verifica la posición exacta del FAB — 20dp desde el borde
    // izquierdo de la pantalla (`design/mock/design-tokens.md`) y `AppTheme.dimensions.navFloatingMargin`
    // desde el borde superior de la barra de navegación flotante —, no ninguna condición de visibilidad
    // ya cubierta por los tests anteriores.
    @Test
    fun givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonIsOffsetFromScreenEdgesPerMock() {
        // Given: el botón de revertir visible
        var expectedBottomOffset = 0.dp
        composeTestRule.setContent {
            AppTheme {
                expectedBottomOffset = AppTheme.dimensions.navFloatingMargin
                PracticeSessionScreen(
                    uiState = practicing(current = item()).copy(canRevertLastAnswer = true),
                    onAction = {},
                )
            }
        }

        // When: se miden los límites del botón y los de la caja de pantalla completa (ver [CONTENT_AREA_TAG])
        val buttonBounds = composeTestRule.onNodeWithTag(REVERT_LAST_ANSWER_BUTTON_TAG).getUnclippedBoundsInRoot()
        val contentAreaBounds = composeTestRule.onNodeWithTag(CONTENT_AREA_TAG).getUnclippedBoundsInRoot()

        // Then: está a 20dp del borde izquierdo y a `navFloatingMargin` del borde inferior de la pantalla
        assertTrue(
            "left=${buttonBounds.left}, expected ~$REVERT_FAB_LEFT_OFFSET",
            abs((buttonBounds.left - REVERT_FAB_LEFT_OFFSET).value) <= OFFSET_TOLERANCE_DP.value,
        )
        val bottomOffset = contentAreaBounds.bottom - buttonBounds.bottom
        assertTrue(
            "bottomOffset=$bottomOffset, expected ~$expectedBottomOffset",
            abs((bottomOffset - expectedBottomOffset).value) <= OFFSET_TOLERANCE_DP.value,
        )
    }
}
