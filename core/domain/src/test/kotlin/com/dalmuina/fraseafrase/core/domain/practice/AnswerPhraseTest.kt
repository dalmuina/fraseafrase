package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AnswerPhraseTest {

    private fun batchOf(vararg ids: String, sessionCount: Int = 5) = PracticeSnapshot(
        progress = ids.map { aPhraseProgress(id = it) },
        activeBatch = ActiveBatch(order = ids.map { PhraseId(it) }, capacity = BatchSize(10)),
        sessionCount = sessionCount,
    )

    // Scenario: Acierto mueve la frase al final (spec: practice-batch)
    @Test
    fun `given a batch of A, B and C when answering A with a hit then the order becomes B, C, A`() = runTest {
        // Given
        val practice = FakePracticeRepository(batchOf("A", "B", "C"))

        // When
        AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        practice.read().activeBatch?.order?.map { it.value } shouldBe listOf("B", "C", "A")
    }

    @Test
    fun `given a batch when answering a hit then the phrase streak grows and it counts as answered`() = runTest {
        // Given
        val practice = FakePracticeRepository(batchOf("A", "B"))

        // When
        AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        val answered = practice.read().progress.first { it.id.value == "A" }
        answered.streak.value shouldBe 1
        answered.answeredInBatch shouldBe true
    }

    // Scenario: Bajada instantánea de nivel ante un fallo (spec: spaced-repetition)
    @Test
    fun `given a level 2 phrase when answering with a miss then its level drops and dueAtSession is next session`() =
        runTest {
            // Given: contador de sesiones en 5, frase A de nivel 2
            val progress = aPhraseProgress(id = "A", level = Level(2), dueAtSession = 20)
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(progress, aPhraseProgress(id = "B")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                    sessionCount = 5,
                ),
            )

            // When
            AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.MISS)

            // Then
            val answered = practice.read().progress.first { it.id.value == "A" }
            answered.level shouldBe Level(1)
            answered.dueAtSession shouldBe 6
        }

    // Scenario: Responder una frase que no está en la tanda (spec: practice-batch)
    @Test
    fun `given a batch of A and B when answering C then throws IllegalArgumentException`() = runTest {
        // Given
        val practice = FakePracticeRepository(batchOf("A", "B"))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            AnswerPhrase(practice).invoke(PhraseId("C"), SelfAssessment.HIT)
        }
    }

    @Test
    fun `given no active batch when answering then throws IllegalArgumentException`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When / Then
        shouldThrow<IllegalArgumentException> {
            AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)
        }
    }

    // Scenario: AnswerPhrase expone el nivel resultante (spec: practice-batch)
    @Test
    fun `given a hit that certifies the phrase when answering then it returns the new level`() = runTest {
        // Given: streak a una de certificar, nivel 0
        val progress = aPhraseProgress(id = "A", level = Level(0), streak = Streak(2))
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(progress, aPhraseProgress(id = "B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 5,
            ),
        )

        // When
        val result = AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        result shouldBe Level(1)
    }

    @Test
    fun `given a hit that does not certify the phrase when answering then it returns the same level`() = runTest {
        // Given: streak lejos de certificar, nivel 2
        val progress = aPhraseProgress(id = "A", level = Level(2), streak = Streak(0))
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(progress, aPhraseProgress(id = "B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 5,
            ),
        )

        // When
        val result = AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        result shouldBe Level(2)
    }

    @Test
    fun `given a miss on a phrase with level 1 or higher when answering then it returns the dropped level`() =
        runTest {
            // Given: nivel 2
            val progress = aPhraseProgress(id = "A", level = Level(2))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(progress, aPhraseProgress(id = "B")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                    sessionCount = 5,
                ),
            )

            // When
            val result = AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.MISS)

            // Then
            result shouldBe Level(1)
        }

    @Test
    fun `given a miss on a phrase at level 0 when answering then it returns level 0`() = runTest {
        // Given: nivel 0
        val progress = aPhraseProgress(id = "A", level = Level(0))
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(progress, aPhraseProgress(id = "B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 5,
            ),
        )

        // When
        val result = AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.MISS)

        // Then
        result shouldBe Level(0)
    }

    // Scenario: Certificar de nuevo en nivel máximo se mantiene en 5 (ADR-0015)
    @Test
    fun `given a hit that certifies the phrase at max level when answering then it returns level 5`() = runTest {
        // Given: nivel máximo, streak a una de certificar de nuevo
        val progress = aPhraseProgress(id = "A", level = Level(5), streak = Streak(2))
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(progress, aPhraseProgress(id = "B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 5,
            ),
        )

        // When
        val result = AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)

        // Then
        result shouldBe Level(5)
    }
}
