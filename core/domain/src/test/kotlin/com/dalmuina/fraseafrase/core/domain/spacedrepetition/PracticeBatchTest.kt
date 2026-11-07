package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PracticeBatchTest {

    private val currentSession = 5

    // Scenario: Acierto mueve la frase al final (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when the user hits A then the order becomes B C A`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch.answer(PhraseId("A"), SelfAssessment.HIT, currentSession)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("B"), PhraseId("C"), PhraseId("A"))
    }

    // Scenario: Fallo mueve la frase al final (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when the user misses B then the order becomes A C B`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch.answer(PhraseId("B"), SelfAssessment.MISS, currentSession)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("A"), PhraseId("C"), PhraseId("B"))
    }

    // Scenario: Responder la última frase no cambia el orden (spec: Lista circular de la tanda)
    @Test
    fun `given a batch of A B C when the user answers C then the order stays A B C`() {
        // Given
        val batch = batchOf("A", "B", "C")

        // When
        val updated = batch.answer(PhraseId("C"), SelfAssessment.HIT, currentSession)

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
            .answer(PhraseId("A"), SelfAssessment.HIT, currentSession)
            .answer(PhraseId("B"), SelfAssessment.MISS, currentSession)
            .answer(PhraseId("C"), SelfAssessment.HIT, currentSession)
            .answer(PhraseId("A"), SelfAssessment.MISS, currentSession)
            .answer(PhraseId("B"), SelfAssessment.HIT, currentSession)

        // Then
        updated.entries.map { it.id } shouldContainExactly listOf(PhraseId("C"), PhraseId("A"), PhraseId("B"))
    }

    // Scenario: Bajada instantánea de nivel ante un fallo (spec: Racha de aciertos dentro de la tanda)
    @Test
    fun `given a batch with a phrase of level 2 when the user misses it then its level drops to 1`() {
        // Given
        val batch = PracticeBatch(
            entries = listOf(
                aPhraseProgress(id = "A", level = Level(2), dueAtSession = 20),
            ),
            capacity = BatchSize(10),
        )

        // When
        val updated = batch.answer(PhraseId("A"), SelfAssessment.MISS, currentSession)

        // Then
        val answered = updated.entries.single { it.id == PhraseId("A") }
        answered.level shouldBe Level(1)
        answered.dueAtSession shouldBe currentSession + 1
    }

    // Scenario: Responder una frase que no está en la tanda (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a batch of A and B when answering C then throws IllegalArgumentException`() {
        // Given
        val batch = batchOf("A", "B")

        // When / Then
        shouldThrow<IllegalArgumentException> {
            batch.answer(PhraseId("C"), SelfAssessment.HIT, currentSession)
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
        val updated = batch.answer(PhraseId("P1"), SelfAssessment.HIT, currentSession)

        // Then
        updated.capacity shouldBe BatchSize(30)
    }

    private fun batchOf(vararg ids: String, capacity: BatchSize = BatchSize(10)) = PracticeBatch(
        entries = ids.map { aPhraseProgress(id = it) },
        capacity = capacity,
    )
}
