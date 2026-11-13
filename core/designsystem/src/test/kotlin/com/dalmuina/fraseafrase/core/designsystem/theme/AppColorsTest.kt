package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [AppColors], los tokens de color del mock de referencia que no corresponden a
 * ningún rol estándar de `ColorScheme` de Material 3 (adopt-reference-mock-ui, design - decisión 2).
 * No requieren Compose runtime: comprueban directamente los valores hexadecimales exactos de
 * `design/mock/design-tokens.md`.
 */
class AppColorsTest {

    // Scenario: cada token nuevo de design.md (decisión 2) existe con el valor hexadecimal exacto
    // de design-tokens.md, incluido textSecondary unificado a #5A5670 (no #5A6070)
    @Test
    fun `given app colors when reading new tokens then they match the mock hex values`() {
        AppColors.AccentStrong shouldBe Color(0xFF3A1FC2)
        AppColors.AccentSoft shouldBe Color(0xFFECE7FF)
        AppColors.TextSecondary shouldBe Color(0xFF5A5670)
        AppColors.TextOnLevel shouldBe Color(0xFF3A404D)
        AppColors.Track shouldBe Color(0xFFE4DFF5)
        AppColors.TrackSoft shouldBe Color(0xFFEFECF8)
        AppColors.DashedBorder shouldBe Color(0xFFD3CCF0)
        AppColors.Disabled shouldBe Color(0xFF8A909C)
        AppColors.HintBg shouldBe Color(0xFFFFF1EA)
        AppColors.HintText shouldBe Color(0xFF5C2A12)
        AppColors.RankUpText shouldBe Color(0xFF9A3412)
        AppColors.FabContent shouldBe Color(0xFF2B0E02)
        AppColors.Success shouldBe Color(0xFF0B8A4A)
        AppColors.SuccessBg shouldBe Color(0xFFE3F6EC)
        AppColors.SuccessText shouldBe Color(0xFF08693A)
        AppColors.Error shouldBe Color(0xFFD92D3F)
        AppColors.ErrorBg shouldBe Color(0xFFFFECEE)
        AppColors.ErrorText shouldBe Color(0xFFB4232F)
    }

    // Scenario: el token successStatus del mensaje "Ajustes guardados" existe con el valor
    // hexadecimal exacto de design-tokens.md (adopt-reference-mock-ui, tarea 9.4)
    @Test
    fun `given app colors when reading the success status token then it matches the mock hex value`() {
        AppColors.SuccessStatus shouldBe Color(0xFF1B6B44)
    }
}
