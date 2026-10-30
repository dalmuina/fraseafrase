package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReviewScheduleTest {

    // Scenario: Intervalo del nivel 1 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 10-03-2026 when computing next review for level 1 then returns 11-03-2026`() {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(1), from)

        // Then
        next shouldBe LocalDate.of(2026, 3, 11)
    }

    // Scenario: Intervalo del nivel 2 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 10-03-2026 when computing next review for level 2 then returns 13-03-2026`() {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(2), from)

        // Then
        next shouldBe LocalDate.of(2026, 3, 13)
    }

    // Scenario: Intervalo del nivel 3 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 10-03-2026 when computing next review for level 3 then returns 17-03-2026`() {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(3), from)

        // Then
        next shouldBe LocalDate.of(2026, 3, 17)
    }

    // Scenario: Intervalo del nivel 4 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 10-03-2026 when computing next review for level 4 then returns 24-03-2026`() {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(4), from)

        // Then
        next shouldBe LocalDate.of(2026, 3, 24)
    }

    // Scenario: Intervalo del nivel 5 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 10-03-2026 when computing next review for level 5 then returns 09-04-2026`() {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(5), from)

        // Then
        next shouldBe LocalDate.of(2026, 4, 9)
    }

    // Scenario: Nivel 0 no tiene próximo repaso (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 10-03-2026 when computing next review for level 0 then returns null`() {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(0), from)

        // Then
        next shouldBe null
    }

    // Scenario: Cambio de año en el intervalo del nivel 1 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 31-12-2026 when computing next review for level 1 then returns 01-01-2027`() {
        // Given
        val from = LocalDate.of(2026, 12, 31)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(1), from)

        // Then
        next shouldBe LocalDate.of(2027, 1, 1)
    }

    // Scenario: Cambio de año en el intervalo del nivel 5 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 15-12-2026 when computing next review for level 5 then returns 14-01-2027`() {
        // Given
        val from = LocalDate.of(2026, 12, 15)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(5), from)

        // Then
        next shouldBe LocalDate.of(2027, 1, 14)
    }

    // Scenario: Año bisiesto en el intervalo del nivel 1 (spec: Próximo repaso según el nivel)
    @Test
    fun `given date 28-02-2028 when computing next review for level 1 then returns 29-02-2028`() {
        // Given
        val from = LocalDate.of(2028, 2, 28)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(1), from)

        // Then
        next shouldBe LocalDate.of(2028, 2, 29)
    }
}
