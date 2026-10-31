package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class CarryQueueTest {

    // Scenario: Cola de arrastradas con ids duplicados (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a carry queue with the same id twice when building it then throws IllegalArgumentException`() {
        // Given
        val a = PhraseId("A")

        // When / Then
        shouldThrow<IllegalArgumentException> {
            CarryQueue(listOf(a, a))
        }
    }
}
