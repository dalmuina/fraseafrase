package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Pruebas de `canRevertLastAnswer` y de `PracticeSessionAction.RevertLastAnswer` (spec: revert-selection). Vive en
 * una clase propia, separada de [PracticeSessionViewModelTest], para no superar el límite de tamaño de clase de
 * detekt.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionRevertTest {

    // StandardTestDispatcher: las acciones se encolan y los tests avanzan el reloj de Main a mano.
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    // Scenario: El botón aparece tras autoevaluar (spec: practice-session)
    @Test
    fun `given a revealed phrase when answering with a hit then canRevertLastAnswer is true`() = runTest {
        // Given: tanda A, B con A revelada
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().current.id.value shouldBe "B"
        viewModel.practicing().canRevertLastAnswer shouldBe true
    }

    // Scenario: El botón no aparece durante la transición de cambio de insignia (spec: practice-session)
    @Test
    fun `given a hit that certifies then canRevertLastAnswer is false until badge change is consumed`() = runTest {
        // Given: tanda A (nivel 2, racha 2) y B, con A revelada
        val practice = FakePracticeRepository(levelUpOnHitSnapshot())
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When: el acierto certifica y dispara badgeChange
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then: mientras la transición está en curso, el botón no aparece
        viewModel.practicing().badgeChange shouldNotBe null
        viewModel.practicing().canRevertLastAnswer shouldBe false

        // When: se consume la transición de insignia
        viewModel.onAction(PracticeSessionAction.ConsumeBadgeChange)
        advanceUntilIdle()

        // Then: el botón vuelve a aparecer
        viewModel.practicing().canRevertLastAnswer shouldBe true
    }

    // Scenario: El botón no aparece antes de la primera autoevaluación (spec: practice-session)
    @Test
    fun `given a freshly opened screen when no answer was given then canRevertLastAnswer is false`() = runTest {
        // Given: tanda A, B recién cargada, sin ninguna autoevaluación todavía
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))

        // When
        advanceUntilIdle()

        // Then
        viewModel.practicing().canRevertLastAnswer shouldBe false
    }

    // Scenario: El botón no aparece mientras se registra una nueva autoevaluación (spec: practice-session)
    @Test
    fun `given a pending undo when answering a second phrase then canRevertLastAnswer is false synchronously`() =
        runTest {
            // Given: tanda A, B y C con A ya autoevaluada; la pantalla muestra B con el botón visible
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.practicing().canRevertLastAnswer shouldBe true
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When: se autoevalúa B, sin dejar avanzar el dispatcher todavía (StandardTestDispatcher)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))

            // Then: el botón desaparece de inmediato, antes de que termine esa corrutina
            viewModel.practicing().canRevertLastAnswer shouldBe false
        }

    // Scenario: Revertir un acierto vuelve a mostrar la frase anterior (spec: practice-session)
    @Test
    fun `given a pending undo when RevertLastAnswer is dispatched then the previous phrase is shown revealed`() =
        runTest {
            // Given: tanda A, B y C con A revelada, autoevaluada con acierto; la pantalla muestra B con el botón
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            val streakBefore = practice.streakOf("A")
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.practicing().current.id.value shouldBe "B"

            // When
            viewModel.onAction(PracticeSessionAction.RevertLastAnswer)
            advanceUntilIdle()

            // Then: A vuelve a mostrarse revelada, en su posición anterior, con su racha restaurada
            val state = viewModel.practicing()
            state.current.id.value shouldBe "A"
            state.revealed shouldBe true
            state.index shouldBe 1
            state.canRevertLastAnswer shouldBe false
            practice.streakOf("A") shouldBe streakBefore
        }

    // Scenario: Doble toque en revertir solo revierte una vez (spec: practice-session, design D5)
    @Test
    fun `given a pending undo when RevertLastAnswer is dispatched twice then only the first has an effect`() =
        runTest {
            // Given: tanda A, B y C con A revelada, autoevaluada con acierto; el botón de revertir visible
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()

            // When: se pulsa dos veces seguidas, sin dejar avanzar el dispatcher entre ellas
            viewModel.onAction(PracticeSessionAction.RevertLastAnswer)
            viewModel.onAction(PracticeSessionAction.RevertLastAnswer)
            advanceUntilIdle()

            // Then: solo se revierte una vez, sin lanzar ninguna excepción
            viewModel.practicing().current.id.value shouldBe "A"
            practice.activeOrder() shouldBe listOf("A", "B", "C")
        }

    // Scenario: design D5, riesgo de carrera con una autoevaluación en curso (spec: practice-session)
    @Test
    fun `given a response being registered when RevertLastAnswer is dispatched then it has no effect`() = runTest {
        // Given: tanda A, B y C; A ya autoevaluada (botón visible apuntando a su deshacer), B revelada y su
        // autoevaluación encolada, todavía sin confirmar (busy)
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))

        // When: se pide revertir antes de que la segunda autoevaluación termine de registrarse
        viewModel.onAction(PracticeSessionAction.RevertLastAnswer)
        advanceUntilIdle()

        // Then: la reversión se ignora, sin lanzar ninguna excepción; la tanda refleja las dos autoevaluaciones
        practice.activeOrder() shouldBe listOf("C", "A", "B")
    }

    // Scenario: Autoevaluar de nuevo tras revertir vuelve a ofrecer el botón (spec: practice-session)
    @Test
    fun `given a reverted answer when answering again and advancing then canRevertLastAnswer is true`() = runTest {
        // Given: tanda A, B y C; A autoevaluada con acierto, revertida, revelada de nuevo
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.RevertLastAnswer)
        advanceUntilIdle()
        viewModel.practicing().current.id.value shouldBe "A"

        // When: se autoevalúa A de nuevo y la pantalla avanza a B
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().current.id.value shouldBe "B"
        viewModel.practicing().canRevertLastAnswer shouldBe true
    }

    // Scenario: Generar sesión hace desaparecer el botón (spec: practice-session)
    @Test
    fun `given a pending undo when GenerateSession is dispatched then canRevertLastAnswer is false`() = runTest {
        // Given: tanda A, B con ambas certificadas (tanda completada), B autoevaluada en último lugar, con el
        // botón de revertir visible
        val practice = FakePracticeRepository(
            activeBatchSnapshot(order = listOf("A", "B"), progress = listOf(certified("A"), certified("B"))),
        )
        val catalog = catalogOf("A", "B", "C", "D", "E", "F", "G", "H")
        val viewModel = sessionViewModel(practice, catalog)
        advanceUntilIdle()
        viewModel.practicing().completed shouldBe true

        // When
        viewModel.onAction(PracticeSessionAction.GenerateSession)
        advanceUntilIdle()

        // Then
        viewModel.practicing().canRevertLastAnswer shouldBe false
    }

    // Scenario: design D3, limpieza de pendingUndo fuera del flujo de autoevaluación (spec: practice-session)
    @Test
    fun `given a pending undo when Resume reloads a changed batch then RevertLastAnswer has no effect`() = runTest {
        // Given: tanda A, B y C; A autoevaluada (botón visible apuntando a su deshacer); Ajustes vacía la tanda
        // activa sin aviso (design D3), y Resume la recompone
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val catalog = catalogOf("A", "B", "C", "D", "E")
        val viewModel = sessionViewModel(practice, catalog)
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()
        viewModel.practicing().canRevertLastAnswer shouldBe true
        practice.transact { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }

        // When
        viewModel.onAction(PracticeSessionAction.Resume)
        advanceUntilIdle()
        viewModel.practicing().canRevertLastAnswer shouldBe false
        val before = viewModel.practicing()
        viewModel.onAction(PracticeSessionAction.RevertLastAnswer)
        advanceUntilIdle()

        // Then: no tiene ningún efecto
        viewModel.practicing() shouldBe before
    }

    // Scenario: El botón desaparece al revelar la frase siguiente (spec: practice-session)
    @Test
    fun `given a pending undo when revealing the next phrase then canRevertLastAnswer is false`() = runTest {
        // Given: tanda A, B y C; A autoevaluada; la pantalla muestra B sin revelar con el botón visible
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()
        viewModel.practicing().canRevertLastAnswer shouldBe true
        viewModel.practicing().revealed shouldBe false

        // When
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then
        viewModel.practicing().canRevertLastAnswer shouldBe false
    }

    /** Frase certificada: racha 3 respondida en la tanda. */
    private fun certified(id: String): PhraseProgress =
        aPhraseProgress(id = id, streak = Streak(3), answeredInBatch = true)
}
