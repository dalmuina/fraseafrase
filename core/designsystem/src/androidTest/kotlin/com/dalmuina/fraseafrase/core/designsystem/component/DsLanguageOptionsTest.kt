package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [DsLanguageOptions] (ADR-0007, add-multi-language-catalog). Verifican el
 * desplegable de idiomas destino de la especificación `settings-screen`, requisito "Selección del
 * idioma destino".
 */
class DsLanguageOptionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val options = listOf(
        DsLanguageOption(code = "pt-PT", displayName = "Portugués (Portugal)"),
        DsLanguageOption(code = "en-GB", displayName = "Inglés (Reino Unido)")
    )

    // Scenario: Elegir un idioma destino de la lista (settings-screen). Verifica que se listan los
    // idiomas destino disponibles y que el activo aparece marcado como seleccionado al abrir.
    @Test
    fun givenAvailableLanguagesWhenOpeningThenActiveOneIsSelected() {
        composeTestRule.setContent {
            AppTheme {
                DsLanguageOptions(
                    options = options,
                    selectedCode = "pt-PT",
                    onSelect = {}
                )
            }
        }

        // Then: el campo cerrado muestra el nombre del idioma activo
        composeTestRule.onNodeWithText("Portugués (Portugal)").assertExists()

        // When: se abre el desplegable
        composeTestRule.onNodeWithText("Portugués (Portugal)").performClick()

        // Then: ambas opciones aparecen listadas, y la activa está marcada como seleccionada
        composeTestRule.onNodeWithText("Inglés (Reino Unido)").assertExists()
        composeTestRule.onNodeWithContentDescription("pt-PT").assertIsSelected()
    }

    // Scenario: Elegir un idioma destino de la lista (settings-screen)
    @Test
    fun givenDropdownOpenWhenSelectingAnOptionThenOnSelectReceivesItsCode() {
        var selected: String? = null
        composeTestRule.setContent {
            AppTheme {
                DsLanguageOptions(
                    options = options,
                    selectedCode = "pt-PT",
                    onSelect = { selected = it }
                )
            }
        }

        // When: se abre el desplegable y se elige en-GB
        composeTestRule.onNodeWithText("Portugués (Portugal)").performClick()
        composeTestRule.onNodeWithText("Inglés (Reino Unido)").performClick()

        // Then: la selección notificada es el código de en-GB
        assert(selected == "en-GB")
    }
}
