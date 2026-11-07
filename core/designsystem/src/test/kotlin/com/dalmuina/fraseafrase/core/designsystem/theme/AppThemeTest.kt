package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de los roles de `ColorScheme` usados por [AppTheme] (design D1,
 * `openspec/changes/new-style`). No requieren Compose runtime: comprueban directamente los valores
 * de [LightColors]/[DarkColors] que [AppTheme] pasa a `MaterialTheme`.
 */
class AppThemeTest {

    // Scenario: paleta clara fija los roles de color del design D1
    @Test
    fun `given light color scheme when reading roles then they match design D1`() {
        LightColors.primary shouldBe Color(0xFF3346C8)
        LightColors.onPrimary shouldBe Color(0xFFFFFFFF)
        LightColors.primaryContainer shouldBe Color(0xFFE6E9FB)
        LightColors.onPrimaryContainer shouldBe Color(0xFF1B2559)
        LightColors.tertiary shouldBe Color(0xFF1B6B44)
        LightColors.onTertiary shouldBe Color(0xFFFFFFFF)
        LightColors.background shouldBe Color(0xFFF5F6FA)
        LightColors.surface shouldBe Color(0xFFFFFFFF)
        LightColors.onSurface shouldBe Color(0xFF14171F)
    }

    // Scenario: paleta oscura deriva los mismos matices del design D1, sin mock de referencia
    @Test
    fun `given dark color scheme when reading roles then they differ from default Material3`() {
        // Material 3 por defecto en oscuro usa primary = #D0BCFF (morado) y background = #1C1B1F:
        // la paleta oscura definitiva debe derivar del matiz índigo/verde fijado en claro, no del
        // morado por defecto.
        DarkColors.primary shouldBe Color(0xFFB9C3F2)
        DarkColors.onPrimary shouldBe Color(0xFF1B2559)
        DarkColors.tertiary shouldBe Color(0xFF7FCBA0)
        DarkColors.onTertiary shouldBe Color(0xFF0B2E1A)
        DarkColors.background shouldBe Color(0xFF14171F)
        DarkColors.surface shouldBe Color(0xFF1E2130)
        DarkColors.onSurface shouldBe Color(0xFFE6E8F0)
    }
}
