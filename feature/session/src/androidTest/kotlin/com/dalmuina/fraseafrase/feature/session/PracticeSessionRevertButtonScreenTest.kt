package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
         * Umbral de proporción de píxeles distintos para considerar dos iconos "el mismo" en
         * [givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonShowsAnUndoIcon]: deja margen sobre el
         * ~3 % de ruido de antialiasing medido con el mismo vector, muy por debajo del ~21 % medido con un
         * vector distinto (`ArrowBack`).
         */
        private const val SAME_ICON_DIFF_RATIO_TOLERANCE = 0.10f
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
    // fix-redesign-ui-mismatches), comprobado aquí a nivel de pantalla sobre el icono del botón: debe ser de
    // recarga/deshacer, no una flecha de "volver atrás". Compara los píxeles del icono realmente pintado
    // dentro del botón con los de un icono de referencia con el vector Undo esperado, pintado en el mismo
    // árbol con el mismo tinte, para distinguirlo de forma determinista de cualquier otro icono.
    @Test
    fun givenCanRevertLastAnswerTrue_whenScreenShown_thenRevertButtonShowsAnUndoIcon() {
        // Given: el botón de revertir visible, superpuesto a un icono de referencia con el vector Undo
        val expectedUndoIconTag = "expected_undo_icon_reference"
        composeTestRule.setContent {
            AppTheme {
                Box {
                    PracticeSessionScreen(
                        uiState = practicing(current = item()).copy(canRevertLastAnswer = true),
                        onAction = {},
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
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
}
