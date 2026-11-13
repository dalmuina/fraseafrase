package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [levelBadgeLabel] y [levelBadgeColors] (design D6,
 * replace-batch-closure-with-session-generation; colores por nivel añadidos por
 * adopt-reference-mock-ui, design - decisión 2). No requieren Compose: son funciones puras,
 * probadas aparte de los tests de UI de [DsLevelBadge].
 */
class DsLevelBadgeTest {

    // Scenario: nivel 1 corresponde a Bronce (design D6)
    @Test
    fun `given level 1 when getting label then returns bronce`() {
        levelBadgeLabel(1) shouldBe "Bronce"
    }

    // Scenario: nivel 2 corresponde a Plata (design D6)
    @Test
    fun `given level 2 when getting label then returns plata`() {
        levelBadgeLabel(2) shouldBe "Plata"
    }

    // Scenario: nivel 3 corresponde a Oro (design D6)
    @Test
    fun `given level 3 when getting label then returns oro`() {
        levelBadgeLabel(3) shouldBe "Oro"
    }

    // Scenario: nivel 4 corresponde a Platino (design D6)
    @Test
    fun `given level 4 when getting label then returns platino`() {
        levelBadgeLabel(4) shouldBe "Platino"
    }

    // Scenario: nivel 5 corresponde a Diamante (design D6)
    @Test
    fun `given level 5 when getting label then returns diamante`() {
        levelBadgeLabel(5) shouldBe "Diamante"
    }

    // Scenario: Nivel 0 no muestra insignia (ahora muestra "Sin rango", spec: level-badges, design D3)
    @Test
    fun `given level 0 when getting label then returns sin rango`() {
        levelBadgeLabel(0) shouldBe "Sin rango"
    }

    // Scenario: nivel fuera de rango no tiene insignia (design D6)
    @Test
    fun `given level out of range when getting label then returns null`() {
        levelBadgeLabel(-1) shouldBe null
        levelBadgeLabel(6) shouldBe null
    }

    // Scenario: "Sin rango" no tiene interior, solo el color de su trazo discontinuo, exacto a la
    // tabla "Rangos" de design-tokens.md (spec: level-badges, design - decisión 2)
    @Test
    fun `given level 0 when getting colors then exterior matches disabled token and interior is null`() {
        val colors = levelBadgeColors(0)

        colors?.exterior shouldBe Color(0xFF8A909C)
        colors?.interior shouldBe null
    }

    // Scenario: Bronce usa el exterior/interior exacto de la tabla "Rangos" (spec: level-badges)
    @Test
    fun `given level 1 when getting colors then they match design tokens exactly`() {
        val colors = levelBadgeColors(1)

        colors?.exterior shouldBe Color(0xFFB4693A)
        colors?.interior shouldBe Color(0xFFE3A47C)
    }

    // Scenario: Plata usa el exterior/interior exacto de la tabla "Rangos" (spec: level-badges)
    @Test
    fun `given level 2 when getting colors then they match design tokens exactly`() {
        val colors = levelBadgeColors(2)

        colors?.exterior shouldBe Color(0xFF8E98A5)
        colors?.interior shouldBe Color(0xFFD5DAE1)
    }

    // Scenario: Oro usa el exterior/interior exacto de la tabla "Rangos" (spec: level-badges)
    @Test
    fun `given level 3 when getting colors then they match design tokens exactly`() {
        val colors = levelBadgeColors(3)

        colors?.exterior shouldBe Color(0xFFD9A21B)
        colors?.interior shouldBe Color(0xFFF4CF6B)
    }

    // Scenario: Platino usa el exterior/interior exacto de la tabla "Rangos" (spec: level-badges)
    @Test
    fun `given level 4 when getting colors then they match design tokens exactly`() {
        val colors = levelBadgeColors(4)

        colors?.exterior shouldBe Color(0xFF3E9C9C)
        colors?.interior shouldBe Color(0xFFA9DCDC)
    }

    // Scenario: Diamante usa el color exterior y el de facetas exacto de la tabla "Rangos"
    // (spec: level-badges)
    @Test
    fun `given level 5 when getting colors then they match design tokens exactly`() {
        val colors = levelBadgeColors(5)

        colors?.exterior shouldBe Color(0xFF4F7DF0)
        colors?.interior shouldBe Color(0xFFC9D8FF)
    }

    // Scenario: nivel fuera de rango no tiene colores (design D6)
    @Test
    fun `given level out of range when getting colors then returns null`() {
        levelBadgeColors(-1) shouldBe null
        levelBadgeColors(6) shouldBe null
    }
}
