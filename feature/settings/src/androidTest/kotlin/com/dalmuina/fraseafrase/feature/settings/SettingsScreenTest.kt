package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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

    private companion object {
        // Margen para redondeos de píxeles al comparar límites de layout entre nodos (design D1,
        // fix-ui-pass-three).
        const val BOUNDS_TOLERANCE = 0.5f
    }
}
