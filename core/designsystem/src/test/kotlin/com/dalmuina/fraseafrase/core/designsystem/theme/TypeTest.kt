package com.dalmuina.fraseafrase.core.designsystem.theme

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de la tipografía de [AppTheme] (design D4, `openspec/changes/new-style`). No
 * requieren Compose runtime: comprueban directamente la `FontFamily` asignada en [AppTypography].
 */
class TypeTest {

    // Scenario: los titulares usan la familia Baloo 2 empaquetada (design D4)
    @Test
    fun `given app typography when reading title large then font family is Baloo2`() {
        AppTypography.titleLarge.fontFamily shouldBe Baloo2Family
    }
}
