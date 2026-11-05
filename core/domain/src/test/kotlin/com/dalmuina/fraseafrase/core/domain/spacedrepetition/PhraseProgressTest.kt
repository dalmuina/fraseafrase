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

    // Scenario: Fallo en nivel 3 baja a nivel 2 y recalcula el repaso
    // (spec: Bajada instantánea de nivel ante un fallo)
    @Test
    fun `given a level 3 phrase when recording a miss then it drops to level 2 and reschedules`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(3),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 20),
        )

        // When
        val updated = progress.record(SelfAssessment.MISS, LocalDate.of(2026, 3, 10))

        // Then
        updated.level shouldBe Level(2)
        updated.nextReview shouldBe LocalDate.of(2026, 3, 13)
    }

    // Scenario: Fallo en nivel 1 baja a nivel 0 y elimina el próximo repaso
    // (spec: Bajada instantánea de nivel ante un fallo)
    @Test
    fun `given a level 1 phrase when recording a miss then it drops to level 0 without next review`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(1),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 11),
        )

        // When
        val updated = progress.record(SelfAssessment.MISS, LocalDate.of(2026, 3, 10))

        // Then
        updated.level shouldBe Level(0)
        updated.nextReview shouldBe null
    }

    // Scenario: Fallo en nivel 0 no cambia el nivel
    // (spec: Bajada instantánea de nivel ante un fallo)
    @Test
    fun `given a level 0 phrase when recording a miss then the level and next review stay unchanged`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = null,
        )

        // When
        val updated = progress.record(SelfAssessment.MISS, LocalDate.of(2026, 3, 10))

        // Then
        updated.level shouldBe Level(0)
        updated.nextReview shouldBe null
    }

    // Scenario implícito en "Bajada instantánea de nivel ante un fallo": un acierto no baja el nivel
    @Test
    fun `given a level 3 phrase when recording a hit then the level and next review stay unchanged`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(3),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 20),
        )

        // When
        val updated = progress.record(SelfAssessment.HIT, LocalDate.of(2026, 3, 10))

        // Then
        updated.level shouldBe Level(3)
        updated.nextReview shouldBe LocalDate.of(2026, 3, 20)
    }

    // Scenario: Varios fallos en la misma tanda bajan el nivel de nuevo cada vez
    // (spec: Bajada instantánea de nivel ante un fallo)
    @Test
    fun `given a level 4 phrase when recording two consecutive misses then it drops to level 2`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(4),
            streak = Streak(0),
            answeredInBatch = false,
            nextReview = LocalDate.of(2026, 3, 20),
        )
        val today = LocalDate.of(2026, 3, 10)

        // When
        val afterFirstMiss = progress.record(SelfAssessment.MISS, today)
        val afterSecondMiss = afterFirstMiss.record(SelfAssessment.MISS, today)

        // Then
        afterSecondMiss.level shouldBe Level(2)
        afterSecondMiss.nextReview shouldBe LocalDate.of(2026, 3, 13)
    }
}
