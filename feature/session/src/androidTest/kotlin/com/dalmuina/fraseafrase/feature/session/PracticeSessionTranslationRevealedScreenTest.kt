package com.dalmuina.fraseafrase.feature.session

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Tests de UI del panel de traducción, los botones Fallo/Acierto y el botón Escuchar de
 * [PracticeSessionScreen] con la frase revelada (ADR-0007; spec: practice-session, phrase-audio).
 * Vive en una clase propia, separada de [PracticeSessionScreenTest], para no superar el límite de
 * tamaño de clase de detekt (mismo motivo que separa [PracticeSessionRevertButtonScreenTest];
 * adopt-reference-mock-ui, tareas 5.1-5.3). Comparte los builders de estado con
 * [PracticeSessionScreenTest] a través de `PracticeSessionScreenFixtures.kt`.
 */
class PracticeSessionTranslationRevealedScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        /** Tag del panel de traducción, ver `TranslationCard` (adopt-reference-mock-ui, tarea 5.1). */
        private const val TRANSLATION_CARD_TAG = "practice_translation_card"

        /** Tags de los botones de autoevaluación, ver `AssessmentButtons` (adopt-reference-mock-ui, tarea 5.2). */
        private const val MISS_BUTTON_TAG = "practice_miss_button"
        private const val HIT_BUTTON_TAG = "practice_hit_button"

        /** Tag del botón Escuchar, ver `ListenButton` (adopt-reference-mock-ui, tarea 5.3). */
        private const val LISTEN_BUTTON_TAG = "practice_listen_button"

        /** Lado del botón Escuchar circular (`design/mock/design-tokens.md`, "Botón Escuchar": 44×44). */
        private val LISTEN_BUTTON_SIZE = 44.dp

        /** Margen de redondeo de densidad al comparar tamaños exactos en dp (ver [LISTEN_BUTTON_SIZE]). */
        private val SIZE_TOLERANCE_DP = 1.dp

        /** Fracción de alto usada por [topEdgeBackgroundPixelColor] para no caer sobre icono o texto. */
        private const val TOP_EDGE_HEIGHT_FRACTION = 0.15f
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session); etiqueta
    // "TRADUCCIÓN" en mayúsculas sobre el panel de traducción (adopt-reference-mock-ui, design -
    // decisión 2, tarea 5.1)
    @Test
    fun givenRevealedPhraseWhenShowingTheScreenThenTranslationLabelIsDisplayed() {
        // Given: la pantalla muestra la frase A revelada
        composeTestRule.setScreen(practicing(current = item(), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: se muestra la etiqueta "TRADUCCIÓN" en mayúsculas sobre la traducción
        composeTestRule.onNodeWithText("TRADUCCIÓN").assertIsDisplayed()
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session); el panel usa
    // el fondo `accentSoft` del mock (adopt-reference-mock-ui, design - decisión 2, tarea 5.1)
    @Test
    fun givenRevealedPhraseWhenShowingTheScreenThenTranslationCardBackgroundIsAccentSoft() {
        // Given: la pantalla muestra la frase A revelada
        composeTestRule.setScreen(practicing(current = item(), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: el panel de traducción usa el color de fondo `accentSoft`
        val backgroundColor = centerPixelColor(TRANSLATION_CARD_TAG)
        assertEquals(AppColors.AccentSoft, backgroundColor)
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session); el botón
    // Fallo usa el fondo `errorBg` del mock (adopt-reference-mock-ui, design - decisión 2, tarea 5.2)
    @Test
    fun givenRevealedState_whenShowingTheScreen_thenMissButtonBackgroundIsErrorBg() {
        // Given: la pantalla muestra una frase revelada
        composeTestRule.setScreen(practicing(current = item(), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: el botón Fallo usa el color de fondo `errorBg`
        val backgroundColor = topEdgeBackgroundPixelColor(MISS_BUTTON_TAG)
        assertEquals(AppColors.ErrorBg, backgroundColor)
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session); el botón
    // Acierto usa el fondo `success` del mock (adopt-reference-mock-ui, design - decisión 2, tarea 5.2)
    @Test
    fun givenRevealedState_whenShowingTheScreen_thenHitButtonBackgroundIsSuccess() {
        // Given: la pantalla muestra una frase revelada
        composeTestRule.setScreen(practicing(current = item(), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: el botón Acierto usa el color de fondo `success`
        val backgroundColor = topEdgeBackgroundPixelColor(HIT_BUTTON_TAG)
        assertEquals(AppColors.Success, backgroundColor)
    }

    // Scenario: los de phrase-audio sobre el control icono-solo (adopt-reference-mock-ui, design -
    // decisión 2 y `design/mock/design-tokens.md` "Tamaños y espaciado": botón Escuchar 44x44
    // circular con fondo `accent`; tarea 5.3)
    @Test
    fun givenRevealedPhraseWhenShowingTheScreenThenListenButtonIsCircular44dpWithAccentBackground() {
        // Given: la pantalla muestra una frase revelada; se captura el color primario del tema actual
        var expectedBackground = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedBackground = MaterialTheme.colorScheme.primary
                PracticeSessionScreen(uiState = practicing(current = item(), revealed = true), onAction = {})
            }
        }

        // When: la pantalla se muestra (estado dado)

        // Then: el botón Escuchar mide 44x44 (con el margen de redondeo de densidad de la pantalla de
        // prueba) y usa el color de fondo `accent` (primary)
        val bounds = composeTestRule.onNodeWithTag(LISTEN_BUTTON_TAG).getUnclippedBoundsInRoot()
        val backgroundColor = centerPixelColor(LISTEN_BUTTON_TAG)
        assertTrue(abs((bounds.right - bounds.left - LISTEN_BUTTON_SIZE).value) <= SIZE_TOLERANCE_DP.value)
        assertTrue(abs((bounds.bottom - bounds.top - LISTEN_BUTTON_SIZE).value) <= SIZE_TOLERANCE_DP.value)
        assertEquals(expectedBackground, backgroundColor)
    }

    /** Color del píxel central del nodo con [tag], igual que `DsStatTileTest` en `:core:designsystem`. */
    private fun centerPixelColor(tag: String): Color {
        val pixelMap = composeTestRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixelMap[pixelMap.width / 2, pixelMap.height / 2]
    }

    /**
     * Color del píxel del nodo con [tag] cerca del borde superior, a la misma columna central que
     * [centerPixelColor]: para los botones Fallo/Acierto, cuyo icono y texto se centran como grupo
     * dentro del botón, el píxel exactamente central puede caer sobre el propio icono o una letra en
     * vez de sobre el fondo (tarea 5.2, adopt-reference-mock-ui).
     */
    private fun topEdgeBackgroundPixelColor(tag: String): Color {
        val pixelMap = composeTestRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixelMap[pixelMap.width / 2, (pixelMap.height * TOP_EDGE_HEIGHT_FRACTION).toInt()]
    }
}
