package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AnswerPhraseTest {

    private val clock = FixedClock(LocalDate.of(2026, 3, 10))

    private fun batchOf(vararg ids: String) = PracticeSnapshot(
        progress = ids.map { aPhraseProgress(id = it) },
        activeBatch = ActiveBatch(order = ids.map { PhraseId(it) }, capacity = BatchSize(10)),
        carry = CarryQueue(emptyList()),
    )

    // Scenario: Acierto mueve la frase al final (spec: practice-batch)
    @Test
    fun `given a batch of A, B and C when answering A with a hit then the order becomes B, C, A`() = runTest {
        // Given
        val practice = FakePracticeRepository(batchOf("A", "B", "C"))

        // When
        AnswerPhrase(practice, clock).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        practice.read().activeBatch?.order?.map { it.value } shouldBe listOf("B", "C", "A")
    }

    @Test
    fun `given a batch when answering a hit then the phrase streak grows and it counts as answered`() = runTest {
        // Given
        val practice = FakePracticeRepository(batchOf("A", "B"))

        // When
        AnswerPhrase(practice, clock).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        val answered = practice.read().progress.first { it.id.value == "A" }
        answered.streak.value shouldBe 1
        answered.answeredInBatch shouldBe true
    }

    // Scenario: Bajada instantánea de nivel ante un fallo (spec: spaced-repetition)
    @Test
    fun `given a level 2 phrase when answering with a miss then its level drops and next review is recalculated`() =
        runTest {
            // Given
            val progress = aPhraseProgress(id = "A", level = Level(2), nextReview = LocalDate.of(2026, 3, 20))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(progress, aPhraseProgress(id = "B")),
                    activeBatch = ActiveBatch(
                        order = listOf(PhraseId("A"), PhraseId("B")),
                        capacity = BatchSize(10),
                    ),
                    carry = CarryQueue(emptyList()),
                ),
            )

            // When
            AnswerPhrase(practice, clock).invoke(PhraseId("A"), SelfAssessment.MISS)

            // Then
            val answered = practice.read().progress.first { it.id.value == "A" }
            answered.level shouldBe Level(1)
            answered.nextReview shouldBe LocalDate.of(2026, 3, 11)
        }

    // Scenario: Responder una frase que no está en la tanda (spec: practice-batch)
    @Test
    fun `given a batch of A and B when answering C then throws IllegalArgumentException`() = runTest {
        // Given
        val practice = FakePracticeRepository(batchOf("A", "B"))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            AnswerPhrase(practice, clock).invoke(PhraseId("C"), SelfAssessment.HIT)
        }
    }

    @Test
    fun `given no active batch when answering then throws IllegalArgumentException`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When / Then
        shouldThrow<IllegalArgumentException> {
            AnswerPhrase(practice, clock).invoke(PhraseId("A"), SelfAssessment.HIT)
        }
    }
}
