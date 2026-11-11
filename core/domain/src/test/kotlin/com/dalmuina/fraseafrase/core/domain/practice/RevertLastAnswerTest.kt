package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RevertLastAnswerTest {

    // Scenario: Revertir un acierto que certificó restaura el nivel y la racha anteriores (spec: spaced-repetition)
    // Scenario: Revertir un acierto devuelve la frase a su posición original (spec: practice-batch)
    @Test
    fun `given a batch of A B where A was the last answer then reverting restores its progress and position`() =
        runTest {
            // Given: A con nivel 1, racha 2, dueAtSession 9; autoevaluación de acierto, sesión actual 10
            val progressBefore = aPhraseProgress(id = "A", level = Level(1), streak = Streak(2), dueAtSession = 9)
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(progressBefore, aPhraseProgress(id = "B")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                    sessionCount = 10,
                ),
            )
            val outcome = AnswerPhrase(practice).invoke(PhraseId("A"), SelfAssessment.HIT)

            // When
            RevertLastAnswer(practice).invoke(outcome.undo)

            // Then
            val snapshot = practice.read()
            snapshot.activeBatch?.order shouldContainExactly listOf(PhraseId("A"), PhraseId("B"))
            snapshot.progress.first { it.id.value == "A" } shouldBe progressBefore
        }

    // Scenario: La reversión no se acumula (spec: spaced-repetition)
    @Test
    fun `given A was answered and then B when reverting A then throws IllegalArgumentException`() = runTest {
        // Given: tanda A, B; se autoevalúa A y después B, la autoevaluación más reciente es la de B
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(aPhraseProgress(id = "A"), aPhraseProgress(id = "B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 10,
            ),
        )
        val answerPhrase = AnswerPhrase(practice)
        val outcomeA = answerPhrase.invoke(PhraseId("A"), SelfAssessment.HIT)
        answerPhrase.invoke(PhraseId("B"), SelfAssessment.HIT)

        // When / Then
        shouldThrow<IllegalArgumentException> {
            RevertLastAnswer(practice).invoke(outcomeA.undo)
        }
    }
}
