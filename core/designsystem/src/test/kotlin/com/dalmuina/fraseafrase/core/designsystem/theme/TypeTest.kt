package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de la tipografía de [AppTheme] (design D4 del cambio `new-style`, tamaños/peso/
 * `letterSpacing` actualizados por `redesign-app-ui` para acercarse al ritmo visual de Space
 * Grotesk/Inter del mock sin cambiar de familia). No requieren Compose runtime: comprueban
 * directamente la `FontFamily`, `fontSize`, `fontWeight` y `letterSpacing` de [AppTypography].
 */
class TypeTest {

    // Scenario: los titulares usan la familia Baloo 2 empaquetada (design D4)
    @Test
    fun `given app typography when reading title large then font family is Baloo2`() {
        AppTypography.titleLarge.fontFamily shouldBe Baloo2Family
    }

    // Scenario: los titulares (Baloo 2) adoptan tamaño, peso y letterSpacing negativo del mock
    // visual (redesign-app-ui, design - Tokens de :core:designsystem)
    @Test
    fun `given app typography when reading headline styles then they match the redesign mock`() {
        AppTypography.displayLarge.fontFamily shouldBe Baloo2Family
        AppTypography.displayLarge.fontSize shouldBe 48.sp
        AppTypography.displayLarge.fontWeight shouldBe FontWeight.Bold
        AppTypography.displayLarge.letterSpacing shouldBe (-0.5).sp

        AppTypography.headlineLarge.fontFamily shouldBe Baloo2Family
        AppTypography.headlineLarge.fontSize shouldBe 28.sp
        AppTypography.headlineLarge.fontWeight shouldBe FontWeight.Bold
        AppTypography.headlineLarge.letterSpacing shouldBe (-0.4).sp

        AppTypography.titleLarge.fontSize shouldBe 22.sp
        AppTypography.titleLarge.fontWeight shouldBe FontWeight.SemiBold
        AppTypography.titleLarge.letterSpacing shouldBe (-0.3).sp
    }

    // Scenario: el cuerpo (Plus Jakarta Sans) adopta tamaño y letterSpacing del mock visual
    // (redesign-app-ui, design - Tokens de :core:designsystem)
    @Test
    fun `given app typography when reading body styles then they match the redesign mock`() {
        AppTypography.titleMedium.fontFamily shouldBe PlusJakartaSansFamily
        AppTypography.titleMedium.fontSize shouldBe 17.sp
        AppTypography.titleMedium.fontWeight shouldBe FontWeight.SemiBold
        AppTypography.titleMedium.letterSpacing shouldBe 0.sp

        AppTypography.bodyLarge.fontFamily shouldBe PlusJakartaSansFamily
        AppTypography.bodyLarge.fontSize shouldBe 15.sp
        AppTypography.bodyLarge.fontWeight shouldBe FontWeight.Normal
        AppTypography.bodyLarge.letterSpacing shouldBe 0.sp
    }
}
