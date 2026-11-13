package com.dalmuina.fraseafrase.feature.session

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI del mensaje de tanda completada de [PracticeSessionScreen] (ADR-0007; spec:
 * practice-session). Vive en una clase propia, separada de [PracticeSessionScreenTest], para no superar
 * el límite de tamaño de clase de detekt (mismo motivo que separa [PracticeSessionRevertButtonScreenTest]
 * de [PracticeSessionScreenTest]). Comparte los builders de estado con [PracticeSessionScreenTest] a
 * través de `PracticeSessionScreenFixtures.kt`.
 */
class PracticeSessionCompletedScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        /** Tags de las tres tarjetas de resultado de [CompletedMessage] (design D4, fix-redesign-ui-mismatches). */
        private const val HITS_TILE_TAG = "practice_outcome_hits_tile"
        private const val MISSES_TILE_TAG = "practice_outcome_misses_tile"
        private const val LEVEL_UPS_TILE_TAG = "practice_outcome_level_ups_tile"
    }

    // Scenario: Tanda completa muestra el mensaje positivo (spec: practice-session)
    @Test
    fun givenCompletedBatchWhenShowingTheScreenThenThePositiveMessageIsDisplayedInsteadOfAPhrase() {
        // Given: la tanda activa está completa
        composeTestRule.setScreen(practicing(current = item(), completed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: se muestra el mensaje positivo con la opción de generar sesión, no la frase
        composeTestRule.onNodeWithText("¡Has completado la sesión!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Generar sesión nueva").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buenos días").assertDoesNotExist()
    }

    // Scenario: Generar sesión muestra la tanda siguiente con sus arrastradas primero (spec: practice-session)
    @Test
    fun givenCompletedBatchWhenGeneratingSessionThenOnActionReceivesGenerateSession() {
        // Given: la tanda activa está completa, con el mensaje positivo visible
        var receivedAction: PracticeSessionAction? = null
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = practicing(current = item(), completed = true),
                    onAction = { action -> receivedAction = action },
                )
            }
        }

        // When: el usuario pide generar sesión
        composeTestRule.onNodeWithText("Generar sesión nueva").performClick()

        // Then: se despacha GenerateSession
        assertTrue(receivedAction == PracticeSessionAction.GenerateSession)
    }

    // Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel (spec: practice-session)
    @Test
    fun givenCompletedBatchWithOutcome_whenShowingTheScreen_thenTheBreakdownStatTilesAndSuccessIconAreDisplayed() {
        // Given: la tanda completada con 3 aciertos, 2 fallos y 1 subida de nivel
        composeTestRule.setScreen(
            practicing(current = item(), completed = true).copy(hits = 3, misses = 2, levelUps = 1),
        )

        // When: la pantalla se muestra (estado dado)

        // Then: se muestran el icono de éxito y las tres tarjetas de estadística con sus valores y etiquetas
        composeTestRule.onNodeWithTag("practice_completed_success_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aciertos").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fallos").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suben de rango").assertIsDisplayed()
    }

    // Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel (spec:
    // practice-session), con colores diferenciados y la misma altura entre las tres tarjetas,
    // aunque una etiqueta ocupe más líneas que otra (design D4, fix-redesign-ui-mismatches)
    @Test
    fun givenCompletedBatchWithOutcome_whenShowingTheScreen_thenOutcomeTilesHaveDistinctColorsAndEqualHeight() {
        // Given: la tanda completada; se capturan los colores de contenedor esperados del tema
        // actual para aciertos, fallos y subidas de rango, en ese orden
        var expectedColors = emptyList<Color>()
        composeTestRule.setContent {
            AppTheme {
                val scheme = MaterialTheme.colorScheme
                expectedColors = listOf(scheme.tertiaryContainer, scheme.errorContainer, scheme.secondaryContainer)
                PracticeSessionScreen(
                    uiState = practicing(current = item(), completed = true).copy(hits = 3, misses = 2, levelUps = 1),
                    onAction = {},
                )
            }
        }
        val tags = listOf(HITS_TILE_TAG, MISSES_TILE_TAG, LEVEL_UPS_TILE_TAG)

        // When: la pantalla se muestra (estado dado)

        // Then: cada tarjeta usa el color de fondo esperado para su rol, distinto del de las otras
        // dos, y las tres miden lo mismo aunque "Suben de rango" ocupe más líneas que las otras
        val actualColors = tags.map { outcomeTileBackgroundColor(it) }
        val heights = tags.map { composeTestRule.onNodeWithTag(it).getUnclippedBoundsInRoot().heightValue() }
        assertEquals(expectedColors, actualColors)
        assertEquals(actualColors.size, actualColors.toSet().size)
        assertTrue(heights.all { it == heights.first() })
    }

    // Scenario: Mensaje de tanda completada (spec: practice-session), con la insignia compuesta de
    // 4 capas del mock (adopt-reference-mock-ui, design - decisión 6): no solo un icono simple de
    // acierto, sino también su mini-hexágono decorativo y el check centrado sobre ambos hexágonos.
    @Test
    fun givenCompletedBatch_whenShowingTheScreen_thenTheCompositeBadgeWithMiniHexAndCenteredCheckIsDisplayed() {
        // Given: la tanda activa está completa
        composeTestRule.setScreen(practicing(current = item(), completed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: se muestran el check centrado y el mini-hexágono decorativo de la insignia compuesta
        composeTestRule.onNodeWithTag("practice_completed_success_icon", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("practice_completed_badge_mini_hex", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // Scenario: El mensaje ofrece ir a Progreso sin generar sesión (spec: practice-session)
    @Test
    fun givenCompletedBatchWhenActivatingViewProgressLinkThenOnNavigateToProgressIsInvokedWithoutGeneratingSession() {
        // Given: la pantalla muestra el mensaje de tanda completada
        var receivedAction: PracticeSessionAction? = null
        var navigatedToProgress = false
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = practicing(current = item(), completed = true),
                    onAction = { action -> receivedAction = action },
                    onNavigateToProgress = { navigatedToProgress = true },
                )
            }
        }

        // When: el usuario activa el enlace a Progreso
        composeTestRule.onNodeWithText("Ver mi progreso").performClick()

        // Then: se navega a Progreso sin despachar ninguna acción de generar sesión ni cambiar la tanda activa
        assertTrue(navigatedToProgress)
        assertTrue(receivedAction == null)
    }

    // Scenario: Mientras hay una respuesta en curso pedir generar sesión no tiene efecto (spec: practice-session)
    @Test
    fun givenAnswerInProgressWhenShowingTheScreenThenTheGenerateSessionButtonIsDisabled() {
        // Given: la tanda está completa y, además, una autoevaluación se está registrando (busy = true)
        composeTestRule.setScreen(practicing(current = item(), busy = true, completed = true))

        // When: la pantalla muestra el estado con la respuesta en curso (estado dado)

        // Then: el botón de generar sesión está deshabilitado
        composeTestRule.onNodeWithText("Generar sesión nueva").assertIsNotEnabled()
    }

    // Scenario: Mensaje de tanda completada (spec: practice-session), con icono junto al texto del
    // botón de generar sesión (design D8, fix-redesign-ui-mismatches)
    @Test
    fun givenCompletedBatch_whenShowingTheScreen_thenGenerateSessionButtonShowsIconNextToText() {
        // Given: la tanda activa está completa
        composeTestRule.setScreen(practicing(current = item(), completed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: el botón "Generar sesión nueva" muestra un icono junto al texto
        composeTestRule.onNodeWithTag("practice_generate_session_icon", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Generar sesión nueva").assertIsDisplayed()
    }

    /** Alto de un [DpRect], no expuesto como propiedad por la API de Compose UI. */
    private fun DpRect.heightValue(): Dp = bottom - top

    /** Color del píxel central del nodo con [tag], igual que `DsStatTileTest` en `:core:designsystem`. */
    private fun outcomeTileBackgroundColor(tag: String): Color {
        val pixelMap = composeTestRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixelMap[pixelMap.width / 2, pixelMap.height / 2]
    }
}
