package com.dalmuina.fraseafrase.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dalmuina.fraseafrase.MainActivity
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

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
        // La cabecera de Practicar también pinta el texto "Practicar" (fix-style-pass-two, design
        // D1), así que ese destino se busca dentro de su elemento de la barra, no por texto suelto.
        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE).assert(hasText("Practicar"))
        composeTestRule.onNodeWithText("Progreso").assertExists()
        composeTestRule.onNodeWithText("Ajustes").assertExists()
    }

    // Scenario: Los tres destinos están disponibles (spec: app-navigation, new-style delta: icono)
    @Test
    fun givenAppLaunched_whenLookingAtTheBottomBar_thenAllThreeDestinationsShowAnIcon() {
        // `NavigationBarItem` merges its icon's semantics into the item node, so the icon's own
        // testTag is only visible in the unmerged tree.
        composeTestRule.onNodeWithTag(NAV_TAG_PRACTICE_ICON, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(NAV_TAG_PROGRESS_ICON, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(NAV_TAG_SETTINGS_ICON, useUnmergedTree = true).assertExists()
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

    // Scenario: la barra de navegación respeta las medidas del mock (bottom-bar-navigation,
    // design D1/D3/D4): queda separada de los tres bordes inferiores de la pantalla por
    // `navFloatingMargin` (más el inset de `WindowInsets.navigationBars` en el borde inferior), en
    // vez de un alto fijo a sangre.
    @Test
    fun givenAppLaunched_whenLookingAtTheBottomBar_thenItsHeightMatchesTheMock() {
        waitForPracticeScreen()

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val barBounds = composeTestRule.onNodeWithTag(NAV_TAG_BAR).getUnclippedBoundsInRoot()
        val navigationBarInset = systemNavigationBarInsetDp()

        val leftMargin = barBounds.left - rootBounds.left
        val rightMargin = rootBounds.right - barBounds.right
        val bottomMargin = rootBounds.bottom - barBounds.bottom

        assertCloseTo("leftMargin", leftMargin, NAV_FLOATING_MARGIN)
        assertCloseTo("rightMargin", rightMargin, NAV_FLOATING_MARGIN)
        assertCloseTo("bottomMargin", bottomMargin, NAV_FLOATING_MARGIN + navigationBarInset)
    }

    // Scenario: La barra no se recorta con barra de gestos del sistema (spec: app-navigation)
    // Scenario: La barra no se recorta con barra de navegación de 3 botones (spec: app-navigation)
    // El mecanismo bajo prueba -- leer el inset real de `WindowInsets.navigationBars` y comprobar que
    // el icono y la etiqueta de cada destino quedan íntegramente por encima de él -- no distingue el
    // tipo de barra del sistema, así que este único test cubre los dos GIVEN de la especificación
    // (design.md, Riesgos): el emulador reporta un inset real y distinto de cero, igual en desarrollo
    // y en CI. Se comprueban los nodos hoja (icono/etiqueta, árbol sin fusionar) porque el nodo
    // fusionado del destino no refleja el posible desbordamiento de su contenido interno.
    @Test
    fun givenAppLaunched_whenLookingAtTheBottomBar_thenAllDestinationsAreAboveTheSystemNavigationBar() {
        waitForPracticeScreen()

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()
        val systemNavigationBarTop = rootBounds.bottom - systemNavigationBarInsetDp()

        listOf(
            NAV_TAG_PRACTICE_ICON,
            "nav-practice-label",
            NAV_TAG_PROGRESS_ICON,
            NAV_TAG_PROGRESS_LABEL,
            NAV_TAG_SETTINGS_ICON,
            NAV_TAG_SETTINGS_LABEL,
        ).forEach { tag ->
            val bounds = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue(
                "tag=$tag bottom=${bounds.bottom}, expected <= $systemNavigationBarTop",
                bounds.bottom <= systemNavigationBarTop,
            )
        }
    }

    private fun systemNavigationBarInsetDp(): Dp {
        val activity = composeTestRule.activity
        val rootInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        val insetPx = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        return with(Density(activity)) { insetPx.toDp() }
    }

    private fun assertCloseTo(label: String, actual: Dp, expected: Dp) {
        assertTrue(
            "$label=$actual, expected ~$expected",
            abs((actual - expected).value) <= SIZE_TOLERANCE_DP.value
        )
    }

    // Scenario: la barra de navegación respeta las medidas del mock (adopt-reference-mock-ui,
    // design D7, tasks 10.1/10.2): indicador del tab activo 64x32dp.
    @Test
    fun givenAppLaunched_whenLookingAtTheBottomBar_thenTheActiveIndicatorMatchesMockSize() {
        waitForPracticeScreen()

        val bounds = composeTestRule
            .onNodeWithTag(NAV_TAG_PRACTICE_INDICATOR, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top

        assertTrue(
            "width=$width, expected ~$NAV_INDICATOR_WIDTH",
            abs((width - NAV_INDICATOR_WIDTH).value) <= SIZE_TOLERANCE_DP.value
        )
        assertTrue(
            "height=$height, expected ~$NAV_INDICATOR_HEIGHT",
            abs((height - NAV_INDICATOR_HEIGHT).value) <= SIZE_TOLERANCE_DP.value
        )
    }

    // Scenario: la barra de navegación respeta los colores del mock (adopt-reference-mock-ui,
    // design D7, tasks 10.1/10.2): las 3 etiquetas inactivas usan el mismo color textSecondary.
    @Test
    fun givenPracticeScreen_whenLookingAtTheInactiveLabels_thenAllThreeUseTheSameTextSecondaryColor() {
        waitForPracticeScreen()

        val progressLabelColor = darkestPixelColor(NAV_TAG_PROGRESS_LABEL)
        val settingsLabelColor = darkestPixelColor(NAV_TAG_SETTINGS_LABEL)

        assertColorCloseTo(AppColors.TextSecondary, progressLabelColor)
        assertColorCloseTo(AppColors.TextSecondary, settingsLabelColor)
    }

    /** Color del píxel más oscuro del nodo con [tag]: aproxima el color de tinta del texto sin
     * verse afectado por el antialiasing de los bordes de los glifos (igual estrategia de
     * muestreo de píxel que `DsStatTileTest`/`PracticeSessionTranslationRevealedScreenTest`). */
    private fun darkestPixelColor(tag: String): Color {
        val pixelMap = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).captureToImage().toPixelMap()
        var darkest = Color.White
        var minLuminance = Float.MAX_VALUE
        for (x in 0 until pixelMap.width) {
            for (y in 0 until pixelMap.height) {
                val pixel = pixelMap[x, y]
                val luminance = pixel.red + pixel.green + pixel.blue
                if (luminance < minLuminance) {
                    minLuminance = luminance
                    darkest = pixel
                }
            }
        }
        return darkest
    }

    private fun assertColorCloseTo(expected: Color, actual: Color) {
        assertTrue(
            "expected=$expected, actual=$actual",
            abs(expected.red - actual.red) <= COLOR_TOLERANCE &&
                abs(expected.green - actual.green) <= COLOR_TOLERANCE &&
                abs(expected.blue - actual.blue) <= COLOR_TOLERANCE
        )
    }

    private fun waitForPracticeScreen() {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText("Mostrar traducción").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // La fila de nivel 0 ("Sin rango: N frases") existe siempre en el estado con contenido
    // (ADR-0012/add-level-badges D5), tanto con tanda activa como sin ella: a diferencia de "No hay
    // tanda activa", no asume que la práctica no haya creado ya una tanda (`ObtainPracticeSession`
    // compone una al leer la sesión, incluso sin interacción del usuario).
    private fun waitForProgressScreen() {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText("Sin rango", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val NAV_TAG_PRACTICE = "nav-practice"
        const val NAV_TAG_PROGRESS = "nav-progress"
        const val NAV_TAG_SETTINGS = "nav-settings"
        const val NAV_TAG_PRACTICE_ICON = "nav-practice-icon"
        const val NAV_TAG_PROGRESS_ICON = "nav-progress-icon"
        const val NAV_TAG_SETTINGS_ICON = "nav-settings-icon"
        const val NAV_TAG_BAR = "bottom-nav-bar"
        const val NAV_TAG_PRACTICE_INDICATOR = "nav-practice-indicator"
        const val NAV_TAG_PROGRESS_LABEL = "nav-progress-label"
        const val NAV_TAG_SETTINGS_LABEL = "nav-settings-label"
        const val WAIT_TIMEOUT_MILLIS = 5_000L
        val NAV_FLOATING_MARGIN = 16.dp
        val NAV_INDICATOR_WIDTH = 64.dp
        val NAV_INDICATOR_HEIGHT = 32.dp
        val SIZE_TOLERANCE_DP = 1.dp
        const val COLOR_TOLERANCE = 0.02f
    }
}
