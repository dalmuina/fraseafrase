package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        composeTestRule.onNodeWithText("Frases disponibles").assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nivel actual: B1").assertIsDisplayed()
    }

    // Scenario: tarjeta destacada reestilizada con DsHeroStat (design D3, fix-redesign-ui-mismatches
    // tasks 10.3/10.4). DsHeroStat pinta su texto con el color de contenido indicado (onPrimary),
    // ya no el onPrimaryContainer del DsStatTile centrado anterior.
    @Test
    fun givenTheFeaturedCard_whenShowingTheScreen_thenItsTextUsesOnPrimaryColor() {
        // Given: se captura el color onPrimary esperado del tema actual
        var expectedColor: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedColor = MaterialTheme.colorScheme.onPrimary
                // When: se muestra la pantalla con un catálogo con frases disponibles
                ProgressScreen(
                    uiState = ProgressUiState.Content(
                        totalAvailable = 10,
                        cefrLevel = CefrLevel.B1,
                        byLevel = allLevels(levelZero = 10),
                    ),
                )
            }
        }

        // Then: el color del texto de la tarjeta destacada coincide con onPrimary
        val layoutResults = mutableListOf<TextLayoutResult>()
        composeTestRule.onAllNodesWithText("frases disponibles", substring = true, ignoreCase = true)
            .onFirst()
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)

        assertEquals(expectedColor, layoutResults.first().layoutInput.style.color)
    }

    // Scenario: la cabecera usa el mismo patrón de título grande que Practicar, sin TopAppBar
    // (design D7, fix-redesign-ui-mismatches, mismo patrón que 9.3 en SettingsScreenTest)
    @Test
    fun givenACatalogWithPhrasesAvailable_whenShowingTheScreen_thenTitleUsesHeadlineLargeTypography() {
        // Given: se captura el tamaño de fuente esperado para headlineLarge del tema actual
        var expectedFontSize: TextUnit = TextUnit.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedFontSize = MaterialTheme.typography.headlineLarge.fontSize
                // When: se muestra la pantalla con un catálogo con frases disponibles
                ProgressScreen(
                    uiState = ProgressUiState.Content(
                        totalAvailable = 10,
                        cefrLevel = CefrLevel.B1,
                        byLevel = allLevels(levelZero = 10),
                    ),
                )
            }
        }

        // Then: el tamaño de fuente del título "Progreso" coincide con headlineLarge, no con la
        // tipografía pequeña por defecto de TopAppBar
        val layoutResults = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText("Progreso")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)

        assertEquals(expectedFontSize, layoutResults.first().layoutInput.style.fontSize)
    }

    // Scenario: Total de frases disponibles y nivel CEFR vigente, en una tarjeta destacada con
    // fondo de color propio y texto alineado a la izquierda (spec: progress-overview, design D3,
    // fix-redesign-ui-mismatches)
    @Test
    fun givenACatalogWithPhrasesAvailable_whenShowingTheScreen_thenTheFeaturedCardHasADistinctBackground() {
        // Given/When: la tarjeta destacada muestra 10 frases disponibles y el nivel CEFR B1
        setScreen(
            ProgressUiState.Content(
                totalAvailable = 10,
                cefrLevel = CefrLevel.B1,
                byLevel = allLevels(levelZero = 6) + mapOf(Level(2) to 3, Level(5) to 1),
            ),
        )

        // Then: el valor, la etiqueta y el nivel CEFR vigente comparten el mismo borde izquierdo
        val valueLeft = composeTestRule.onNodeWithText("10").getUnclippedBoundsInRoot().left
        val labelLeft = composeTestRule.onNodeWithText("Frases disponibles")
            .getUnclippedBoundsInRoot().left
        val secondaryLeft = composeTestRule.onNodeWithText("Nivel actual: B1")
            .getUnclippedBoundsInRoot().left
        assertEquals(valueLeft, labelLeft)
        assertEquals(valueLeft, secondaryLeft)

        // Then: el fondo de la tarjeta destacada es distinto del fondo general de la pantalla
        val cardPixelMap = composeTestRule.onNodeWithTag("progress_available_tile")
            .captureToImage()
            .toPixelMap()
        val cardColor = cardPixelMap[2, cardPixelMap.height / 2]
        val screenColor = composeTestRule.onRoot().captureToImage().toPixelMap()[0, 0]
        assertNotEquals(screenColor, cardColor)
    }

    // Scenario: Total de frases disponibles y nivel CEFR vigente (spec: progress-overview)
    @Test
    fun givenACatalogWithPhrasesAvailable_whenShowingTheScreen_thenTheFeaturedCardIsDisplayed() {
        // Given/When: la tarjeta destacada muestra 10 frases disponibles y el nivel CEFR B1
        setScreen(
            ProgressUiState.Content(
                totalAvailable = 10,
                cefrLevel = CefrLevel.B1,
                byLevel = allLevels(levelZero = 6) + mapOf(Level(2) to 3, Level(5) to 1),
            ),
        )

        // Then: la tarjeta destacada se construye con el componente compartido DsHeroStat
        composeTestRule.onNodeWithTag("progress_available_tile").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Frases disponibles").assertIsDisplayed()
        composeTestRule.onNodeWithText("6").assertIsDisplayed()
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

        // Then: la única aparición del texto "Nivel" es la de la tarjeta destacada de nivel CEFR
        // vigente ("Nivel actual: ..."); ninguna fila de nivel por insignia lo usa.
        composeTestRule.onAllNodesWithText("Nivel", substring = true).assertCountEquals(1)
    }

    // Scenario: Total de frases por nivel (spec: progress-overview) - reestilizado con
    // DsProgressBar (design - Componentes Ds* nuevos, redesign-app-ui tasks 7.3/7.4)
    @Test
    fun givenACatalogWithPhrasesInSeveralLevels_whenShowingTheScreen_thenEachLevelRowShowsAProportionalProgressBar() {
        // Given/When: total disponible 10, nivel 2 con 3 frases (proporción 0.3)
        setScreen(
            ProgressUiState.Content(
                totalAvailable = 10,
                cefrLevel = CefrLevel.B1,
                byLevel = allLevels(levelZero = 6) + mapOf(Level(2) to 3, Level(5) to 1),
            ),
        )

        // Then: la fila del nivel 2 muestra una barra de progreso proporcional (3 de 10)
        composeTestRule.onNodeWithTag("progress_level_bar_2")
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 0.3f, range = 0f..1f))
    }

    private fun setScreen(uiState: ProgressUiState) {
        composeTestRule.setContent {
            AppTheme {
                ProgressScreen(uiState = uiState)
            }
        }
    }
}
