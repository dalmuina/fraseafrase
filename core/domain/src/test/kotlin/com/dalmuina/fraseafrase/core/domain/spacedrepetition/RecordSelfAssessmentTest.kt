package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RecordSelfAssessmentTest {

    private val recordSelfAssessment = RecordSelfAssessment()

    // Scenario: Acierto suma uno a la racha (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a streak of 1 when the user assesses a hit then the streak becomes 2`() {
        // Given
        val progress = progressWith(streak = Streak(1), answeredInBatch = true)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.HIT)

        // Then
        updated.streak shouldBe Streak(2)
    }

    // Scenario: Fallo reinicia la racha (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a streak of 2 when the user assesses a miss then the streak becomes 0`() {
        // Given
        val progress = progressWith(streak = Streak(2), answeredInBatch = true)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.MISS)

        // Then
        updated.streak shouldBe Streak(0)
    }

    // Scenario: Frase con un fallo cuenta como respondida (spec: Frase respondida en la tanda)
    @Test
    fun `given a phrase never answered when the user assesses a miss then the phrase counts as answered`() {
        // Given
        val progress = progressWith(streak = Streak(0), answeredInBatch = false)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.MISS)

        // Then
        updated.answeredInBatch shouldBe true
    }

    // Scenario: Frase con un acierto cuenta como respondida (spec: Frase respondida en la tanda)
    @Test
    fun `given a phrase never answered when the user assesses a hit then the phrase counts as answered`() {
        // Given
        val progress = progressWith(streak = Streak(0), answeredInBatch = false)

        // When
        val updated = recordSelfAssessment(progress, SelfAssessment.HIT)

        // Then
        updated.answeredInBatch shouldBe true
    }

    private fun progressWith(streak: Streak, answeredInBatch: Boolean) = PhraseProgress(
        id = PhraseId("p-1"),
        level = Level(0),
        streak = streak,
        answeredInBatch = answeredInBatch,
        nextReview = null,
    )
}
