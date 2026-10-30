package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LevelTest {

    // Scenario: Nivel fuera de rango se rechaza (spec: Invariantes de PhraseProgress)
    @ParameterizedTest
    @ValueSource(ints = [-1, 6])
    fun `given a level outside the range when building it then throws IllegalArgumentException`(value: Int) {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            Level(value)
        }
    }

    // Scenario: Niveles límite se aceptan (spec: Invariantes de PhraseProgress)
    @ParameterizedTest
    @ValueSource(ints = [0, 5])
    fun `given a level at a boundary when building it then keeps its value`(value: Int) {
        // When
        val level = Level(value)

        // Then
        level.value shouldBe value
    }

    @Test
    fun `given a value inside the range when building it then the value is accepted`() {
        // When
        val level = Level(Level.MAX_LEVEL)

        // Then
        level.value shouldBe Level.MAX_LEVEL
    }
}
