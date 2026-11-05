package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
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

    // Scenario: Confirmación tras guardar (spec: settings-screen)
    @Test
    fun givenASnackbarMessage_whenShownThroughTheScreen_thenTheConfirmationTextIsDisplayed() {
        // Given/When: la pantalla muestra el snackbar con el texto de confirmación de guardado, como hará
        // SettingsRoute cuando saveConfirmationPending sea true (design D6)
        composeTestRule.setContent {
            AppTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(Unit) { snackbarHostState.showSnackbar(CONFIRMATION_TEXT) }
                SettingsScreen(
                    uiState = loaded(),
                    onAction = {},
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        // Then: se muestra el texto de confirmación
        composeTestRule.onNodeWithText(CONFIRMATION_TEXT).assertIsDisplayed()
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
        const val CONFIRMATION_TEXT = "Ajustes guardados"
    }
}
