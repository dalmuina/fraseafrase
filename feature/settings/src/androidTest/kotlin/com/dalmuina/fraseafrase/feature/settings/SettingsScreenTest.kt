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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.math.sqrt

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

    // Scenario: N fuera de rango impide guardar (spec: settings-screen)
    @Test
    fun givenInvalidBatchSize_whenShowingTheScreen_thenSaveIsDisabled() {
        // Given/When: la pantalla muestra un tamaño de tanda no válido
        setScreen(loaded(isBatchSizeValid = false))

        // Then: la acción de guardar no está disponible
        composeTestRule.onNodeWithText("Guardar").assertIsNotEnabled()
    }

    // Scenario: N dentro de rango permite guardar (spec: settings-screen)
    @Test
    fun givenValidBatchSize_whenShowingTheScreen_thenSaveIsEnabled() {
        // Given/When: la pantalla muestra un tamaño de tanda válido
        setScreen(loaded(isBatchSizeValid = true))

        // Then: la acción de guardar está disponible
        composeTestRule.onNodeWithText("Guardar").assertIsEnabled()
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

    // Scenario: Confirmación visual al guardar (spec: settings-screen; el icono es un detalle de
    // presentación del mock, fix-redesign-ui-mismatches)
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenSaveButtonShowsACheckIcon() {
        // Given/When: la pantalla muestra el botón de guardar
        setScreen(loaded())

        // Then: el botón de guardar incluye un icono de check junto al texto
        composeTestRule.onNodeWithTag("settings_save_icon", useUnmergedTree = true).assertIsDisplayed()
    }

    // Scenario: no hay escenario de spec propio; corrección visual del hallazgo 3 de la tarea 11.3
    // (adopt-reference-mock-ui, tarea 12.2): el botón "Guardar" debe tener radio 16dp ("no son
    // píldoras", `design/mock/design-tokens.md`), no la forma de píldora por defecto de Material 3.
    @Test
    fun givenLoadedSettings_whenShowingTheScreen_thenSaveButtonIsNotFullyRounded() {
        // Given: la pantalla muestra el botón de guardar habilitado
        var expectedPrimary: Color = Color.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedPrimary = MaterialTheme.colorScheme.primary
                SettingsScreen(uiState = loaded(), onAction = {})
            }
        }

        // When: se mide la esquina del botón, a mitad de camino entre el punto en el que dejaría de
        // estar coloreada con el radio objetivo (16dp) y el punto en el que dejaría de estarlo con el
        // radio de una píldora completa (la mitad de la altura medida del propio botón)
        val cornerColor = notFullyRoundedCornerColor("settings_save_button", TARGET_SAVE_BUTTON_RADIUS_DP)

        // Then: a esa distancia de la esquina, el botón ya está coloreado
        assertEquals(expectedPrimary, cornerColor)
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

    private fun setScreen(uiState: SettingsUiState) {
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(uiState = uiState, onAction = {})
            }
        }
    }

    private fun loaded(
        cefrLevel: CefrLevel = CefrLevel.A1,
        batchSizeText: String = "20",
        isBatchSizeValid: Boolean = true,
    ): SettingsUiState.Loaded = SettingsUiState.Loaded(
        cefrLevel = cefrLevel,
        batchSizeText = batchSizeText,
        isBatchSizeValid = isBatchSizeValid,
    )

    /**
     * Color del píxel diagonal del nodo con [tag], a una distancia de su esquina elegida para
     * distinguir un radio objetivo [targetRadiusDp] de una píldora completa (radio igual a la mitad
     * de la altura medida del propio nodo). Misma técnica que
     * `PracticeSessionScreenTest.notFullyRoundedCornerColor` (`:feature:session`).
     */
    private fun notFullyRoundedCornerColor(tag: String, targetRadiusDp: Float): Color {
        val node = composeTestRule.onNodeWithTag(tag)
        val bounds = node.getUnclippedBoundsInRoot()
        val halfHeightDp = (bounds.bottom - bounds.top).value / 2f
        val cornerFactor = 1f - 1f / sqrt(2f)
        val targetRadiusThresholdDp = targetRadiusDp * cornerFactor
        val fullPillRadiusThresholdDp = halfHeightDp * cornerFactor
        val sampleDp = (targetRadiusThresholdDp + fullPillRadiusThresholdDp) / 2f
        val density = composeTestRule.density.density
        val insetPx = (sampleDp * density).toInt()
        val pixelMap = node.captureToImage().toPixelMap()
        return pixelMap[insetPx, insetPx]
    }

    private companion object {
        // Margen para redondeos de píxeles al comparar límites de layout entre nodos (design D1,
        // fix-ui-pass-three).
        const val BOUNDS_TOLERANCE = 0.5f

        // Radio objetivo del botón "Guardar" (16dp, "no son píldoras", design-tokens.md), igual que
        // `PracticeSessionScreenTest.TARGET_BUTTON_RADIUS_DP`.
        const val TARGET_SAVE_BUTTON_RADIUS_DP = 16f
    }
}
