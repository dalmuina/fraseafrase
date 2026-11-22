package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionMissCounterTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    // Scenario: Fallo con racha mayor que 0 suma un acierto al contador (spec: practice-session, ADR-0024)
    @Test
    fun `given a level 2 phrase with streak 2 when answering with a miss then it shows 2 remaining hits for Oro`() =
        runTest {
            // Given / When: A (nivel 2, racha 2) falla (racha 2 → 1, el nivel no cambia) y vuelve a mostrarse
            val state = missThenShowAgain(streak = 2, consumeBadgeChange = false).practicing()

            // Then
            state.current.id.value shouldBe "A"
            state.remainingHitsForNextLevel shouldBe 2
            state.nextLevelBadgeLabel shouldBe "Oro"
        }

    // Scenario: Fallo reinicia el contador (spec: practice-session, ADR-0024)
    @Test
    fun `given a level 2 phrase with streak 1 when answering with a miss then it shows 3 remaining hits for Oro`() =
        runTest {
            // Given / When: A (nivel 2, racha 1) falla (racha 1 → 0, el nivel no cambia) y vuelve a mostrarse
            val state = missThenShowAgain(streak = 1, consumeBadgeChange = false).practicing()

            // Then
            state.current.id.value shouldBe "A"
            state.remainingHitsForNextLevel shouldBe 3
            state.nextLevelBadgeLabel shouldBe "Oro"
        }

    // Scenario: Fallo con racha 0 deja la frase a un acierto del nivel que tenía (spec: practice-session, ADR-0024)
    @Test
    fun `given a level 2 phrase with streak 0 when answering with a miss then it shows 1 remaining hit for Plata`() =
        runTest {
            // Given / When: A (nivel 2, racha 0) falla y baja a nivel 1 con racha 2; el cambio de insignia se
            // consume para liberar los controles (spec: add-level-change-animation)
            val state = missThenShowAgain(streak = 0, consumeBadgeChange = true).practicing()

            // Then
            state.current.id.value shouldBe "A"
            state.remainingHitsForNextLevel shouldBe 1
            state.nextLevelBadgeLabel shouldBe "Plata"
        }

    /** Tanda A (nivel 2) y B: A falla, B acierta y A vuelve a mostrarse revelada. */
    private fun TestScope.missThenShowAgain(streak: Int, consumeBadgeChange: Boolean): PracticeSessionViewModel {
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(
                        id = "A",
                        level = Level(2),
                        streak = Streak(streak),
                        answeredInBatch = true,
                        dueAtSession = 1,
                    ),
                    aPhraseProgress(id = "B"),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
        advanceUntilIdle()
        if (consumeBadgeChange) {
            viewModel.onAction(PracticeSessionAction.ConsumeBadgeChange)
            advanceUntilIdle()
        }
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        return viewModel
    }
}
