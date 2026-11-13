package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de la tipografía de [AppTheme]: Space Grotesk + Inter con los tamaños, pesos,
 * tracking y alto de línea exactos de `design/mock/design-tokens.md`, sustituyendo Baloo 2 + Plus
 * Jakarta Sans (adopt-reference-mock-ui, design - decisión 1 y 2). No requiere Compose runtime:
 * comprueba directamente la `FontFamily`, `fontSize`, `fontWeight`, `letterSpacing` y
 * `lineHeight` de [AppTypography]. Sin escenario de spec propio: cambio de tipografía puramente
 * visual (tasks.md 2.4).
 */
class TypeTest {

    // Scenario: título de pantalla 28sp/700/tracking -0.5 en Space Grotesk
    @Test
    fun `given app typography when reading headline large then it matches the mock screen title`() {
        AppTypography.headlineLarge.fontFamily shouldBe SpaceGroteskFamily
        AppTypography.headlineLarge.fontSize shouldBe 28.sp
        AppTypography.headlineLarge.fontWeight shouldBe FontWeight.Bold
        AppTypography.headlineLarge.letterSpacing shouldBe (-0.5).sp
    }

    // Scenario: frase a traducir corta 28sp/600/lh 1.25/tracking -0.3 en Space Grotesk
    @Test
    fun `given app typography when reading headline medium then it matches the mock short phrase`() {
        AppTypography.headlineMedium.fontFamily shouldBe SpaceGroteskFamily
        AppTypography.headlineMedium.fontSize shouldBe 28.sp
        AppTypography.headlineMedium.fontWeight shouldBe FontWeight.SemiBold
        AppTypography.headlineMedium.lineHeight shouldBe 35.sp
        AppTypography.headlineMedium.letterSpacing shouldBe (-0.3).sp
    }

    // Scenario: frase a traducir larga 22sp/600/lh 1.3 en Space Grotesk, sin tracking
    @Test
    fun `given app typography when reading headline small then it matches the mock long phrase`() {
        AppTypography.headlineSmall.fontFamily shouldBe SpaceGroteskFamily
        AppTypography.headlineSmall.fontSize shouldBe 22.sp
        AppTypography.headlineSmall.fontWeight shouldBe FontWeight.SemiBold
        AppTypography.headlineSmall.lineHeight shouldBe 28.6.sp
        AppTypography.headlineSmall.letterSpacing shouldBe 0.sp
    }

    // Scenario: número grande (Progreso) 48sp/700/lh 1.1 en Space Grotesk, sin tracking
    @Test
    fun `given app typography when reading display large then it matches the mock big number`() {
        AppTypography.displayLarge.fontFamily shouldBe SpaceGroteskFamily
        AppTypography.displayLarge.fontSize shouldBe 48.sp
        AppTypography.displayLarge.fontWeight shouldBe FontWeight.Bold
        AppTypography.displayLarge.lineHeight shouldBe 52.8.sp
        AppTypography.displayLarge.letterSpacing shouldBe 0.sp
    }

    // Scenario: número de estadística / campo numérico 24sp/700 en Space Grotesk
    @Test
    fun `given app typography when reading title large then it matches the mock stat number`() {
        AppTypography.titleLarge.fontFamily shouldBe SpaceGroteskFamily
        AppTypography.titleLarge.fontSize shouldBe 24.sp
        AppTypography.titleLarge.fontWeight shouldBe FontWeight.Bold
        AppTypography.titleLarge.letterSpacing shouldBe 0.sp
    }

    // Scenario: botón primario / Fallo / Acierto 17sp/600 en Inter
    @Test
    fun `given app typography when reading title medium then it matches the mock button text`() {
        AppTypography.titleMedium.fontFamily shouldBe InterFamily
        AppTypography.titleMedium.fontSize shouldBe 17.sp
        AppTypography.titleMedium.fontWeight shouldBe FontWeight.SemiBold
        AppTypography.titleMedium.letterSpacing shouldBe 0.sp
    }

    // Scenario: traducción 17sp/500/lh 1.4 en Inter
    @Test
    fun `given app typography when reading title small then it matches the mock translation text`() {
        AppTypography.titleSmall.fontFamily shouldBe InterFamily
        AppTypography.titleSmall.fontSize shouldBe 17.sp
        AppTypography.titleSmall.fontWeight shouldBe FontWeight.Medium
        AppTypography.titleSmall.lineHeight shouldBe 23.8.sp
    }

    // Scenario: contador "Frase 3 de 10" 15sp/600 en Inter
    @Test
    fun `given app typography when reading body large then it matches the mock counter text`() {
        AppTypography.bodyLarge.fontFamily shouldBe InterFamily
        AppTypography.bodyLarge.fontSize shouldBe 15.sp
        AppTypography.bodyLarge.fontWeight shouldBe FontWeight.SemiBold
        AppTypography.bodyLarge.letterSpacing shouldBe 0.sp
    }

    // Scenario: texto de ayuda / mensaje de rango 14sp/400/lh 1.4-1.5 en Inter
    @Test
    fun `given app typography when reading body medium then it matches the mock hint text`() {
        AppTypography.bodyMedium.fontFamily shouldBe InterFamily
        AppTypography.bodyMedium.fontSize shouldBe 14.sp
        AppTypography.bodyMedium.fontWeight shouldBe FontWeight.Normal
        AppTypography.bodyMedium.lineHeight shouldBe 19.6.sp
    }

    // Scenario: chips de nivel y rango 13sp/700 en Inter
    @Test
    fun `given app typography when reading label large then it matches the mock chip text`() {
        AppTypography.labelLarge.fontFamily shouldBe InterFamily
        AppTypography.labelLarge.fontSize shouldBe 13.sp
        AppTypography.labelLarge.fontWeight shouldBe FontWeight.Bold
    }

    // Scenario: etiqueta "TRADUCCIÓN" / etiqueta del nav 12sp/700 (activo) en Inter
    @Test
    fun `given app typography when reading label medium then it matches the mock label text`() {
        AppTypography.labelMedium.fontFamily shouldBe InterFamily
        AppTypography.labelMedium.fontSize shouldBe 12.sp
        AppTypography.labelMedium.fontWeight shouldBe FontWeight.Bold
    }
}
