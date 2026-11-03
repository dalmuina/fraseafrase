package com.dalmuina.fraseafrase.ui

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/**
 * Test de integración de la navegación de `:app` (design.md - Decisiones, "Navegación: Navigation 3 básico").
 * Usa el grafo de producción real (ADR-0007, ADR-0014): sin módulos de prueba.
 */
@HiltAndroidTest
class FraseAFraseAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Scenario: Pantalla de práctica como inicio (spec: practice-session); Abrir ajustes desde la práctica y
    // Volver sin guardar (spec: settings-screen)
    @Test
    fun givenAppStart_whenLaunched_thenShowsPracticeThenSettings() {
        // Given: la app arranca mostrando la práctica
        composeTestRule.onNodeWithText("Ajustes").assertExists()

        // When: el usuario activa la acción de ajustes
        composeTestRule.onNodeWithText("Ajustes").performClick()

        // Then: se muestra la pantalla de ajustes
        composeTestRule.onNodeWithText("Volver").assertExists()

        // When: el usuario vuelve sin guardar
        composeTestRule.onNodeWithText("Volver").performClick()

        // Then: la app vuelve a mostrar la práctica
        composeTestRule.onNodeWithText("Ajustes").assertExists()
    }
}
