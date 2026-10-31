package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BatchSizeTest {

    // Scenario: Valor por defecto (spec: Tamaño de tanda N)
    @Test
    fun `given no configured size when using the default then the value is 20`() {
        // When
        val size = BatchSize.DEFAULT

        // Then
        size.value shouldBe 20
    }

    // Scenario: Límites inferior y superior se aceptan (spec: Tamaño de tanda N)
    @ParameterizedTest
    @ValueSource(ints = [10, 30])
    fun `given a boundary size when building it then the value is accepted`(value: Int) {
        // When
        val size = BatchSize(value)

        // Then
        size.value shouldBe value
    }

    // Scenario: Valores fuera de rango se rechazan (spec: Tamaño de tanda N)
    @ParameterizedTest
    @ValueSource(ints = [9, 31])
    fun `given a size outside the range when building it then throws IllegalArgumentException`(value: Int) {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            BatchSize(value)
        }
    }
}
