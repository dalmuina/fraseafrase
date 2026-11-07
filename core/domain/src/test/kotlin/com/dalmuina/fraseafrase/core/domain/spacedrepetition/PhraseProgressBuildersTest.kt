package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PhraseProgressBuildersTest {

    // Escenario por defecto del builder: construye una frase válida sin argumentos (spec: Frase en un único lugar,
    // precondición de los escenarios de composición; los valores por defecto cumplen las invariantes de PhraseProgress)
    @Test
    fun `given no arguments when building a phrase progress then it is built with valid defaults`() {
        // When
        val progress = aPhraseProgress()

        // Then
        progress.id shouldBe PhraseId("phrase-1")
        progress.level shouldBe Level(0)
    }

    // Scenario: `dueAtSession` negativo se rechaza (spec: Invariantes de PhraseProgress con sesión de
    // vencimiento, en spaced-repetition). Cobertura ya existente en PhraseProgressTest; aquí se comprueba
    // que el builder no la corrige.
    @Test
    fun `given a negative due at session when using the builder then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            aPhraseProgress(dueAtSession = -1)
        }
    }
}
