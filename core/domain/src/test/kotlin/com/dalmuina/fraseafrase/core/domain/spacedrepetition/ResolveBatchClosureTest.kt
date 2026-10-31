package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.testing.FixedClock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ResolveBatchClosureTest {

    private val closingDate = LocalDate.of(2026, 3, 10)
    private val resolveBatchClosure = ResolveBatchClosure(FixedClock(closingDate))

    // Scenario: Racha de tres certifica la frase al cerrar (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a phrase with streak 3 when closing the batch then it is classified as certified`() {
        // Given
        val progress = progressWith(level = 0, streak = 3, answeredInBatch = true, nextReview = null)

        // When
        val outcomes = resolveBatchClosure(listOf(progress))

        // Then
        outcomes.single().shouldBeInstanceOf<BatchClosureOutcome.Certified>()
    }

    // Scenario: Racha de dos no certifica la frase al cerrar (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a phrase with streak 2 when closing the batch then it is classified as carried`() {
        // Given
        val progress = progressWith(level = 0, streak = 2, answeredInBatch = true, nextReview = null)

        // When
        val outcomes = resolveBatchClosure(listOf(progress))

        // Then
        outcomes.single().shouldBeInstanceOf<BatchClosureOutcome.Carried>()
    }

    // Scenario: Un fallo tras tres aciertos impide la certificación
    // (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a phrase with streak 3 that is missed before closing then it is classified as carried`() {
        // Given
        val progress = progressWith(level = 0, streak = 3, answeredInBatch = true, nextReview = null)
        val missed = RecordSelfAssessment()(progress, SelfAssessment.MISS)

        // When
        val outcomes = resolveBatchClosure(listOf(missed))

        // Then
        val outcome = outcomes.single()
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.streak shouldBe Streak(0)
    }

    // Scenario: Tanda vacía devuelve lista vacía (spec: Cierre de una tanda sin frases)
    @Test
    fun `given an empty batch when closing the batch then the result is an empty list`() {
        // Given
        val batch = emptyList<PhraseProgress>()

        // When
        val outcomes = resolveBatchClosure(batch)

        // Then
        outcomes shouldBe emptyList()
    }

    // Design (Casos de uso): el resultado conserva el orden de entrada, que practice-batch necesita para arrastrar
    @Test
    fun `given a mixed batch when closing the batch then the outcomes keep the input order`() {
        // Given
        val first = progressWith(level = 0, streak = 3, answeredInBatch = true, nextReview = null)
        val second = progressWith(id = "p-2", level = 0, streak = 2, answeredInBatch = true, nextReview = null)
        val third = progressWith(id = "p-3", level = 0, streak = 4, answeredInBatch = true, nextReview = null)

        // When
        val outcomes = resolveBatchClosure(listOf(first, second, third))

        // Then
        outcomes[0].shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        outcomes[1].shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcomes[2].shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        outcomes.map { it.progress.id } shouldBe listOf(first.id, second.id, third.id)
    }

    // Scenario: Certificada de nivel 0 pasa a nivel 1
    // (spec: Resolución de frase certificada al cerrar la tanda)
    @Test
    fun `given a certified phrase of level 0 when closing then it moves to level 1 with review on 11-03-2026`() {
        // Given
        val progress = progressWith(level = 0, streak = 3, answeredInBatch = true, nextReview = null)

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        assertClosedCertified(outcome.progress, level = 1, nextReview = LocalDate.of(2026, 3, 11))
    }

    // Scenario: Certificada de nivel 1 pasa a nivel 2
    // (spec: Resolución de frase certificada al cerrar la tanda)
    @Test
    fun `given a certified phrase of level 1 when closing then it moves to level 2 with review on 13-03-2026`() {
        // Given
        val progress =
            progressWith(level = 1, streak = 3, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 9))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        assertClosedCertified(outcome.progress, level = 2, nextReview = LocalDate.of(2026, 3, 13))
    }

    // Scenario: Certificada de nivel 2 pasa a nivel 3
    // (spec: Resolución de frase certificada al cerrar la tanda)
    @Test
    fun `given a certified phrase of level 2 when closing then it moves to level 3 with review on 17-03-2026`() {
        // Given
        val progress =
            progressWith(level = 2, streak = 3, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 9))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        assertClosedCertified(outcome.progress, level = 3, nextReview = LocalDate.of(2026, 3, 17))
    }

    // Scenario: Certificada de nivel 3 pasa a nivel 4
    // (spec: Resolución de frase certificada al cerrar la tanda)
    @Test
    fun `given a certified phrase of level 3 when closing then it moves to level 4 with review on 24-03-2026`() {
        // Given
        val progress =
            progressWith(level = 3, streak = 4, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 9))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        assertClosedCertified(outcome.progress, level = 4, nextReview = LocalDate.of(2026, 3, 24))
    }

    // Scenario: Certificada de nivel 4 pasa a nivel 5
    // (spec: Resolución de frase certificada al cerrar la tanda)
    @Test
    fun `given a certified phrase of level 4 when closing then it moves to level 5 with review on 09-04-2026`() {
        // Given
        val progress =
            progressWith(level = 4, streak = 3, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 9))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        assertClosedCertified(outcome.progress, level = 5, nextReview = LocalDate.of(2026, 4, 9))
    }

    // Scenario: Certificada de nivel 5 se mantiene en nivel 5
    // (spec: Resolución de frase certificada al cerrar la tanda)
    @Test
    fun `given a certified phrase of level 5 when closing then it stays at level 5 with review on 09-04-2026`() {
        // Given
        val progress =
            progressWith(level = 5, streak = 3, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 9))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Certified>()
        assertClosedCertified(outcome.progress, level = 5, nextReview = LocalDate.of(2026, 4, 9))
    }

    // Scenario: Respondida no certificada de nivel 4 vuelve a nivel 0
    // (spec: Resolución de frase respondida no certificada al cerrar la tanda)
    @Test
    fun `given an answered uncertified phrase of level 4 when closing then it is carried at level 0`() {
        // Given
        val progress =
            progressWith(level = 4, streak = 1, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 20))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(0)
        outcome.progress.nextReview shouldBe null
    }

    // Scenario: Respondida no certificada de nivel 5 vuelve a nivel 0
    // (spec: Resolución de frase respondida no certificada al cerrar la tanda)
    @Test
    fun `given an answered uncertified phrase of level 5 when closing then it is carried at level 0`() {
        // Given
        val progress =
            progressWith(level = 5, streak = 0, answeredInBatch = true, nextReview = LocalDate.of(2026, 4, 9))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(0)
        outcome.progress.nextReview shouldBe null
    }

    // Scenario: Respondida no certificada de nivel 0 permanece en nivel 0
    // (spec: Resolución de frase respondida no certificada al cerrar la tanda)
    @Test
    fun `given an answered uncertified phrase of level 0 when closing then it stays at level 0`() {
        // Given
        val progress = progressWith(level = 0, streak = 0, answeredInBatch = true, nextReview = null)

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(0)
        outcome.progress.nextReview shouldBe null
    }

    // Scenario: Frase con un fallo cuenta como respondida (spec: Frase respondida en la tanda)
    @Test
    fun `given a phrase that only received a miss when closing then it is carried at level 0 without review`() {
        // Given
        val progress =
            progressWith(level = 2, streak = 0, answeredInBatch = false, nextReview = LocalDate.of(2026, 3, 13))
        val missed = RecordSelfAssessment()(progress, SelfAssessment.MISS)

        // When
        val outcome = closeSingle(missed)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(0)
        outcome.progress.nextReview shouldBe null
    }

    // Scenario: Frase sin autoevaluaciones cuenta como nunca respondida (spec: Frase respondida en la tanda)
    @Test
    fun `given a phrase without self assessments when closing then it keeps its level and review`() {
        // Given
        val progress =
            progressWith(level = 2, streak = 0, answeredInBatch = false, nextReview = LocalDate.of(2026, 3, 13))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(2)
        outcome.progress.nextReview shouldBe LocalDate.of(2026, 3, 13)
    }

    // Scenario: Nunca respondida conserva su nivel y su repaso
    // (spec: Resolución de frase nunca respondida al cerrar la tanda)
    @Test
    fun `given a never answered phrase of level 3 when closing then it keeps level 3 and its review date`() {
        // Given
        val progress =
            progressWith(level = 3, streak = 0, answeredInBatch = false, nextReview = LocalDate.of(2026, 3, 20))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(3)
        outcome.progress.nextReview shouldBe LocalDate.of(2026, 3, 20)
    }

    // Scenario: Nivel nunca baja de 0 (spec: Niveles de consolidación)
    @Test
    fun `given a never answered phrase of level 0 when closing then it stays at level 0`() {
        // Given
        val progress = progressWith(level = 0, streak = 0, answeredInBatch = false, nextReview = null)

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.level shouldBe Level(0)
    }

    // Scenario: Frase arrastrada queda con racha 0 y sin respuestas
    // (spec: Estado de la frase tras cerrar la tanda)
    @Test
    fun `given an answered phrase of level 2 with streak 2 when closing then it is carried with streak 0`() {
        // Given
        val progress =
            progressWith(level = 2, streak = 2, answeredInBatch = true, nextReview = LocalDate.of(2026, 3, 13))

        // When
        val outcome = closeSingle(progress)

        // Then
        outcome.shouldBeInstanceOf<BatchClosureOutcome.Carried>()
        outcome.progress.streak shouldBe Streak(0)
        outcome.progress.answeredInBatch shouldBe false
    }

    private fun closeSingle(progress: PhraseProgress): BatchClosureOutcome =
        resolveBatchClosure(listOf(progress)).single()

    // Spec: Estado de la frase tras cerrar la tanda (racha 0 y sin respuesta en toda variante)
    private fun assertClosedCertified(progress: PhraseProgress, level: Int, nextReview: LocalDate) {
        progress.level shouldBe Level(level)
        progress.nextReview shouldBe nextReview
        progress.streak shouldBe Streak(0)
        progress.answeredInBatch shouldBe false
    }

    private fun progressWith(
        id: String = "p-1",
        level: Int,
        streak: Int,
        answeredInBatch: Boolean,
        nextReview: LocalDate?,
    ) = PhraseProgress(
        id = PhraseId(id),
        level = Level(level),
        streak = Streak(streak),
        answeredInBatch = answeredInBatch,
        nextReview = nextReview,
    )
}
