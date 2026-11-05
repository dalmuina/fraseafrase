package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ForceCloseActiveBatchTest {

    private val closingDate: LocalDate = LocalDate.of(2026, 3, 10)

    private fun forceCloseUseCase(practice: FakePracticeRepository) =
        ForceCloseActiveBatch(practice, ResolveBatchClosure(FixedClock(closingDate)))

    // Certificada: racha 3 respondida en la tanda. Sin certificar: nivel 0, nunca respondida.
    private fun certified(id: String) = aPhraseProgress(
        id = id,
        level = Level(1),
        streak = Streak(3),
        answeredInBatch = true,
        nextReview = closingDate.plusDays(1),
    )

    private fun uncertified(id: String) = aPhraseProgress(id = id)

    private fun stateWith(
        progress: List<PhraseProgress>,
        carry: List<String> = emptyList(),
    ) = PracticeSnapshot(
        progress = progress,
        activeBatch = ActiveBatch(order = progress.map { it.id }, capacity = BatchSize(10)),
        carry = CarryQueue(carry.map { PhraseId(it) }),
    )

    // Scenario: Sin tanda activa no hay nada que cerrar (spec: settings-screen)
    @Test
    fun `given no active batch when force closing then the state does not change`() = runTest {
        // Given
        val initial = PracticeSnapshot(
            progress = listOf(uncertified("A")),
            activeBatch = null,
            carry = CarryQueue(emptyList()),
        )
        val practice = FakePracticeRepository(initial)

        // When
        forceCloseUseCase(practice).invoke()

        // Then
        practice.read() shouldBe initial
    }

    // Scenario: Cambiar el nivel CEFR cierra la tanda activa sin aviso (spec: settings-screen)
    @Test
    fun `given an active batch with certified and uncertified phrases when force closing then it closes as expected`() =
        runTest {
            // Given
            val practice = FakePracticeRepository(
                stateWith(listOf(certified("A"), uncertified("B")), carry = listOf("Z")),
            )

            // When
            forceCloseUseCase(practice).invoke()

            // Then
            val state = practice.read()
            state.activeBatch shouldBe null
            state.carry.ids.map { it.value } shouldContainExactly listOf("Z", "B")
            state.progress.first { it.id.value == "A" }.level shouldBe Level(2)
        }
}
