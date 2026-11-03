package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [ProgressScreen] (ADR-0007). La pantalla es sin estado, así que cada test construye el estado a
 * mano, sin ViewModel.
 */
class ProgressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun allLevels(levelZero: Int) = (0..Level.MAX_LEVEL).associate { value ->
        Level(value) to if (value == 0) levelZero else 0
    }

    // Scenario: Tanda con certificadas y no certificadas (spec: progress-overview)
    @Test
    fun givenABatchWithCertifiedAndUncertifiedPhrases_whenShowingTheScreen_thenTheCertifiedCountIsDisplayed() {
        // Given/When: la pantalla muestra 2 de 5 frases certificadas
        setScreen(
            ProgressUiState.Content(
                certifiedInBatch = CertifiedCount(certified = 2, total = 5),
                byLevel = allLevels(levelZero = 3),
            ),
        )

        // Then: se muestra el número de certificadas sobre el total de la tanda
        composeTestRule.onNodeWithText("2 de 5 frases certificadas").assertIsDisplayed()
    }

    // Scenario: Sin tanda activa muestra solo el total por nivel (spec: progress-overview)
    @Test
    fun givenNoActiveBatch_whenShowingTheScreen_thenItIndicatesNoActiveBatchAndShowsTheTotalByLevel() {
        // Given/When: no hay tanda activa y el catálogo tiene 6 frases en el nivel 0
        setScreen(
            ProgressUiState.Content(
                certifiedInBatch = null,
                byLevel = allLevels(levelZero = 6),
            ),
        )

        // Then: se indica que no hay tanda activa y se muestra el total del nivel 0
        composeTestRule.onNodeWithText("No hay tanda activa").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nivel 0: 6 frases").assertIsDisplayed()
    }

    // Scenario: Catálogo ilegible al abrir el progreso (spec: progress-overview)
    @Test
    fun givenAnUnreadableCatalog_whenShowingTheScreen_thenAnErrorIsDisplayedWithoutAnyNumber() {
        // Given/When: el catálogo no se pudo leer
        setScreen(ProgressUiState.CatalogError)

        // Then: se muestra un error y no se muestra ningún nivel con número
        composeTestRule.onNodeWithText("El progreso no está disponible").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Nivel 0: 0 frases").assertCountEquals(0)
    }

    private fun setScreen(uiState: ProgressUiState) {
        composeTestRule.setContent {
            AppTheme {
                ProgressScreen(uiState = uiState)
            }
        }
    }
}
