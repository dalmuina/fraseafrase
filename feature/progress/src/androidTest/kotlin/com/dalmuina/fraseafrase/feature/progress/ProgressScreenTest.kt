package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
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
        composeTestRule.onNodeWithText("Sin rango: 6 frases").assertIsDisplayed()
    }

    // Scenario: Catálogo ilegible al abrir el progreso (spec: progress-overview)
    @Test
    fun givenAnUnreadableCatalog_whenShowingTheScreen_thenAnErrorIsDisplayedWithoutAnyNumber() {
        // Given/When: el catálogo no se pudo leer
        setScreen(ProgressUiState.CatalogError)

        // Then: se muestra un error y no se muestra ningún nivel con número
        composeTestRule.onNodeWithText("El progreso no está disponible").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Sin rango: 0 frases").assertCountEquals(0)
    }

    // Scenario: Catálogo con frases en varios niveles (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesInLevelThree_whenShowingTheScreen_thenTheGoldBadgeIsDisplayed() {
        // Given/When: el nivel 3 tiene 3 frases
        setScreen(
            ProgressUiState.Content(
                certifiedInBatch = null,
                byLevel = (0..Level.MAX_LEVEL).associate { value ->
                    Level(value) to if (value == 3) 3 else 0
                },
            ),
        )

        // Then: se muestra la insignia "Oro" junto al número de frases del nivel 3, sin el número de
        // nivel como texto (spec `progress-overview`, requisito "Total de frases por nivel")
        composeTestRule.onNodeWithText("3 frases").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oro").assertIsDisplayed()
    }

    // Scenario: La fila de nivel 0 no muestra insignia (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesInLevelZero_whenShowingTheScreen_thenNoBadgeIsDisplayedForThatRow() {
        // Given/When: el catálogo tiene frases en nivel 0
        setScreen(
            ProgressUiState.Content(
                certifiedInBatch = null,
                byLevel = allLevels(levelZero = 6),
            ),
        )

        // Then: la fila de nivel 0 muestra su número de frases sin ninguna insignia. Los niveles 1 a
        // 5 siempre muestran su propia insignia (spec `level-badges`), así que la comprobación se
        // limita a los hermanos del texto de nivel 0 dentro de su fila, no a toda la pantalla.
        composeTestRule.onNodeWithText("Sin rango: 6 frases").assertIsDisplayed()
        composeTestRule.onNodeWithTag("progress_level_row_0").onChildren().assertCountEquals(1)
    }

    // Scenario: La fila de nivel 0 no muestra insignia ni número de nivel (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesInEveryLevel_whenShowingTheScreen_thenNoRowShowsTheWordNivel() {
        // Given/When: el catálogo tiene frases repartidas entre varios niveles
        setScreen(
            ProgressUiState.Content(
                certifiedInBatch = null,
                byLevel = (0..Level.MAX_LEVEL).associate { value -> Level(value) to value },
            ),
        )

        // Then: ninguna fila muestra el texto "Nivel" seguido de un número; cada nivel se identifica
        // solo por su insignia (o "Sin rango" en nivel 0)
        composeTestRule.onAllNodesWithText("Nivel", substring = true).assertCountEquals(0)
    }

    private fun setScreen(uiState: ProgressUiState) {
        composeTestRule.setContent {
            AppTheme {
                ProgressScreen(uiState = uiState)
            }
        }
    }
}
