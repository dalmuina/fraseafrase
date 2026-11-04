package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class ReviewScheduleTest {

    // Rows, in order (spec: Próximo repaso según el nivel):
    // - "0,"                 -> Nivel 0 no tiene próximo repaso
    // - "1,2026-03-11"       -> Intervalo del nivel 1
    // - "2,2026-03-13"       -> Intervalo del nivel 2
    // - "3,2026-03-17"       -> Intervalo del nivel 3
    // - "4,2026-03-24"       -> Intervalo del nivel 4
    // - "5,2026-04-09"       -> Intervalo del nivel 5
    @ParameterizedTest(name = "given date 10-03-2026 when computing next review for level {0} then returns {1}")
    @CsvSource(
        "0,",
        "1,2026-03-11",
        "2,2026-03-13",
        "3,2026-03-17",
        "4,2026-03-24",
        "5,2026-04-09",
    )
    fun `given date 10-03-2026 when computing next review for level then returns expected date`(
        level: Int,
        expectedDate: String?,
    ) {
        // Given
        val from = LocalDate.of(2026, 3, 10)

        // When
        val next = ReviewSchedule.nextReviewAfter(Level(level), from)

        // Then
        next shouldBe expectedDate?.let { LocalDate.parse(it) }
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
