package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // Scenario: Volver sin guardar (spec: settings-screen)
    @Test
    fun givenTheScreen_whenBackIsActivated_thenOnBackIsInvokedDirectlyWithoutAnAction() {
        // Given: la pantalla con un callback de volver y uno de acción, ambos vacíos de inicio
        var backInvoked = false
        var actionInvoked = false
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    uiState = loaded(),
                    onAction = { actionInvoked = true },
                    onBack = { backInvoked = true },
                )
            }
        }

        // When: el usuario activa la acción de volver
        composeTestRule.onNodeWithText("Volver").performClick()

        // Then: se invoca onBack directamente, sin pasar por onAction
        assertTrue(backInvoked)
        assertFalse(actionInvoked)
    }

    private fun setScreen(uiState: SettingsUiState) {
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(uiState = uiState, onAction = {}, onBack = {})
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
}
