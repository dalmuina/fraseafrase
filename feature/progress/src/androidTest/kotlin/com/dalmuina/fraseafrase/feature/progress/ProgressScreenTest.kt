package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
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

    // Scenario: El total coincide con la suma de los niveles (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesInSeveralLevels_whenShowingTheScreen_thenTheFeaturedCardShowsTheTotalAndTheCefrLevel() {
        // Given/When: la tarjeta destacada muestra 10 frases disponibles y el nivel CEFR B1
        setScreen(
            ProgressUiState.Content(
                totalAvailable = 10,
                cefrLevel = CefrLevel.B1,
                byLevel = allLevels(levelZero = 6) + mapOf(Level(2) to 3, Level(5) to 1),
            ),
        )

        // Then: se muestran el total disponible y el nivel CEFR vigente
        composeTestRule.onNodeWithText("10 frases disponibles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nivel actual: B1").assertIsDisplayed()
    }

    // Scenario: Sin tanda activa muestra solo el total por nivel (spec: progress-overview)
    @Test
    fun givenNoActiveBatch_whenShowingTheScreen_thenItShowsTheTotalAvailableTheCefrLevelAndTheTotalByLevel() {
        // Given/When: no hay tanda activa, un catálogo de seis frases sin progreso y el nivel CEFR B1
        setScreen(
            ProgressUiState.Content(
                totalAvailable = 6,
                cefrLevel = CefrLevel.B1,
                byLevel = allLevels(levelZero = 6),
            ),
        )

        // Then: se muestran las 6 frases disponibles, el nivel CEFR B1 y el total del nivel 0
        composeTestRule.onNodeWithText("6 frases disponibles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nivel actual: B1").assertIsDisplayed()
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
                totalAvailable = 3,
                cefrLevel = CefrLevel.A1,
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

    // Scenario: La fila de nivel 0 no muestra insignia ni número de nivel (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesInLevelZero_whenShowingTheScreen_thenTheNoRankBadgeIsDisplayedForThatRow() {
        // Given/When: el catálogo tiene frases en nivel 0
        setScreen(
            ProgressUiState.Content(
                totalAvailable = 6,
                cefrLevel = CefrLevel.A1,
                byLevel = allLevels(levelZero = 6),
            ),
        )

        // Then: la fila de nivel 0 muestra su número de frases junto a la insignia "Sin rango" (su
        // etiqueta de nivel visible; el nivel 1 a 5 siempre muestran su propia insignia, spec
        // `level-badges`)
        composeTestRule.onNodeWithText("Sin rango").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sin rango: 6 frases").assertIsDisplayed()
    }

    // Scenario: La fila de nivel 0 no muestra insignia ni número de nivel (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesInEveryLevel_whenShowingTheScreen_thenNoRowShowsTheWordNivel() {
        // Given/When: el catálogo tiene frases repartidas entre varios niveles
        setScreen(
            ProgressUiState.Content(
                totalAvailable = (0..Level.MAX_LEVEL).sum(),
                cefrLevel = CefrLevel.A1,
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
