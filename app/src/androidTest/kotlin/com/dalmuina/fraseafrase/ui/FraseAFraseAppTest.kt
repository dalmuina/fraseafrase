package com.dalmuina.fraseafrase.ui

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests instrumentados de [FraseAFraseApp] (spec `app-navigation`). Usan el grafo de producción
 * real, sin módulos de prueba (patrón de `PersistenceWiringTest`), para verificar también el
 * cableado de Hilt de `ProgressRoute`.
 */
@HiltAndroidTest
class FraseAFraseAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Scenario: Los tres destinos están disponibles (spec: app-navigation)
    @Test
    fun givenAppLaunched_whenLookingAtTheBottomBar_thenAllThreeDestinationsAreShown() {
        composeTestRule.onNodeWithText("Practicar").assertExists()
        composeTestRule.onNodeWithText("Progreso").assertExists()
        composeTestRule.onNodeWithText("Ajustes").assertExists()
    }

    // Scenario: Arranque muestra la práctica seleccionada (spec: app-navigation)
    @Test
    fun givenAppLaunched_whenStartupFinishes_thenPracticeIsShownAndSelected() {
        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE).assertIsSelected()
        waitForPracticeScreen()
    }

    // Scenario: Seleccionar progreso desde la práctica (spec: app-navigation)
    @Test
    fun givenPracticeScreen_whenSelectingProgress_thenProgressIsShownAndSelected() {
        waitForPracticeScreen()

        composeTestRule.onNodeWithTag(NAV_TAG_PROGRESS).performClick()

        composeTestRule.onNodeWithTag(NAV_TAG_PROGRESS).assertIsSelected()
        waitForProgressScreen()
    }

    // Scenario: Seleccionar ajustes desde el progreso (spec: app-navigation)
    @Test
    fun givenProgressScreen_whenSelectingSettings_thenSettingsIsShownAndSelected() {
        waitForPracticeScreen()
        composeTestRule.onNodeWithTag(NAV_TAG_PROGRESS).performClick()
        waitForProgressScreen()

        composeTestRule.onNodeWithTag(NAV_TAG_SETTINGS).performClick()

        composeTestRule.onNodeWithTag(NAV_TAG_SETTINGS).assertIsSelected()
        composeTestRule.onNodeWithText("Guardar").assertExists()
    }

    // Scenario: Volver a la práctica desde cualquier destino (spec: app-navigation)
    @Test
    fun givenProgressScreen_whenSelectingPractice_thenPracticeIsShownAndSelected() {
        waitForPracticeScreen()
        composeTestRule.onNodeWithTag(NAV_TAG_PROGRESS).performClick()
        waitForProgressScreen()

        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE).performClick()

        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE).assertIsSelected()
        waitForPracticeScreen()
    }

    // Scenario: Volver a la práctica desde cualquier destino (spec: app-navigation)
    @Test
    fun givenSettingsScreen_whenSelectingPractice_thenPracticeIsShownAndSelected() {
        composeTestRule.onNodeWithTag(NAV_TAG_SETTINGS).performClick()
        composeTestRule.onNodeWithText("Guardar").assertExists()

        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE).performClick()

        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE).assertIsSelected()
        waitForPracticeScreen()
    }

    private fun waitForPracticeScreen() {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText("Cerrar tanda").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForProgressScreen() {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText("No hay tanda activa").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val NAV_TAG_PRACTICE = "nav-practice"
        const val NAV_TAG_PROGRESS = "nav-progress"
        const val NAV_TAG_SETTINGS = "nav-settings"
        const val WAIT_TIMEOUT_MILLIS = 5_000L
    }
}
