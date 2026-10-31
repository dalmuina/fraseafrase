package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Test de integración de dominio del ciclo cierre → composición (ADR-0012, design D4 y D9).
 */
class PracticeCycleTest {

    private val closingDate = LocalDate.of(2026, 3, 10)
    private val resolveBatchClosure = ResolveBatchClosure(FixedClock(closingDate))

    // Scenario: Arrastradas conservan el orden de la lista al cerrar (spec: Cola de arrastradas ordenada)
    @Test
    fun `given batch B C A with A and C uncertified when closing then the carry queue is C A`() {
        // Given
        val batch = listOf(certified("B"), uncertified("C"), uncertified("A"))

        // When
        val carry = CarryQueue.afterClosure(CarryQueue(emptyList()), resolveBatchClosure(batch))

        // Then
        carry.ids shouldBe ids("C", "A")
    }

    // Scenario: La cola sobrante va antes que las arrastradas del cierre (spec: Ciclo de cierre y composición)
    @Test
    fun `given leftover Z and batch B C A with A and C uncertified when closing then the next queue is Z C A`() {
        // Given
        val leftover = CarryQueue(ids("Z"))
        val batch = listOf(certified("B"), uncertified("C"), uncertified("A"))

        // When
        val carry = CarryQueue.afterClosure(leftover, resolveBatchClosure(batch))

        // Then
        carry.ids shouldBe ids("Z", "C", "A")
    }

    // Scenario: El aviso coincide con las arrastradas del cierre (spec: Ciclo de cierre y composición)
    @Test
    fun `given five phrases with two certified when closing then the warning is three and the carry has three`() {
        // Given
        val batch = PracticeBatch(
            entries = listOf(
                certified("P1"),
                uncertified("P2"),
                certified("P3"),
                uncertified("P4"),
                uncertified("P5"),
            ),
            capacity = BatchSize.DEFAULT,
        )

        // When
        val warning = batch.uncertifiedCount()
        val carry = CarryQueue.afterClosure(CarryQueue(emptyList()), resolveBatchClosure(batch.entries))

        // Then
        warning shouldBe 3
        carry.ids shouldBe ids("P2", "P4", "P5")
    }

    // Scenario: Cierre con un id ya en la cola (spec: Ciclo de cierre y composición)
    @Test
    fun `given leftover Z and a closing that carries Z again then throws IllegalArgumentException`() {
        // Given
        val leftover = CarryQueue(ids("Z"))
        val outcomes = resolveBatchClosure(listOf(uncertified("Z")))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            CarryQueue.afterClosure(leftover, outcomes)
        }
    }

    // Scenario: La cola sobrante entra primera tras el cierre (spec: Ciclo de cierre y composición)
    @Test
    fun `given carry Z C A and nine pending phrases when composing on 10-03 then the batch starts with Z C A and P1`() {
        // Given
        val carried = listOf("Z", "C", "A").map { aPhraseProgress(id = it) }
        val pending = (1..9).map { aPhraseProgress(id = "P$it", level = 1, nextReview = LocalDate.of(2026, 3, 9)) }
        val catalog: List<CatalogPhrase> = (listOf("Z", "C", "A") + pending.map { it.id.value })
            .map { aCatalogPhrase(id = it, level = CefrLevel.A1) }

        // When
        val result = ComposeNextBatch(FixedClock(LocalDate.of(2026, 3, 10)))(
            carry = CarryQueue(ids("Z", "C", "A")),
            progress = carried + pending,
            catalog = catalog,
            cefrLevel = CefrLevel.A1,
            size = BatchSize(10),
        )

        // Then
        val batch = result.shouldBeInstanceOf<Composed>().batch
        batch.entries.take(4).map { it.id.value } shouldContainExactly listOf("Z", "C", "A", "P1")
    }

    private fun certified(id: String) = aPhraseProgress(id = id, streak = 3, answeredInBatch = true)

    private fun uncertified(id: String) = aPhraseProgress(id = id)

    private fun ids(vararg values: String): List<PhraseId> = values.map { PhraseId(it) }
}
