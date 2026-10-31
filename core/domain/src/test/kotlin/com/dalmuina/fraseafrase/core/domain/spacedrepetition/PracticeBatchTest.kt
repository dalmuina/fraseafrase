package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PracticeBatchTest {

    // Scenario: Acierto mueve la frase al final (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when the user hits A then the order becomes B C A`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch.answer(PhraseId("A"), SelfAssessment.HIT)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("B"), PhraseId("C"), PhraseId("A"))
    }

    // Scenario: Fallo mueve la frase al final (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when the user misses B then the order becomes A C B`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch.answer(PhraseId("B"), SelfAssessment.MISS)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("A"), PhraseId("C"), PhraseId("B"))
    }

    // Scenario: Responder la última frase no cambia el orden (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when the user answers C then the order stays A B C`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch.answer(PhraseId("C"), SelfAssessment.HIT)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("A"), PhraseId("B"), PhraseId("C"))
    }

    // Scenario: Varias respuestas mantienen las mismas entradas (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when answering several times then it keeps the same entries in order C A B`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch
            .answer(PhraseId("A"), SelfAssessment.HIT)
            .answer(PhraseId("B"), SelfAssessment.MISS)
            .answer(PhraseId("C"), SelfAssessment.HIT)
            .answer(PhraseId("A"), SelfAssessment.MISS)
            .answer(PhraseId("B"), SelfAssessment.HIT)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("C"), PhraseId("A"), PhraseId("B"))
    }

    // Scenario: Responder una frase que no está en la tanda (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a batch of A and B when answering C then throws IllegalArgumentException`() {
        // Given
        val batch = batchOf("A", "B")

        // When / Then
        shouldThrow<IllegalArgumentException> {
            batch.answer(PhraseId("C"), SelfAssessment.HIT)
        }
    }

    // Scenario: Tanda con ids duplicados (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a batch with the id A twice when building it then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            batchOf("A", "A")
        }
    }

    // Scenario: Tanda con más frases que su tamaño (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a batch of size 10 with 11 phrases when building it then throws IllegalArgumentException`() {
        // Given
        val ids = (1..11).map { "P$it" }.toTypedArray()

        // When / Then
        shouldThrow<IllegalArgumentException> {
            batchOf(*ids, capacity = BatchSize(10))
        }
    }

    // Scenario: Tamaño de tanda N (spec: Tanda activa conserva su tamaño al cambiar N)
    @Test
    fun `given a batch of 30 entries when answering then the capacity stays 30`() {
        // Given
        val batch = batchOf(*(1..30).map { "P$it" }.toTypedArray(), capacity = BatchSize(30))

        // When
        val updated = batch.answer(PhraseId("P1"), SelfAssessment.HIT)

        // Then
        updated.capacity shouldBe BatchSize(30)
    }

    // Scenario: Aviso con frases no certificadas (spec: Aviso previo al cierre)
    @Test
    fun `given five phrases with two certified when computing the warning then it counts three`() {
        // Given
        val batch = PracticeBatch(
            entries = listOf(
                aPhraseProgress(id = "A", streak = 3, answeredInBatch = true),
                aPhraseProgress(id = "B", streak = 3, answeredInBatch = true),
                aPhraseProgress(id = "C", streak = 1, answeredInBatch = true),
                aPhraseProgress(id = "D", streak = 2, answeredInBatch = true),
                aPhraseProgress(id = "E", streak = 0, answeredInBatch = true),
            ),
            capacity = BatchSize(10),
        )

        // When
        val count = batch.uncertifiedCount()

        // Then
        count shouldBe 3
    }

    // Scenario: Aviso sin frases no certificadas (spec: Aviso previo al cierre)
    @Test
    fun `given two certified phrases when computing the warning then it counts zero`() {
        // Given
        val batch = PracticeBatch(
            entries = listOf(
                aPhraseProgress(id = "A", streak = 3, answeredInBatch = true),
                aPhraseProgress(id = "B", streak = 3, answeredInBatch = true),
            ),
            capacity = BatchSize(10),
        )

        // When
        val count = batch.uncertifiedCount()

        // Then
        count shouldBe 0
    }

    // Scenario: Frase nunca respondida cuenta como no certificada (spec: Aviso previo al cierre)
    @Test
    fun `given one certified and one never answered phrase when computing the warning then it counts one`() {
        // Given
        val batch = PracticeBatch(
            entries = listOf(
                aPhraseProgress(id = "A", streak = 3, answeredInBatch = true),
                aPhraseProgress(id = "B", streak = 0, answeredInBatch = false),
            ),
            capacity = BatchSize(10),
        )

        // When
        val count = batch.uncertifiedCount()

        // Then
        count shouldBe 1
    }

    // Scenario: Calcular el aviso no modifica la tanda (spec: Aviso previo al cierre)
    @Test
    fun `given three uncertified phrases when computing the warning then the batch and its streaks stay the same`() {
        // Given
        val batch = PracticeBatch(
            entries = listOf(
                aPhraseProgress(id = "A", streak = 1, answeredInBatch = true),
                aPhraseProgress(id = "B", streak = 2, answeredInBatch = true),
                aPhraseProgress(id = "C", streak = 0, answeredInBatch = false),
            ),
            capacity = BatchSize(10),
        )
        val before = batch.copy()
        val streaksBefore = batch.entries.map { it.streak }

        // When
        val count = batch.uncertifiedCount()

        // Then
        count shouldBe 3
        batch shouldBe before
        batch.entries.map { it.streak } shouldBe streaksBefore
    }

    private fun batchOf(vararg ids: String, capacity: BatchSize = BatchSize(10)) = PracticeBatch(
        entries = ids.map { aPhraseProgress(id = it) },
        capacity = capacity,
    )
}
