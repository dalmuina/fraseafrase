package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StreakTest {

    // Scenario: Racha negativa se rechaza (spec: Invariantes de PhraseProgress)
    @Test
    fun `given a negative streak when building it then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            Streak(-1)
        }
    }

    // Scenario: Acierto suma uno a la racha (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a streak of 1 when a hit is recorded then the streak becomes 2`() {
        // Given
        val streak = Streak(1)

        // When
        val next = streak.afterHit()

        // Then
        next shouldBe Streak(2)
    }

    // Scenario: Fallo reinicia la racha (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a streak of 2 when a miss is recorded then the streak becomes 0`() {
        // Given
        val streak = Streak(2)

        // When
        val next = streak.afterMiss()

        // Then
        next shouldBe Streak(0)
    }
}
