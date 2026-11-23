package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Tests de UI de [SettingsScreen] (ADR-0007). La pantalla es sin estado, así que cada test construye el estado a
 * mano, sin ViewModel.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: Se muestran los ajustes guardados (spec: settings-screen)
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenLevelAndBatchSizeAreDisplayed() {
        // Given/When: la pantalla muestra el nivel CEFR B1 y N igual a 25
        setScreen(loaded(cefrLevel = CefrLevel.B1, batchSizeText = "25"))

        // Then: se muestran ambos valores
        composeTestRule.onNodeWithText("B1").assertIsDisplayed()
        composeTestRule.onNodeWithText("25").assertIsDisplayed()
    }

    // Scenario: no hay escenario de spec propio; cubre las tareas 4.1/4.2 (remove-settings-save-button):
    // ya no existe ningún botón "Guardar" ni su icono, porque cada edición se autoguarda (grupo 3).
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenThereIsNoSaveButton() {
        // Given/When: la pantalla muestra los ajustes cargados
        setScreen(loaded())

        // Then: no hay ningún texto, testTag de botón ni testTag de icono de "Guardar"
        composeTestRule.onAllNodesWithText("Guardar").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("settings_save_button").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("settings_save_icon", useUnmergedTree = true).assertCountEquals(0)
    }

    // Scenario: El control de disminuir se deshabilita en el mínimo (spec: settings-screen)
    @Test
    fun givenBatchSizeAtMinimum_whenShowingTheScreen_thenDecreaseButtonIsDisabled() {
        // Given/When: la pantalla muestra el tamaño de tanda en el mínimo (10)
        setScreen(loaded(batchSizeText = "10"))

        // Then: el control de disminuir está deshabilitado
        composeTestRule.onNodeWithContentDescription("Disminuir tamaño de tanda").assertIsNotEnabled()
    }

    // Scenario: El control de aumentar se deshabilita en el máximo (spec: settings-screen)
    @Test
    fun givenBatchSizeAtMaximum_whenShowingTheScreen_thenIncreaseButtonIsDisabled() {
        // Given/When: la pantalla muestra el tamaño de tanda en el máximo (30)
        setScreen(loaded(batchSizeText = "30"))

        // Then: el control de aumentar está deshabilitado
        composeTestRule.onNodeWithContentDescription("Aumentar tamaño de tanda").assertIsNotEnabled()
    }

    // Scenario: El control de disminuir se reactiva por encima del mínimo (spec: settings-screen)
    @Test
    fun givenBatchSizeAboveMinimum_whenTypingAValue_thenDecreaseButtonIsEnabled() {
        // Given: la pantalla muestra el tamaño de tanda en el mínimo (10)
        var uiState: SettingsUiState by mutableStateOf(loaded(batchSizeText = "10"))
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is SettingsAction.ChangeBatchSizeText) {
                            uiState = loaded(batchSizeText = action.text)
                        }
                    },
                )
            }
        }

        // When: el usuario escribe 15 como tamaño de tanda
        composeTestRule.onNodeWithText("10").performTextReplacement("15")

        // Then: el control de disminuir deja de estar deshabilitado
        composeTestRule.onNodeWithContentDescription("Disminuir tamaño de tanda").assertIsEnabled()
    }

    // Scenario: Elegir un nivel CEFR de la lista (spec: settings-screen) - selector segmentado accesible como
    // grupo de radio (design.md - Risks/Trade-offs)
    @Test
    fun givenCefrLevelSelector_whenShowingTheScreen_thenItIsASelectableRadioGroup() {
        // Given/When: la pantalla se muestra con el nivel B1 seleccionado
        setScreen(loaded(cefrLevel = CefrLevel.B1))

        // Then: el contenedor de los cinco niveles es un grupo de selección única
        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()

        // Then: el botón del nivel seleccionado tiene rol de radio y está marcado como seleccionado
        composeTestRule.onNodeWithText("B1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()

        // Then: un botón no seleccionado también tiene rol de radio, pero no está marcado como seleccionado
        composeTestRule.onNodeWithText("A1")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsNotSelected()
    }

    // Scenario: Elegir un nivel CEFR de la lista (spec: settings-screen) - botones propios de 52dp en vez de
    // SegmentedButtonRow (design.md decisión 4, adopt-reference-mock-ui)
    @Test
    fun givenCefrLevelSelector_whenShowingTheScreen_thenEachButtonUsesTheSettingsControlHeight() {
        // Given: se captura la altura de control esperada para los ajustes del tema actual
        var expectedHeightDp = 0f
        composeTestRule.setContent {
            AppTheme {
                expectedHeightDp = AppTheme.dimensions.settingsControlHeight.value
                // When: se muestra la pantalla con el nivel B1 seleccionado
                SettingsScreen(uiState = loaded(cefrLevel = CefrLevel.B1), onAction = {})
            }
        }

        // Then: la altura del botón de nivel seleccionado coincide con el control de 52dp del mock, no con la
        // altura por defecto de SegmentedButtonRow
        val bounds = composeTestRule.onNodeWithText("B1").getBoundsInRoot()
        val actualHeightDp = (bounds.bottom - bounds.top).value
        assertEquals(expectedHeightDp, actualHeightDp, BOUNDS_TOLERANCE)
    }

    // Scenario: estilo de etiqueta pequeña compartido para insignias y nivel CEFR (design D4,
    // fix-style-pass-two)
    @Test
    fun givenCefrLevelChip_whenShowingTheScreen_thenLabelUsesLabelMediumTypography() {
        // Given: se captura el tamaño de fuente esperado para labelMedium del tema actual
        var expectedFontSize: TextUnit = TextUnit.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedFontSize = MaterialTheme.typography.labelMedium.fontSize
                // When: se muestra la pantalla con el nivel CEFR B1 seleccionado
                SettingsScreen(uiState = loaded(cefrLevel = CefrLevel.B1), onAction = {})
            }
        }

        // Then: el tamaño de fuente de la etiqueta del chip coincide con labelMedium
        val layoutResults = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText("B1")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)

        assertEquals(expectedFontSize, layoutResults.first().layoutInput.style.fontSize)
    }

    // Scenario: Los controles se alinean con el campo de texto sin error visible (spec: settings-screen,
    // fix-ui-pass-three)
    @Test
    fun givenNoBatchSizeError_whenShowingTheScreen_thenStepperButtonsAlignWithTextFieldTop() {
        // Given/When: la pantalla muestra un tamaño de tanda válido, sin texto de error visible
        setScreen(loaded(batchSizeText = "20", isBatchSizeValid = true))

        // Then: el borde superior de los controles de disminuir y aumentar coincide con el del campo de texto
        val textFieldTop = composeTestRule.onNodeWithText("20").getBoundsInRoot().top
        val decreaseTop = composeTestRule.onNodeWithContentDescription("Disminuir tamaño de tanda")
            .getBoundsInRoot().top
        val increaseTop = composeTestRule.onNodeWithContentDescription("Aumentar tamaño de tanda")
            .getBoundsInRoot().top

        assertEquals(textFieldTop.value, decreaseTop.value, BOUNDS_TOLERANCE)
        assertEquals(textFieldTop.value, increaseTop.value, BOUNDS_TOLERANCE)
    }

    // Scenario: Los controles se mantienen alineados cuando el campo muestra el texto de error (spec:
    // settings-screen, fix-ui-pass-three)
    @Test
    fun givenBatchSizeErrorVisible_whenShowingTheScreen_thenStepperButtonsAlignWithTextFieldTop() {
        // Given/When: la pantalla muestra un tamaño de tanda no válido, con su texto de error visible
        setScreen(loaded(batchSizeText = "5", isBatchSizeValid = false))

        // Then: el borde superior de los controles de disminuir y aumentar sigue coincidiendo con el del
        // campo de texto
        val textFieldTop = composeTestRule.onNodeWithText("5").getBoundsInRoot().top
        val decreaseTop = composeTestRule.onNodeWithContentDescription("Disminuir tamaño de tanda")
            .getBoundsInRoot().top
        val increaseTop = composeTestRule.onNodeWithContentDescription("Aumentar tamaño de tanda")
            .getBoundsInRoot().top

        assertEquals(textFieldTop.value, decreaseTop.value, BOUNDS_TOLERANCE)
        assertEquals(textFieldTop.value, increaseTop.value, BOUNDS_TOLERANCE)
    }

    // Scenario: Estilo de tarjeta del selector de idioma destino (spec: settings-screen) - cubre las
    // tareas 4.3/4.4 (remove-settings-save-button): el selector de idioma destino tiene el mismo fondo
    // que las tarjetas de nivel CEFR y de tamaño de tanda, en vez de quedar suelto sobre el fondo de la
    // pantalla.
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenLanguageSelectorHasTheSameCardBackgroundAsCefrCard() {
        // Given: la pantalla muestra los ajustes cargados, con el fondo de tarjeta del tema actual
        var expectedCardColor: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedCardColor = MaterialTheme.colorScheme.surface
                // When: se muestra la pantalla
                SettingsScreen(uiState = loaded(), onAction = {})
            }
        }

        // Then: el fondo pintado de la tarjeta del selector de idioma coincide con el de la tarjeta de
        // nivel CEFR, ambos con el mismo color de fondo de tarjeta
        val languageCardBackground = cardBackgroundColor("settings_language_card")
        val cefrCardBackground = cardBackgroundColor("settings_cefr_card")

        assertSolidColor(expectedCardColor, languageCardBackground)
        assertSolidColor(expectedCardColor, cefrCardBackground)
    }

    // Scenario: no hay escenario de spec propio; corrección visual del hallazgo 6 de la tarea 11.3
    // (adopt-reference-mock-ui, tarea 12.5): faltan los textos de ayuda bajo "Nivel CEFR" y "Tamaño
    // de tanda" que sí aparecen en el mock.
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenHelpTextsAreDisplayed() {
        // Given/When: la pantalla muestra los ajustes cargados
        setScreen(loaded())

        // Then: se muestran los textos de ayuda del nivel CEFR y del tamaño de tanda
        composeTestRule.onNodeWithText("Elige tu nivel. Solo verás frases de este nivel o inferiores.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Mínimo 10 frases por sesión.").assertIsDisplayed()
    }

    // Scenario: la cabecera usa el mismo patrón de título grande que Practicar, sin TopAppBar
    // (design D7, fix-redesign-ui-mismatches)
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenTitleUsesHeadlineLargeTypography() {
        // Given: se captura el tamaño de fuente esperado para headlineLarge del tema actual
        var expectedFontSize: TextUnit = TextUnit.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedFontSize = MaterialTheme.typography.headlineLarge.fontSize
                // When: se muestra la pantalla con ajustes cargados
                SettingsScreen(uiState = loaded(), onAction = {})
            }
        }

        // Then: el tamaño de fuente del título "Ajustes" coincide con headlineLarge, no con la
        // tipografía pequeña por defecto de TopAppBar
        val layoutResults = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText("Ajustes")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)

        assertEquals(expectedFontSize, layoutResults.first().layoutInput.style.fontSize)
    }

    // Scenario: Elegir un idioma destino de la lista (spec: settings-screen)
    @Test
    fun givenActiveLanguage_whenShowingTheScreen_thenTheLanguageDropdownShowsItPreselected() {
        // Given/When: la pantalla muestra pt-PT como idioma activo entre pt-PT y en-GB disponibles
        setScreen(
            loaded(
                activeTargetLocale = LanguageTag("pt-PT"),
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("en-GB")),
            ),
        )

        // Then: el desplegable de idioma muestra el nombre de pt-PT preseleccionado
        composeTestRule.onNodeWithText("Português (Portugal)").assertIsDisplayed()
    }

    // Scenario: Elegir un idioma destino de la lista (spec: settings-screen) - es-ES disponible entre
    // los idiomas destino (add-spanish-target-language)
    @Test
    fun givenEsEsAvailable_whenShowingTheScreen_thenTheLanguageDropdownShowsItsSpanishName() {
        // Given/When: la pantalla muestra pt-PT como idioma activo con pt-PT, en-GB y es-ES disponibles
        setScreen(
            loaded(
                activeTargetLocale = LanguageTag("pt-PT"),
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("en-GB"), LanguageTag("es-ES")),
            ),
        )

        // When: el usuario abre el desplegable (su opción de es-ES solo se compone expandido)
        composeTestRule.onNodeWithText("Português (Portugal)").performClick()

        // Then: el desplegable de idioma muestra el nombre de es-ES entre sus opciones
        composeTestRule.onNodeWithText("Español (España)").assertIsDisplayed()
    }

    // Scenario: Elegir un idioma destino de la lista (spec: settings-screen) - it-IT, en-GB, de-DE,
    // fr-FR y ru-RU disponibles entre los idiomas destino (add-multilingual-sources)
    @Test
    fun givenNewTargetLanguagesAvailable_whenShowingTheScreen_thenTheLanguageDropdownShowsTheirNames() {
        // Given/When: la pantalla muestra pt-PT como idioma activo con los 7 idiomas disponibles
        setScreen(
            loaded(
                activeTargetLocale = LanguageTag("pt-PT"),
                availableTargetLocales = listOf(
                    LanguageTag("pt-PT"),
                    LanguageTag("es-ES"),
                    LanguageTag("it-IT"),
                    LanguageTag("en-GB"),
                    LanguageTag("de-DE"),
                    LanguageTag("fr-FR"),
                    LanguageTag("ru-RU"),
                ),
            ),
        )

        // When: el usuario abre el desplegable (las opciones solo se componen expandido)
        composeTestRule.onNodeWithText("Português (Portugal)").performClick()

        // Then: el desplegable de idioma muestra el nombre de cada uno de los 5 idiomas nuevos
        composeTestRule.onNodeWithText("Italiano (Italia)").assertIsDisplayed()
        composeTestRule.onNodeWithText("English (UK)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deutsch (Deutschland)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Français (France)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Русский (Россия)").assertIsDisplayed()
    }

    // Scenario: El desplegable de destino excluye el idioma de origen (spec: settings-screen)
    @Test
    fun givenStateWithoutSourceLanguage_whenOpeningTheDropdown_thenSourceLanguageIsNotListed() {
        // Given: el estado ofrece pt-PT y en-GB (es-ES es el origen y no viene en la lista)
        setScreen(
            loaded(
                activeTargetLocale = LanguageTag("pt-PT"),
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("en-GB")),
            ),
        )

        // When: el usuario abre el desplegable
        composeTestRule.onNodeWithText("Português (Portugal)").performClick()

        // Then: aparece English (UK) y no Español (España)
        composeTestRule.onNodeWithText("English (UK)").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Español (España)").assertCountEquals(0)
    }

    // Scenario: El icono muestra el idioma activo en Ajustes (spec: language-indicator)
    @Test
    fun givenActiveTargetLocaleEnGb_whenShowingTheScreen_thenTheLanguageBadgeIsDisplayed() {
        // Given/When: la cabecera muestra el idioma destino activo en-GB
        setScreen(loaded(activeTargetLocale = LanguageTag("en-GB")))

        // Then: la cabecera muestra el icono circular correspondiente a en-GB
        composeTestRule.onNodeWithContentDescription("en-GB").assertIsDisplayed()
    }

    // Scenario: Tocar el icono estando ya en Ajustes no hace nada (spec: language-indicator)
    @Test
    fun givenSettingsLoaded_whenTappingTheLanguageBadge_thenOnNavigateToSettingsIsInvoked() {
        // Given: la pantalla de Ajustes abierta, con un callback de navegación a Ajustes
        var navigatedToSettings = false
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    uiState = loaded(),
                    onAction = {},
                    onNavigateToSettings = { navigatedToSettings = true },
                )
            }
        }

        // When: el usuario toca el icono circular del idioma activo
        composeTestRule.onNodeWithContentDescription("pt-PT").performClick()

        // Then: la aplicación sigue mostrando la pantalla de Ajustes (no hace nada más)
        assertTrue(navigatedToSettings)
    }

    private fun setScreen(uiState: SettingsUiState) {
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(uiState = uiState, onAction = {})
            }
        }
    }

    private fun loaded(
        activeTargetLocale: LanguageTag = LanguageTag("pt-PT"),
        availableTargetLocales: List<LanguageTag> = listOf(LanguageTag("pt-PT")),
        cefrLevel: CefrLevel = CefrLevel.A1,
        batchSizeText: String = "20",
        isBatchSizeValid: Boolean = true,
    ): SettingsUiState.Loaded = SettingsUiState.Loaded(
        activeTargetLocale = activeTargetLocale,
        availableTargetLocales = availableTargetLocales,
        cefrLevel = cefrLevel,
        batchSizeText = batchSizeText,
        isBatchSizeValid = isBatchSizeValid,
    )

    /**
     * Color de fondo pintado de la tarjeta con [tag]: muestrea un píxel centrado horizontalmente y
     * cerca del borde superior (4dp), por encima del `Modifier.padding(AppTheme.spacing.l)` de su
     * contenido, para no caer sobre ningún hijo ni sobre la curva de la esquina redondeada.
     */
    private fun cardBackgroundColor(tag: String): Color {
        val node = composeTestRule.onNodeWithTag(tag).performScrollTo()
        val bounds = node.getUnclippedBoundsInRoot()
        val widthPx = ((bounds.right - bounds.left).value * composeTestRule.density.density).toInt()
        val insetPx = (CARD_BACKGROUND_SAMPLE_INSET_DP * composeTestRule.density.density).toInt()
        val pixelMap = node.captureToImage().toPixelMap()
        return pixelMap[widthPx / 2, insetPx]
    }

    /**
     * Compara [actual] con [expected] admitiendo el margen de antialiasing entre emuladores
     * ([COLOR_CHANNEL_TOLERANCE] por canal), en vez de igualdad exacta. Misma técnica que
     * `PracticeSessionScreenTest.assertSolidColor` (`:feature:session`).
     */
    private fun assertSolidColor(expected: Color, actual: Color) {
        val close = abs(expected.red - actual.red) <= COLOR_CHANNEL_TOLERANCE &&
            abs(expected.green - actual.green) <= COLOR_CHANNEL_TOLERANCE &&
            abs(expected.blue - actual.blue) <= COLOR_CHANNEL_TOLERANCE
        assertTrue("expected=$expected, actual=$actual", close)
    }

    private companion object {
        // Margen para redondeos de píxeles al comparar límites de layout entre nodos (design D1,
        // fix-ui-pass-three).
        const val BOUNDS_TOLERANCE = 0.5f

        // Igual que `PracticeSessionScreenTest.COLOR_CHANNEL_TOLERANCE` (`:feature:session`).
        const val COLOR_CHANNEL_TOLERANCE = 0.08f

        // Distancia desde el borde superior de una tarjeta usada para muestrear su fondo, por debajo
        // del `Modifier.padding(AppTheme.spacing.l)` (24dp) de su contenido.
        const val CARD_BACKGROUND_SAMPLE_INSET_DP = 4f
    }
}
