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
    fun `given a level 3 phrase with streak 0 when recording a miss then it is level 2 streak 2 due at 11`() {
        missed(level = 3, streak = 0) shouldBe expected(level = 2, streak = 2)
    }

    // Scenario: Fallo en nivel 1 baja a nivel 0 y fija la sesión siguiente (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 1 phrase with streak 0 when recording a miss then it is level 0 streak 2 due at 11`() {
        missed(level = 1, streak = 0) shouldBe expected(level = 0, streak = 2)
    }

    // Scenario: Fallo con racha 0 en nivel 5 baja a nivel 4 con racha 2 (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 5 phrase with streak 0 when recording a miss then it is level 4 streak 2 due at 11`() {
        missed(level = 5, streak = 0) shouldBe expected(level = 4, streak = 2)
    }

    // Scenario: Fallo con racha mayor que 0 no baja de nivel (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 3 phrase with streak 2 when recording a miss then it is level 3 streak 1 due at 11`() {
        missed(level = 3, streak = 2) shouldBe expected(level = 3, streak = 1)
    }

    // Scenario: Fallo en nivel 0 no cambia el nivel pero sí fija la sesión siguiente
    // (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 0 phrase with streak 0 when recording a miss then it stays level 0 streak 0 due at 11`() {
        missed(level = 0, streak = 0) shouldBe expected(level = 0, streak = 0)
    }

    // Scenario: Fallo en nivel 0 con racha mayor que 0 resta uno a la racha (spec: Resolución de la autoevaluación)
    @Test
    fun `given a level 0 phrase with streak 2 when recording a miss then it is level 0 streak 1 due at 11`() {
        missed(level = 0, streak = 2) shouldBe expected(level = 0, streak = 1)
    }

    // Scenario: Fallo resta uno a la racha (spec: Racha persistente entre sesiones)
    @Test
    fun `given a level 2 phrase with streak 2 when recording a miss then it is level 2 streak 1 due at 11`() {
        missed(level = 2, streak = 2) shouldBe expected(level = 2, streak = 1)
    }

    // Scenario: Fallo reinicia la racha (spec: Racha persistente entre sesiones)
    @Test
    fun `given a level 2 phrase with streak 1 when recording a miss then it is level 2 streak 0 due at 11`() {
        missed(level = 2, streak = 1) shouldBe expected(level = 2, streak = 0)
    }

    private fun missed(level: Int, streak: Int): PhraseProgress = PhraseProgress(
        id = PhraseId("p-1"),
        level = Level(level),
        streak = Streak(streak),
        answeredInBatch = false,
        dueAtSession = 20,
    ).record(SelfAssessment.MISS, 10)

    private fun expected(level: Int, streak: Int): PhraseProgress = PhraseProgress(
        id = PhraseId("p-1"),
        level = Level(level),
        streak = Streak(streak),
        answeredInBatch = true,
        dueAtSession = 11,
    )

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

    // Scenario: Acierto que no certifica en nivel 3 espera dos sesiones
    // (spec: Acierto que no certifica en nivel 3 espera dos sesiones)
    @Test
    fun `given a level 3 phrase with streak 1 when a hit is recorded at session 10 then dueAtSession is 12`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(3),
            streak = Streak(1),
            answeredInBatch = false,
            dueAtSession = 10,
        )

        // When
        val updated = progress.record(SelfAssessment.HIT, 10)

        // Then
        updated.level shouldBe Level(3)
        updated.streak shouldBe Streak(2)
        updated.dueAtSession shouldBe 12
    }

    // Scenario: Acierto que no certifica en nivel 5 espera ocho sesiones
    // (spec: Acierto que no certifica en nivel 5 espera ocho sesiones)
    @Test
    fun `given a level 5 phrase with streak 0 when a hit is recorded at session 10 then dueAtSession is 18`() {
        // Given
        val progress = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(5),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 10,
        )

        // When
        val updated = progress.record(SelfAssessment.HIT, 10)

        // Then
        updated.level shouldBe Level(5)
        updated.streak shouldBe Streak(1)
        updated.dueAtSession shouldBe 18
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

    // Scenario: Acierto en repaso adelantado no suma (spec: Acierto sobre una frase aún no pendiente)
    @Test
    fun `given a phrase not yet due when recording a hit then only answeredInBatch changes`() {
        // Given
        val progress = notYetDue(level = 2, streak = 1)

        // When
        val updated = progress.record(SelfAssessment.HIT, 12)

        // Then
        updated shouldBe progress.copy(answeredInBatch = true)
    }

    // Scenario: Acierto en repaso adelantado con racha 2 no certifica
    // (spec: Acierto sobre una frase aún no pendiente)
    @Test
    fun `given a phrase not yet due with streak 2 when recording a hit then it does not certify`() {
        // Given
        val progress = notYetDue(level = 1, streak = 2)

        // When
        val updated = progress.record(SelfAssessment.HIT, 12)

        // Then
        updated shouldBe progress.copy(answeredInBatch = true)
    }

    // Scenario: Un segundo acierto en la misma sesión no suma (spec: Acierto sobre una frase aún no pendiente)
    @Test
    fun `given a phrase hit once in the session when recording a second hit then nothing else changes`() {
        // Given
        val hitOnce = PhraseProgress(
            id = PhraseId("p-1"),
            level = Level(1),
            streak = Streak(0),
            answeredInBatch = false,
            dueAtSession = 10,
        ).record(SelfAssessment.HIT, 10)

        // When
        val hitTwice = hitOnce.record(SelfAssessment.HIT, 10)

        // Then
        hitTwice.level shouldBe Level(1)
        hitTwice.streak shouldBe Streak(1)
        hitTwice.dueAtSession shouldBe 11
    }

    // Scenario: Fallo en repaso adelantado cuenta (spec: Acierto sobre una frase aún no pendiente)
    @Test
    fun `given a phrase not yet due when recording a miss then it loses a streak and is due next session`() {
        // Given
        val progress = notYetDue(level = 2, streak = 1)

        // When
        val updated = progress.record(SelfAssessment.MISS, 12)

        // Then
        updated shouldBe progress.copy(streak = Streak(0), answeredInBatch = true, dueAtSession = 13)
    }

    private fun notYetDue(level: Int, streak: Int): PhraseProgress = PhraseProgress(
        id = PhraseId("p-1"),
        level = Level(level),
        streak = Streak(streak),
        answeredInBatch = false,
        dueAtSession = 14,
    )
}
