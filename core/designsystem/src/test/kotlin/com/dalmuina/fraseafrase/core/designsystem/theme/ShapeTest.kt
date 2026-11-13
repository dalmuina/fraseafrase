package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [AppShapes] (design D2, `openspec/changes/new-style`, radio de tarjeta
 * confirmado por `redesign-app-ui`). No requieren Compose runtime: comprueban directamente el
 * radio de esquina configurado para `large`.
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

    // Scenario: el radio de las tarjetas grandes coincide exactamente con el mock visual, ~24dp
    // (redesign-app-ui, design - Tokens de :core:designsystem)
    @Test
    fun `given app shapes when reading large then corner radius matches the redesign mock`() {
        val density = Density(1f)
        val size = Size(1000f, 1000f)

        val appLargeRadiusPx = AppShapes.large.topStart.toPx(size, density)

        appLargeRadiusPx shouldBe with(density) { 24.dp.toPx() }
    }

    // Scenario: los radios adicionales del mock que no caben en los 5 slots de Shapes de
    // Material 3 (panel/zona discontinua, FAB de volver, controles compactos) coinciden
    // exactamente con design/mock/design-tokens.md (adopt-reference-mock-ui, design - decisión 2)
    @Test
    fun `given app corner shapes when reading extra radii then they match the mock exactly`() {
        val density = Density(1f)
        val size = Size(1000f, 1000f)

        AppCornerShapes.Panel.topStart.toPx(size, density) shouldBe with(density) { 20.dp.toPx() }
        AppCornerShapes.Fab.topStart.toPx(size, density) shouldBe with(density) { 18.dp.toPx() }
        AppCornerShapes.Hint.topStart.toPx(size, density) shouldBe with(density) { 14.dp.toPx() }
    }
}
