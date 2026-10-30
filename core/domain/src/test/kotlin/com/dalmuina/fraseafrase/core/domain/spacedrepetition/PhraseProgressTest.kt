package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.testing.FixedClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PhraseProgressTest {

    // Scenario: Racha de tres certifica la frase al cerrar (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a streak of 3 when checking certification then the phrase is certified`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(3),
            answeredInBatch = true,
            nextReview = null,
        )

        // When
        val certified = progress.isCertified

        // Then
        certified shouldBe true
    }

    // Scenario: Racha de dos no certifica la frase al cerrar (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a streak of 2 when checking certification then the phrase is not certified`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(2),
            answeredInBatch = true,
            nextReview = null,
        )

        // When
        val certified = progress.isCertified

        // Then
        certified shouldBe false
    }

    // Scenario: Nivel 0 con próximo repaso se rechaza (spec: Invariantes de PhraseProgress)
    @Test
    fun `given level 0 with a next review when building the phrase then throws IllegalArgumentException`() {
        // Given
        val nextReview = FixedClock(LocalDate.of(2026, 3, 13)).today()

        // When / Then
        shouldThrow<IllegalArgumentException> {
            PhraseProgress(
                id = PhraseId("p-1"),
                level = Level(0),
                streak = Streak(0),
                answeredInBatch = false,
                nextReview = nextReview,
            )
        }
    }

    // Scenario: Nivel 1 sin próximo repaso se rechaza (spec: Invariantes de PhraseProgress)
    @Test
    fun `given level 1 without a next review when building the phrase then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            PhraseProgress(
                id = PhraseId("p-1"),
                level = Level(1),
                streak = Streak(0),
                answeredInBatch = false,
                nextReview = null,
            )
        }
    }

    // Scenario: Racha mayor que 0 sin respuesta se rechaza (spec: Invariantes de PhraseProgress)
    @Test
    fun `given a streak above 0 without answer when building the phrase then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            PhraseProgress(
                id = PhraseId("p-1"),
                level = Level(0),
                streak = Streak(1),
                answeredInBatch = false,
                nextReview = null,
            )
        }
    }
}
