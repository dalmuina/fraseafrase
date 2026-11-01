package com.dalmuina.fraseafrase.ui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StartDestinationTest {

    // Scenario: la pantalla de práctica es el destino inicial (design.md, estrategia de test: "Destino inicial")
    @Test
    fun `given the start destination when reading it then it is the practice screen`() {
        // Given / When: el destino inicial declarado por :app
        val destination = PRACTICE_START_DESTINATION

        // Then: identifica la pantalla de práctica
        destination shouldBe "practice"
    }
}
