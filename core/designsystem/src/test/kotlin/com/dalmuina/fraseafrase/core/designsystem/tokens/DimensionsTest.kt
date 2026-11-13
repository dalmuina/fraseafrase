package com.dalmuina.fraseafrase.core.designsystem.tokens

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [Dimensions], el token de altura de controles primarios que expone
 * `AppTheme.dimensions` (fix-redesign-ui-mismatches, design D8). No requiere Compose runtime:
 * comprueba directamente el valor por defecto de [Dimensions].
 */
class DimensionsTest {

    // Scenario: AppTheme.dimensions.primaryActionHeight existe y vale 56.dp (design D8)
    @Test
    fun `given default dimensions when reading primary action height then it is 56dp`() {
        Dimensions().primaryActionHeight shouldBe 56.dp
    }

    // Scenario: icono y separación del botón primario coinciden con design-tokens.md, sección
    // "Botón primario" (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading primary action icon then it matches the mock`() {
        val dimensions = Dimensions()

        dimensions.primaryActionIconSize shouldBe 22.dp
        dimensions.primaryActionIconGap shouldBe 10.dp
    }

    // Scenario: alto, indicador del tab activo e iconos de la barra de navegación coinciden con
    // design-tokens.md, sección "Nav inferior" (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading nav bar then it matches the mock`() {
        val dimensions = Dimensions()

        dimensions.navHeight shouldBe 80.dp
        dimensions.navIndicatorWidth shouldBe 64.dp
        dimensions.navIndicatorHeight shouldBe 32.dp
        dimensions.navIconSize shouldBe 22.dp
    }

    // Scenario: tamaño, offsets e icono del botón flotante de volver a la frase anterior
    // coinciden con design-tokens.md (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading revert fab then it matches the mock`() {
        val dimensions = Dimensions()

        dimensions.revertFabSize shouldBe 56.dp
        dimensions.revertFabHorizontalOffset shouldBe 20.dp
        dimensions.revertFabBottomOffset shouldBe 172.dp
        dimensions.revertFabIconSize shouldBe 24.dp
    }

    // Scenario: tamaño e icono del botón Escuchar coinciden con design-tokens.md
    // (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading listen button then it matches the mock`() {
        val dimensions = Dimensions()

        dimensions.listenButtonSize shouldBe 44.dp
        dimensions.listenButtonIconSize shouldBe 22.dp
    }

    // Scenario: insignia de los chips coincide con design-tokens.md, sección "Chips"
    // (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading chip badge then it matches the mock`() {
        Dimensions().chipBadgeSize shouldBe 18.dp
    }

    // Scenario: icono del mensaje de rango, en su versión normal y compacta, coincide con
    // design-tokens.md, sección "Mensaje de rango" (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading rank message icon then it matches the mock`() {
        val dimensions = Dimensions()

        dimensions.rankMessageIconSize shouldBe 24.dp
        dimensions.rankMessageIconSizeCompact shouldBe 22.dp
    }

    // Scenario: alto de los controles de Ajustes coincide con design-tokens.md, sección
    // "Ajustes" (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading settings control height then it matches the mock`() {
        Dimensions().settingsControlHeight shouldBe 52.dp
    }

    // Scenario: icono y barra de cada fila de rango de Progreso coinciden con design-tokens.md,
    // sección "Fila de rango (Progreso)" (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given default dimensions when reading rank row then it matches the mock`() {
        val dimensions = Dimensions()

        dimensions.rankRowIconSize shouldBe 40.dp
        dimensions.rankBarHeight shouldBe 6.dp
    }
}
