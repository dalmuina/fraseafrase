package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de los roles de `ColorScheme` usados por [AppTheme] (design D1 del cambio
 * `new-style`, actualizado por el design del cambio `redesign-app-ui`: paleta índigo del mock
 * visual, con éxito/fallo/aviso dedicados). No requieren Compose runtime: comprueban directamente
 * los valores de [LightColors]/[DarkColors] que [AppTheme] pasa a `MaterialTheme`.
 */
class AppThemeTest {

    // Scenario: paleta clara adopta el índigo, éxito, fallo y aviso naranja del mock visual
    // (redesign-app-ui, design - Tokens de :core:designsystem), con contraste 4.5:1 verificado
    @Test
    fun `given light color scheme when reading roles then they match the redesign mock`() {
        LightColors.primary shouldBe Color(0xFF5B3DF5)
        LightColors.onPrimary shouldBe Color(0xFFFFFFFF)
        LightColors.primaryContainer shouldBe Color(0xFFECE7FF)
        LightColors.onPrimaryContainer shouldBe Color(0xFF1B2559)
        LightColors.secondary shouldBe Color(0xFFFF7A50)
        LightColors.onSecondary shouldBe Color(0xFF5C2A12)
        LightColors.secondaryContainer shouldBe Color(0xFFFFF1EA)
        LightColors.onSecondaryContainer shouldBe Color(0xFF9A3412)
        LightColors.tertiary shouldBe Color(0xFF087F45)
        LightColors.onTertiary shouldBe Color(0xFFFFFFFF)
        LightColors.tertiaryContainer shouldBe Color(0xFFE3F6EC)
        LightColors.onTertiaryContainer shouldBe Color(0xFF08693A)
        LightColors.error shouldBe Color(0xFFB4232F)
        LightColors.onError shouldBe Color(0xFFFFFFFF)
        LightColors.errorContainer shouldBe Color(0xFFFFECEE)
        LightColors.onErrorContainer shouldBe Color(0xFFB4232F)
        // background corregido a #F7F5FF por adopt-reference-mock-ui (design - decisión 2)
        LightColors.background shouldBe Color(0xFFF7F5FF)
        LightColors.surface shouldBe Color(0xFFFFFFFF)
        LightColors.onSurface shouldBe Color(0xFF14171F)
    }

    // Scenario: paleta oscura deriva los mismos matices del mock visual, sin mock oscuro de
    // referencia (redesign-app-ui, design - Risks: contraste 4.5:1 revisado para cada combinación)
    @Test
    fun `given dark color scheme when reading roles then they differ from default Material3`() {
        // Material 3 por defecto en oscuro usa primary = #D0BCFF (morado) y background = #1C1B1F:
        // la paleta oscura definitiva debe derivar del índigo/verde/naranja fijado en claro, no del
        // morado por defecto.
        DarkColors.primary shouldBe Color(0xFF9886F9)
        DarkColors.onPrimary shouldBe Color(0xFF1B2559)
        DarkColors.primaryContainer shouldBe Color(0xFF32248D)
        DarkColors.onPrimaryContainer shouldBe Color(0xFFECE7FF)
        DarkColors.secondary shouldBe Color(0xFFFFA98A)
        DarkColors.onSecondary shouldBe Color(0xFF2B1206)
        DarkColors.secondaryContainer shouldBe Color(0xFF5C2A12)
        DarkColors.onSecondaryContainer shouldBe Color(0xFFFFF1EA)
        DarkColors.tertiary shouldBe Color(0xFF7FCBA0)
        DarkColors.onTertiary shouldBe Color(0xFF0B2E1A)
        DarkColors.tertiaryContainer shouldBe Color(0xFF0C4D27)
        DarkColors.onTertiaryContainer shouldBe Color(0xFFE3F6EC)
        DarkColors.error shouldBe Color(0xFFE26A74)
        DarkColors.onError shouldBe Color(0xFF2B0A0E)
        DarkColors.errorContainer shouldBe Color(0xFF5C0F17)
        DarkColors.onErrorContainer shouldBe Color(0xFFFFECEE)
        DarkColors.background shouldBe Color(0xFF14171F)
        DarkColors.surface shouldBe Color(0xFF1E2130)
        DarkColors.onSurface shouldBe Color(0xFFE6E8F0)
    }
}
