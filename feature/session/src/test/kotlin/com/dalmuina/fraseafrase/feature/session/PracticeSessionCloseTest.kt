package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/** Cierre en dos pasos con aviso y diálogo modal (design D4). Los tests de ViewModel usan los casos de uso reales. */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionCloseTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    /** Frase certificada: racha 3 respondida en la tanda. */
    private fun certified(id: String): PhraseProgress = aPhraseProgress(id = id, streak = 3, answeredInBatch = true)

    // Scenario: Aviso con frases no certificadas (spec: practice-session)
    @Test
    fun `given five phrases with two certified when requesting close then the warning counts the three uncertified`() =
        runTest {
            // Given: tanda de cinco frases; A y B certificadas (racha 3), C, D y E sin certificar
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A", "B", "C", "D", "E"),
                    progress = listOf(
                        certified("A"),
                        certified("B"),
                        aPhraseProgress(id = "C"),
                        aPhraseProgress(id = "D"),
                        aPhraseProgress(id = "E")
                    ),
                ),
            )
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C", "D", "E"))
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.RequestClose)
            advanceUntilIdle()

            // Then
            viewModel.practicing().closeWarning shouldBe 3
        }

    // Scenario: Aviso sin frases no certificadas (spec: practice-session)
    @Test
    fun `given two certified phrases when requesting close then the warning is zero`() = runTest {
        // Given: tanda de dos frases, ambas con racha 3
        val practice = FakePracticeRepository(
            activeBatchSnapshot(order = listOf("A", "B"), progress = listOf(certified("A"), certified("B"))),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()

        // When
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()

        // Then
        viewModel.practicing().closeWarning shouldBe 0
    }

    // Scenario: Cerrar mientras hay una respuesta en curso no abre el aviso (spec: practice-session)
    @Test
    fun `given an answer in progress when requesting close then the warning does not open`() = runTest {
        // Given: tanda A, B y C con A revelada y su autoevaluación encolada, todavía sin registrar
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))

        // When: se pide cerrar antes de que la respuesta termine de registrarse
        viewModel.onAction(PracticeSessionAction.RequestClose)

        // Then
        viewModel.practicing().closeWarning shouldBe null
    }

    // Scenario: Cancelar el cierre no cambia nada (spec: practice-session)
    @Test
    fun `given an open close warning when cancelling then the batch and the progress stay unchanged`() = runTest {
        // Given: tanda A, B y C sin certificar, con el aviso abierto
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        val before = practice.read()
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()
        viewModel.practicing().closeWarning shouldBe 3

        // When
        viewModel.onAction(PracticeSessionAction.CancelClose)
        advanceUntilIdle()

        // Then
        viewModel.practicing().closeWarning shouldBe null
        practice.read() shouldBe before
    }

    // Scenario: Confirmar el cierre usa el número del aviso (spec: practice-session)
    @Test
    fun `given a close warning of three when confirming then the batch is closed with that expectation`() = runTest {
        // Given: tanda A (certificada), B, C y D sin certificar; el aviso indica tres
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B", "C", "D"),
                progress = listOf(
                    certified("A"),
                    aPhraseProgress(id = "B"),
                    aPhraseProgress(id = "C"),
                    aPhraseProgress(id = "D")
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C", "D"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()
        viewModel.practicing().closeWarning shouldBe 3

        // When
        viewModel.onAction(PracticeSessionAction.ConfirmClose)
        advanceUntilIdle()

        // Then: la tanda se cerró, así que A queda certificada en el progreso (nivel 1)
        practice.read().progress.single { it.id.value == "A" }.level.value shouldBe 1
    }

    // Scenario: Con el aviso abierto no se puede autoevaluar (spec: practice-session)
    @Test
    fun `given an open warning with a revealed phrase when answering then the answer is not recorded`() = runTest {
        // Given: tanda A y B con A revelada y el aviso abierto
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()
        viewModel.practicing().closeWarning shouldBe 2
        val before = practice.read()

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        practice.read() shouldBe before
        viewModel.practicing().closeWarning shouldBe 2
    }

    // Scenario: Cierre con arrastradas muestra la tanda siguiente (spec: practice-session)
    @Test
    fun `given C and A carried when confirming then the next batch starts with C, A and new phrases`() =
        runTest {
            // Given: tanda C y A sin certificar; catálogo con 8 frases nuevas de A1; N igual a 10
            val newIds = (1..8).map { "N$it" }
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("C", "A")))
            val viewModel = sessionViewModel(
                practice,
                catalogOf("A", "C", *newIds.toTypedArray()),
                settings = UserSettings(cefrLevel = CefrLevel.A1, batchSize = BatchSize(10)),
            )
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.RequestClose)
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.ConfirmClose)
            advanceUntilIdle()

            // Then
            viewModel.practicing().current?.id?.value shouldBe "C"
            practice.activeOrder() shouldBe listOf("C", "A") + newIds
        }

    // Scenario: Cierre con todas certificadas deja la tanda siguiente sin arrastradas (spec: practice-session)
    @Test
    fun `given a closed batch with all phrases certified when confirming then the next batch has no carried phrases`() =
        runTest {
            // Given: tanda de A y B certificadas; el catálogo tiene N1 y N2 nuevas de A1
            val practice = FakePracticeRepository(
                activeBatchSnapshot(order = listOf("A", "B"), progress = listOf(certified("A"), certified("B"))),
            )
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "N1", "N2"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.RequestClose)
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.ConfirmClose)
            advanceUntilIdle()

            // Then
            viewModel.practicing().current?.id?.value shouldBe "N1"
            practice.activeOrder() shouldBe listOf("N1", "N2")
            practice.read().carry.ids.shouldBeEmpty()
        }

    // Scenario: Aviso desfasado recarga la tanda (spec: practice-session)
    @Test
    fun `given a stale warning when confirming then the saved batch is shown again and nothing is closed`() = runTest {
        // Given: aviso con tres no certificadas; después, A se certifica desde fuera y quedan dos
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()
        viewModel.practicing().closeWarning shouldBe 3
        practice.certify("A")

        // When
        viewModel.onAction(PracticeSessionAction.ConfirmClose)
        advanceUntilIdle()

        // Then: la pantalla muestra la tanda guardada (A ya con racha 3), sin aviso y sin cerrar
        viewModel.practicing().closeWarning shouldBe null
        viewModel.practicing().current?.isKnown shouldBe true
        practice.activeOrder() shouldBe listOf("A", "B", "C")
    }

    // Scenario: Cierre sin tanda activa recarga la pantalla (spec: practice-session)
    @Test
    fun `given no active batch when confirming then the screen reloads without error`() = runTest {
        // Given: aviso con dos no certificadas; la tanda activa desaparece antes de confirmar
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "N1"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()
        practice.transact { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }

        // When
        viewModel.onAction(PracticeSessionAction.ConfirmClose)
        advanceUntilIdle()

        // Then: la pantalla se recarga sin error y muestra la tanda que se compone a partir del estado guardado
        viewModel.uiState.value.shouldBeInstanceOf<PracticeSessionUiState.Practicing>()
        viewModel.practicing().current?.id?.value shouldBe "N1"
        viewModel.practicing().closeWarning shouldBe null
    }

    private suspend fun FakePracticeRepository.certify(id: String) = transact { snapshot ->
        PracticeChange(
            snapshot.copy(progress = snapshot.progress.map { if (it.id.value == id) certified(id) else it }),
            Unit
        )
    }
}
