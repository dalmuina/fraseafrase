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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionFailureBadgeTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    // Scenario: Un fallo con racha mayor que 0 no muestra el indicador (spec: practice-session, ADR-0024)
    @Test
    fun `given a level 3 phrase with streak 2 when answering with a miss then badgeChange is null and it advances`() =
        runTest {
            // Given: tanda A (nivel 3, racha 2) y B, con A revelada
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A", "B"),
                    progress = listOf(
                        aPhraseProgress(id = "A", level = Level(3), streak = Streak(2), answeredInBatch = true),
                        aPhraseProgress(id = "B"),
                    ),
                ),
            )
            val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When: el usuario se autoevalúa con fallo
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
            advanceUntilIdle()

            // Then: no hay transición de insignia y la pantalla avanza a B sin revelar
            val state = viewModel.practicing()
            state.badgeChange shouldBe null
            state.current.id.value shouldBe "B"
            state.revealed shouldBe false
        }

    // Scenario: Un fallo con racha mayor que 0 mantiene la insignia (spec: practice-session, ADR-0024)
    @Test
    fun `given a level 3 phrase with streak 1 when answering with a miss then the level stays 3 Oro`() = runTest {
        // Given: tanda A (nivel 3, racha 1) y B, con A revelada
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(3), streak = Streak(1), answeredInBatch = true),
                    aPhraseProgress(id = "B"),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When: el usuario se autoevalúa con fallo
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
        advanceUntilIdle()

        // Then: no hay transición de insignia y A conserva el nivel 3 (insignia "Oro")
        viewModel.practicing().badgeChange shouldBe null
        val progressA = practice.read().progress.first { it.id.value == "A" }
        progressA.level shouldBe Level(3)
    }
}
