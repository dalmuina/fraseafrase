package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CloseActiveBatchTest {

    private val closingDate: LocalDate = LocalDate.of(2026, 3, 10)

    private fun closeUseCase(practice: FakePracticeRepository) =
        CloseActiveBatch(practice, ResolveBatchClosure(FixedClock(closingDate)))

    // Certificada: racha 3 respondida en la tanda. Sin certificar: nivel 0, nunca respondida.
    private fun certified(id: String) = aPhraseProgress(
        id = id,
        level = 1,
        streak = 3,
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

    // Scenario: Cierre con el número confirmado (spec: practice-persistence)
    @Test
    fun `given a batch with one uncertified phrase when closing with expectation 1 then returns Closed`() = runTest {
        // Given
        val practice = FakePracticeRepository(stateWith(listOf(certified("A"), uncertified("B"))))

        // When
        val result = closeUseCase(practice).invoke(expectedUncertified = 1)

        // Then
        result shouldBe CloseBatchResult.Closed
    }

    // Scenario: Cierre con el número confirmado: la tanda queda sin activar y la cola recibe la no certificada
    @Test
    fun `given a batch with one uncertified phrase when closing then the batch is inactive and the queue holds it`() =
        runTest {
            // Given
            val practice = FakePracticeRepository(stateWith(listOf(certified("A"), uncertified("B"))))

            // When
            closeUseCase(practice).invoke(expectedUncertified = 1)

            // Then
            val state = practice.read()
            state.activeBatch shouldBe null
            state.carry.ids.map { it.value } shouldContainExactly listOf("B")
        }

    // Scenario: Cierre sin tanda activa tiene prioridad sobre la expectativa (spec: practice-persistence)
    @Test
    fun `given no active batch when closing with expectation 2 then returns NoActiveBatch and keeps the state`() =
        runTest {
            // Given
            val initial = PracticeSnapshot(
                progress = listOf(uncertified("A")),
                activeBatch = null,
                carry = CarryQueue(emptyList()),
            )
            val practice = FakePracticeRepository(initial)

            // When
            val result = closeUseCase(practice).invoke(expectedUncertified = 2)

            // Then
            result shouldBe CloseBatchResult.NoActiveBatch
            practice.read() shouldBe initial
        }

    // Scenario: Cambio entre el aviso y el cierre (spec: practice-persistence)
    @Test
    fun `given three uncertified phrases when closing with expectation 2 then returns Stale and keeps the state`() =
        runTest {
            // Given
            val initial = stateWith(listOf(uncertified("A"), uncertified("B"), uncertified("C")))
            val practice = FakePracticeRepository(initial)

            // When
            val result = closeUseCase(practice).invoke(expectedUncertified = 2)

            // Then
            result shouldBe CloseBatchResult.Stale
            practice.read() shouldBe initial
        }

    // Scenario: Cierre no compone la tanda siguiente (spec: practice-persistence, "Composición única tras el cierre")
    @Test
    fun `given a batch with two certified phrases when closing then no next batch is composed`() = runTest {
        // Given
        val practice = FakePracticeRepository(stateWith(listOf(certified("A"), certified("B"))))

        // When
        val result = closeUseCase(practice).invoke(expectedUncertified = 0)

        // Then: el cierre no devuelve ni guarda tanda nueva
        result shouldBe CloseBatchResult.Closed
        practice.read().activeBatch shouldBe null
    }

    // Scenario: Cierre sin composición posible deja la tanda sin activar (spec: practice-persistence)
    @Test
    fun `given a batch with two certified phrases and an empty queue when closing then the queue stays empty`() =
        runTest {
            // Given
            val practice = FakePracticeRepository(stateWith(listOf(certified("A"), certified("B"))))

            // When
            closeUseCase(practice).invoke(expectedUncertified = 0)

            // Then
            practice.read().carry.ids.shouldBeEmpty()
        }

    // Scenario: El aviso coincide con las arrastradas del cierre (spec: practice-persistence)
    @Test
    fun `given two uncertified phrases when closing with expectation 2 then the queue holds them in batch order`() =
        runTest {
            // Given
            val practice = FakePracticeRepository(stateWith(listOf(uncertified("X"), certified("C"), uncertified("Y"))))

            // When
            val result = closeUseCase(practice).invoke(expectedUncertified = 2)

            // Then
            result shouldBe CloseBatchResult.Closed
            practice.read().carry.ids.map { it.value } shouldContainExactly listOf("X", "Y")
        }

    // Scenario: Cierre con sobrante: la cola sobrante va antes que las arrastradas del cierre (spec: practice-batch)
    @Test
    fun `given a leftover queue when closing then the leftover goes before the phrases carried by the closure`() =
        runTest {
            // Given
            val practice = FakePracticeRepository(stateWith(listOf(uncertified("B")), carry = listOf("Z")))

            // When
            closeUseCase(practice).invoke(expectedUncertified = 1)

            // Then
            practice.read().carry.ids.map { it.value } shouldContainExactly listOf("Z", "B")
        }
}
