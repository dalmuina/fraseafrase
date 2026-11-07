package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import io.kotest.matchers.comparables.shouldBeGreaterThan
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [AppShapes] (design D2, `openspec/changes/new-style`). No requieren Compose
 * runtime: comprueban directamente el radio de esquina configurado para `large`.
 */
class ShapeTest {

    // Scenario: las tarjetas usan esquinas muy redondeadas, mayores que el valor por defecto de
    // Material 3 (design D2)
    @Test
    fun `given app shapes when reading large then corner radius is bigger than Material3 default`() {
        val density = Density(1f)
        val size = Size(1000f, 1000f)

        val defaultLargeRadiusPx = Shapes().large.topStart.toPx(size, density)
        val appLargeRadiusPx = AppShapes.large.topStart.toPx(size, density)

        appLargeRadiusPx shouldBeGreaterThan defaultLargeRadiusPx
    }
}
