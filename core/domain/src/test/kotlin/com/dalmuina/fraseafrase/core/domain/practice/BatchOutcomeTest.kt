package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BatchOutcomeTest {

    // Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel (spec: practice-session)
    @Test
    fun `given a fresh batch outcome when created then it starts at zero hits, misses and level ups`() {
        // Given / When
        val outcome = BatchOutcome()

        // Then
        outcome shouldBe BatchOutcome(hits = 0, misses = 0, levelUps = 0)
    }

    // Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel (spec: practice-session)
    @Test
    fun `given a fresh batch outcome when recording a hit without a level up then only hits increase`() {
        // Given
        val outcome = BatchOutcome()

        // When
        val recorded = outcome.record(SelfAssessment.HIT, leveledUp = false)

        // Then
        recorded shouldBe BatchOutcome(hits = 1, misses = 0, levelUps = 0)
    }

    // Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel (spec: practice-session)
    @Test
    fun `given a fresh batch outcome when recording a miss then only misses increase`() {
        // Given
        val outcome = BatchOutcome()

        // When
        val recorded = outcome.record(SelfAssessment.MISS, leveledUp = false)

        // Then
        recorded shouldBe BatchOutcome(hits = 0, misses = 1, levelUps = 0)
    }

    // Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel (spec: practice-session)
    @Test
    fun `given a fresh batch outcome when recording a hit that levels up then hits and level ups increase`() {
        // Given
        val outcome = BatchOutcome()

        // When
        val recorded = outcome.record(SelfAssessment.HIT, leveledUp = true)

        // Then
        recorded shouldBe BatchOutcome(hits = 1, misses = 0, levelUps = 1)
    }

    // Scenario: Una frase que sube y vuelve a bajar de nivel en la misma tanda cuenta como subida
    // (spec: practice-session)
    @Test
    fun `given a phrase that leveled up when it later drops with a miss then level ups does not decrease`() {
        // Given: A sube de nivel con un acierto
        val afterLevelUp = BatchOutcome().record(SelfAssessment.HIT, leveledUp = true)

        // When: A baja de nivel con un fallo posterior
        val afterMiss = afterLevelUp.record(SelfAssessment.MISS, leveledUp = false)

        // Then: la subida sigue contando aunque el nivel final sea igual al inicial
        afterMiss shouldBe BatchOutcome(hits = 1, misses = 1, levelUps = 1)
    }
}
