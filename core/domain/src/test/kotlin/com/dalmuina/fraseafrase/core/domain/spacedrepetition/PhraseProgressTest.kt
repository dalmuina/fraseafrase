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

    // Scenario: Repaso de hoy está pendiente (spec: Frase pendiente de repaso)
    @Test
    fun `given a level 2 phrase due today when checking pending today then it is pending`() {
        // Given
        val today = LocalDate.of(2026, 3, 13)
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(2),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 13),
        )

        // When
        val pending = progress.isPendingOn(today)

        // Then
        pending shouldBe true
    }

    // Caso límite de "Repaso de hoy está pendiente": el próximo repaso es exactamente el día evaluado
    // (spec: Frase pendiente de repaso, "su próximo repaso es igual o anterior a today")
    @Test
    fun `given a level 1 phrase due exactly on the evaluated date when checking pending then it is pending`() {
        // Given
        val today = LocalDate.of(2026, 3, 13)
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(1),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = today,
        )

        // When
        val pending = progress.isPendingOn(today)

        // Then
        pending shouldBe true
    }

    // Scenario: Repaso futuro no está pendiente (spec: Frase pendiente de repaso)
    @Test
    fun `given a level 2 phrase due tomorrow when checking pending today then it is not pending`() {
        // Given
        val today = LocalDate.of(2026, 3, 13)
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(2),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 14),
        )

        // When
        val pending = progress.isPendingOn(today)

        // Then
        pending shouldBe false
    }

    // Scenario: Cambio de día hace pendiente una frase (spec: Frase pendiente de repaso)
    @Test
    fun `given a level 2 phrase due on 13 03 when checking 12 03 and 13 03 then only 13 03 is pending`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(2),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 13),
        )

        // When
        val pendingOnTheTwelfth = progress.isPendingOn(LocalDate.of(2026, 3, 12))
        val pendingOnTheThirteenth = progress.isPendingOn(LocalDate.of(2026, 3, 13))

        // Then
        pendingOnTheTwelfth shouldBe false
        pendingOnTheThirteenth shouldBe true
    }

    // Scenario: Nivel 0 nunca está pendiente (spec: Frase pendiente de repaso)
    @Test
    fun `given a level 0 phrase without next review when checking pending then it is never pending`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = null,
        )

        // When
        val pending = progress.isPendingOn(LocalDate.of(2026, 3, 13))

        // Then
        pending shouldBe false
    }
}
