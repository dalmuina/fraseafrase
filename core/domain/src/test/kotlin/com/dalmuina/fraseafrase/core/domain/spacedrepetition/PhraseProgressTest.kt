package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PhraseProgressTest {

    // Scenario: Racha de tres certifica la frase al cerrar (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a streak of 3 when checking certification then the phrase is certified`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(3),
            answeredInBatch = true,
            dueAtSession = 0,
        )

        // When
        val certified = progress.isCertified

        // Then
        certified shouldBe true
    }

    // Scenario: Racha de dos no certifica la frase al cerrar (spec: Certificación por racha al cerrar la tanda)
    @Test
    fun `given a streak of 2 when checking certification then the phrase is not certified`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(2),
            answeredInBatch = true,
            dueAtSession = 0,
        )

        // When
        val certified = progress.isCertified

        // Then
        certified shouldBe false
    }

    // Scenario: `dueAtSession` negativo se rechaza (spec: Invariantes de PhraseProgress con sesión de vencimiento)
    @Test
    fun `given a negative due at session when building the phrase then throws IllegalArgumentException`() {
        // When / Then
        shouldThrow<IllegalArgumentException> {
            PhraseProgress(
                id = PhraseId("p-1"),
                level = Level(0),
                streak = Streak(0),
                answeredInBatch = false,
                dueAtSession = -1,
            )
        }
    }

    // Scenario: Sesión igual al vencimiento está pendiente (spec: Frase pendiente de repaso por sesión)
    @Test
    fun `given a phrase due at session 12 when checking pending at session 12 then it is pending`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(2),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 12,
        )

        // When
        val pending = progress.isPendingAtSession(12)

        // Then
        pending shouldBe true
    }

    // Scenario: Sesión anterior al vencimiento no está pendiente (spec: Frase pendiente de repaso por sesión)
    @Test
    fun `given a phrase due at session 12 when checking pending at session 11 then it is not pending`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(2),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 12,
        )

        // When
        val pending = progress.isPendingAtSession(11)

        // Then
        pending shouldBe false
    }

    // Scenario: Una frase de nivel 0 recién fallada está pendiente en la sesión siguiente
    // (spec: Frase pendiente de repaso por sesión)
    @Test
    fun `given a level 0 phrase due at the next session when checking pending then it is pending`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(0),
            answeredInBatch = true,
            dueAtSession = 11,
        )

        // When
        val pending = progress.isPendingAtSession(11)

        // Then
        pending shouldBe true
    }

    // Scenario: Fallo en nivel 3 baja a nivel 2 y fija la sesión siguiente (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 3 phrase when recording a miss then it drops to level 2 and due at session 11`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(3),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 20,
        )

        // When
        val updated = progress.record(SelfAssessment.MISS, 10)

        // Then
        updated.level shouldBe Level(2)
        updated.dueAtSession shouldBe 11
    }

    // Scenario: Fallo en nivel 1 baja a nivel 0 y fija la sesión siguiente (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 1 phrase when recording a miss then it drops to level 0 and due at session 11`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(1),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 11,
        )

        // When
        val updated = progress.record(SelfAssessment.MISS, 10)

        // Then
        updated.level shouldBe Level(0)
        updated.dueAtSession shouldBe 11
    }

    // Scenario: Fallo en nivel 0 no cambia el nivel pero sí fija la sesión siguiente
    // (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 0 phrase when recording a miss then the level stays and due at session is 11`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 0,
        )

        // When
        val updated = progress.record(SelfAssessment.MISS, 10)

        // Then
        updated.level shouldBe Level(0)
        updated.dueAtSession shouldBe 11
    }

    // Scenario: Acierto que certifica sube de nivel y entra en enfriamiento (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 1 phrase with streak 2 when recording a hit then it certifies to level 2 at session 12`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(1),
            streak = Streak(2),
            answeredInBatch = true,
            dueAtSession = 10,
        )

        // When
        val updated = progress.record(SelfAssessment.HIT, 10)

        // Then
        updated.level shouldBe Level(2)
        updated.streak shouldBe Streak(0)
        updated.dueAtSession shouldBe 12
    }

    // Scenario: Acierto que no certifica fija la sesión siguiente (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 1 phrase with streak 0 when recording a hit then it stays at level 1 due at session 11`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(1),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 10,
        )

        // When
        val updated = progress.record(SelfAssessment.HIT, 10)

        // Then
        updated.level shouldBe Level(1)
        updated.streak shouldBe Streak(1)
        updated.dueAtSession shouldBe 11
    }

    // Scenario: Una frase nueva certificada por primera vez pasa a nivel 1 (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 0 phrase with streak 2 when recording a hit then it certifies to level 1 at session 11`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(0),
            streak = Streak(2),
            answeredInBatch = true,
            dueAtSession = 0,
        )

        // When
        val updated = progress.record(SelfAssessment.HIT, 10)

        // Then
        updated.level shouldBe Level(1)
        updated.streak shouldBe Streak(0)
        updated.dueAtSession shouldBe 11
    }
}
