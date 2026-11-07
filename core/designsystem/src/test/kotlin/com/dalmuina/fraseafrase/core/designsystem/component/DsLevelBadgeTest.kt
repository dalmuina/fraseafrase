package com.dalmuina.fraseafrase.core.designsystem.component

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tests unitarios de [levelBadgeLabel] (design D6, replace-batch-closure-with-session-generation).
 * No requieren Compose: es una función pura, probada aparte de los tests de UI de [DsLevelBadge].
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
}
