package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecordSelfAssessmentTest {

    private val today = LocalDate.of(2026, 3, 10)
    private val recordSelfAssessment = RecordSelfAssessment(FixedClock(today))

    // Scenario: Acierto suma uno a la racha (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a streak of 1 when the user assesses a hit then the streak becomes 2`() {
        // Given
        val progress = aPhraseProgress(streak = Streak(1), answeredInBatch = true)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.HIT)

        // Then
        updated.streak shouldBe Streak(2)
    }

    // Scenario: Fallo reinicia la racha (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a streak of 2 when the user assesses a miss then the streak becomes 0`() {
        // Given
        val progress = aPhraseProgress(streak = Streak(2), answeredInBatch = true)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.MISS)

        // Then
        updated.streak shouldBe Streak(0)
    }

    // Scenario: Bajada instantánea de nivel ante un fallo (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a phrase of level 2 when the user assesses a miss then its level drops to 1`() {
        // Given
        val progress = aPhraseProgress(
            level = Level(2),
            streak = Streak(1),
            answeredInBatch = true,
            nextReview = LocalDate.of(2026, 3, 9),
        )

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.MISS)

        // Then
        updated.level shouldBe Level(1)
        updated.nextReview shouldBe LocalDate.of(2026, 3, 11)
    }

    // Scenario: Frase con un fallo cuenta como respondida (spec: Frase respondida en la tanda)
    @Test
    fun `given a phrase never answered when the user assesses a miss then the phrase counts as answered`() {
        // Given
        val progress = aPhraseProgress(streak = Streak(0), answeredInBatch = false)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.MISS)

        // Then
        updated.answeredInBatch shouldBe true
    }

    // Scenario: Frase con un acierto cuenta como respondida (spec: Frase respondida en la tanda)
    @Test
    fun `given a phrase never answered when the user assesses a hit then the phrase counts as answered`() {
        // Given
        val progress = aPhraseProgress(streak = Streak(0), answeredInBatch = false)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.HIT)

        // Then
        updated.answeredInBatch shouldBe true
    }
}
