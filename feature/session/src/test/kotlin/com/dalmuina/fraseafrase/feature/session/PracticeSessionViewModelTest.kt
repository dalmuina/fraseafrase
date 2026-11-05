package com.dalmuina.fraseafrase.feature.session

import app.cash.turbine.test
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePhrasePlayer
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
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

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionViewModelTest {

    // StandardTestDispatcher: las acciones se encolan y los tests avanzan el reloj de Main a mano (`advanceUntilIdle`).
    // Con un dispatcher sin cola no se podría comprobar el doble toque (tarea 3.3).
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    // Scenario: Al abrir la práctica se muestra la primera frase en español (spec: practice-session)
    @Test
    fun `given an active batch when opening then the first phrase is shown in Spanish with the target hidden`() =
        runTest {
            // Given: tanda activa A, B y C en ese orden; A con "Buenos días" y "Bom dia"
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val catalog = loaded(
                aCatalogPhrase(id = "A", source = "Buenos días", target = "Bom dia"),
                aCatalogPhrase(id = "B"),
                aCatalogPhrase(id = "C"),
            )

            // When
            val viewModel = sessionViewModel(practice, catalog)
            advanceUntilIdle()

            // Then
            viewModel.uiState.test {
                val state = awaitItem().shouldBeInstanceOf<PracticeSessionUiState.Practicing>()
                state.current.source shouldBe "Buenos días"
                state.revealed shouldBe false
            }
        }

    // Scenario: Frases con repaso futuro sin tanda (spec: practice-session, "Estado vacío sin frases disponibles")
    @Test
    fun `given only future reviews and no batch when opening the screen then the state is NothingToPractice`() =
        runTest {
            // Given: el catálogo tiene solo P, con nivel 1 y repaso el 11-03-2026; hoy es 10-03-2026
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(aPhraseProgress(id = "P", level = Level(1), nextReview = TODAY.plusDays(1))),
                    activeBatch = null,
                    carry = CarryQueue(emptyList()),
                ),
            )

            // When
            val viewModel = sessionViewModel(practice, loaded(aCatalogPhrase(id = "P")))
            advanceUntilIdle()

            // Then
            viewModel.uiState.test {
                awaitItem() shouldBe PracticeSessionUiState.NothingToPractice
            }
        }

    // Scenario: Asset de catálogo ilegible (spec: practice-session, "Error de catálogo")
    @Test
    fun `given an unreadable catalog when opening the screen then the state is CatalogError`() = runTest {
        // Given: el catálogo no se puede leer
        val practice = FakePracticeRepository()
        val catalog = CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)

        // When
        val viewModel = sessionViewModel(practice, catalog)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            awaitItem() shouldBe PracticeSessionUiState.CatalogError
        }
    }

    // Scenario: Documento de catálogo malformado (spec: practice-session, "Error de catálogo")
    @Test
    fun `given a malformed catalog document when opening the screen then the state is CatalogError`() = runTest {
        // Given: el documento del catálogo no es JSON válido
        val practice = FakePracticeRepository()
        val catalog = CatalogLoadResult.Failed(CatalogLoadError.MalformedDocument)

        // When
        val viewModel = sessionViewModel(practice, catalog)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            awaitItem() shouldBe PracticeSessionUiState.CatalogError
        }
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session)
    @Test
    fun `given a hidden phrase when revealing then the target text is shown`() = runTest {
        // Given: la pantalla muestra la frase A sin revelar, con "Bom dia" como traducción
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val catalog = loaded(
            aCatalogPhrase(id = "A", source = "Buenos días", target = "Bom dia"),
            aCatalogPhrase(id = "B"),
        )
        val viewModel = sessionViewModel(practice, catalog)
        advanceUntilIdle()

        // When
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then
        viewModel.uiState.test {
            val state = awaitItem().shouldBeInstanceOf<PracticeSessionUiState.Practicing>()
            state.current.target shouldBe "Bom dia"
            state.revealed shouldBe true
        }
    }

    // Scenario: Acierto muestra la siguiente frase (spec: practice-session)
    @Test
    fun `given a revealed phrase when answering with a hit then the answered phrase goes last`() =
        runTest {
            // Given: tanda A, B y C con A revelada
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()

            // Then
            viewModel.practicing().current.id.value shouldBe "B"
            viewModel.practicing().revealed shouldBe false
            practice.activeOrder() shouldBe listOf("B", "C", "A")
        }

    // Scenario: Fallo muestra la siguiente frase (spec: practice-session)
    @Test
    fun `given a revealed phrase when answering with a miss then the answered phrase goes last`() =
        runTest {
            // Given: tanda A, B y C con A revelada
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
            advanceUntilIdle()

            // Then
            viewModel.practicing().current.id.value shouldBe "B"
            viewModel.practicing().revealed shouldBe false
            practice.activeOrder() shouldBe listOf("B", "C", "A")
        }

    // Scenario: Sin revelar no hay autoevaluación (spec: practice-session)
    @Test
    fun `given a hidden phrase when answering then nothing is recorded and the batch does not change`() = runTest {
        // Given: tanda A, B y C con A sin revelar
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        val before = practice.read()

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().current.id.value shouldBe "A"
        viewModel.practicing().revealed shouldBe false
        practice.read() shouldBe before
    }

    // Scenario: Doble toque no registra dos respuestas (spec: practice-session)
    @Test
    fun `given a revealed phrase when answering twice quickly then only one answer is recorded`() =
        runTest {
            // Given: tanda A, B y C con A revelada. El StandardTestDispatcher deja la primera respuesta en cola.
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When: dos aciertos seguidos, sin dejar avanzar el dispatcher entre ellos
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()

            // Then
            practice.streakOf("A").value shouldBe 1
            practice.activeOrder() shouldBe listOf("B", "C", "A")
        }

    // Scenario: La tanda da la vuelta sin desaparecer frases (spec: practice-session)
    @Test
    fun `given a two phrase batch when answering each phrase then the batch cycles and keeps both phrases`() = runTest {
        // Given: tanda A y B
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()

        // When: A con acierto y después B con fallo
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
        advanceUntilIdle()

        // Then: vuelve A sin revelar y la tanda sigue con A y B
        viewModel.practicing().current.id.value shouldBe "A"
        viewModel.practicing().revealed shouldBe false
        practice.activeOrder() shouldBe listOf("A", "B")
    }

    // Scenario: Racha visible tras un acierto (spec: practice-session)
    @Test
    fun `given a phrase with streak 1 when revealing then the visible streak is 1`() = runTest {
        // Given: tanda A con racha 1 y B
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", streak = Streak(1), answeredInBatch = true),
                    aPhraseProgress(id = "B")
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()

        // When
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then
        viewModel.practicing().current.streak.value shouldBe 1
    }

    // Scenario: Fallo reinicia la racha visible (spec: practice-session)
    @Test
    fun `given a single phrase with streak 2 when missing it then its next reveal shows streak 0`() = runTest {
        // Given: tanda con una sola frase A de racha 2
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A"),
                progress = listOf(aPhraseProgress(id = "A", streak = Streak(2), answeredInBatch = true)),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
        advanceUntilIdle()

        // When: A vuelve a mostrarse y se revela
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then
        viewModel.practicing().current.id.value shouldBe "A"
        viewModel.practicing().current.streak.value shouldBe 0
    }

    // Scenario: Frase con racha 3 marcada como sabida (spec: practice-session)
    @Test
    fun `given a phrase with streak 3 when it is shown then it is marked as known and stays in the batch`() = runTest {
        // Given: tanda con A (racha 3) y B
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", streak = Streak(3), answeredInBatch = true),
                    aPhraseProgress(id = "B")
                ),
            ),
        )

        // When
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()

        // Then
        viewModel.practicing().current.isKnown shouldBe true
        practice.activeOrder() shouldBe listOf("A", "B")
    }

    // Scenario: Frase con racha 2 no se marca (spec: practice-session)
    @Test
    fun `given a phrase with streak 2 when it is revealed then it is not marked as known`() = runTest {
        // Given: tanda con A (racha 2) y B
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", streak = Streak(2), answeredInBatch = true),
                    aPhraseProgress(id = "B")
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()

        // When
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then
        viewModel.practicing().current.isKnown shouldBe false
    }

    // Scenario: Primera frase de una tanda de cinco (spec: add-practice-batch-indicator, practice-session)
    @Test
    fun `given an active batch of five phrases when opening the screen then the indicator shows 1 of 5`() = runTest {
        // Given: tanda activa de cinco frases A, B, C, D y E
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C", "D", "E")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C", "D", "E"))

        // When
        advanceUntilIdle()

        // Then
        viewModel.practicing().index shouldBe 1
        viewModel.practicing().total shouldBe 5
    }

    // Scenario: El indicador avanza tras autoevaluar (spec: add-practice-batch-indicator, practice-session)
    @Test
    fun `given a batch of three phrases showing 1 of 3 when answering then the indicator shows 2 of 3`() = runTest {
        // Given: tanda de tres frases A, B y C con A revelada, mostrando "1 de 3"
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
        advanceUntilIdle()
        viewModel.practicing().index shouldBe 1
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().index shouldBe 2
        viewModel.practicing().total shouldBe 3
    }

    // Scenario: El indicador vuelve a 1 al dar la vuelta a la tanda (spec: add-practice-batch-indicator)
    @Test
    fun `given a two phrase batch showing 2 of 2 when answering the second phrase then the indicator shows 1 of 2`() =
        runTest {
            // Given: tanda de dos frases A y B, mostrando B en la posición "2 de 2"
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.practicing().index shouldBe 2
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
            advanceUntilIdle()

            // Then
            viewModel.practicing().index shouldBe 1
            viewModel.practicing().total shouldBe 2
        }

    // Scenario: El indicador refleja el tamaño de una tanda siguiente distinto (spec: add-practice-batch-indicator)
    @Test
    fun `given a closed batch of five when the next batch has eight phrases then the indicator shows 1 of 8`() =
        runTest {
            // Given: cierre confirmado de una tanda de cinco frases cuya tanda siguiente tiene ocho frases
            val practice = FakePracticeRepository(
                activeBatchSnapshot(order = listOf("A", "B", "C", "D", "E")),
            )
            val catalog = catalogOf("A", "B", "C", "D", "E", "F", "G", "H")
            val viewModel = sessionViewModel(practice, catalog)
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.RequestClose)
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.ConfirmClose)
            advanceUntilIdle()

            // Then
            viewModel.practicing().index shouldBe 1
            viewModel.practicing().total shouldBe 8
        }

    // Scenario: Escuchar reproduce la frase en su locale (spec: phrase-audio)
    @Test
    fun `given a revealed phrase when PlayAudio is dispatched then PlayPhraseAudio receives its target and locale`() =
        runTest {
            // Given: tanda A revelada, con "Bom dia" en pt-PT como traducción
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
            val catalog = loaded(
                aCatalogPhrase(id = "A", source = "Buenos días", target = "Bom dia"),
                aCatalogPhrase(id = "B"),
            )
            val phrasePlayer = FakePhrasePlayer()
            val viewModel = sessionViewModel(practice, catalog, phrasePlayer = phrasePlayer)
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When
            viewModel.onAction(PracticeSessionAction.PlayAudio)
            advanceUntilIdle()

            // Then
            phrasePlayer.calls shouldBe listOf(FakePhrasePlayer.Call("Bom dia", LanguageTag("pt-PT")))
        }

    // Scenario: Ocupado deshabilita el control de escuchar (spec: phrase-audio)
    @Test
    fun `given a response being registered when PlayAudio is dispatched then PlayPhraseAudio is not invoked`() =
        runTest {
            // Given: tanda A, B y C con A revelada y su autoevaluación encolada, todavía sin registrar (busy)
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val phrasePlayer = FakePhrasePlayer()
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"), phrasePlayer = phrasePlayer)
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))

            // When: se pide escuchar antes de que la respuesta termine de registrarse
            viewModel.onAction(PracticeSessionAction.PlayAudio)

            // Then
            phrasePlayer.calls.shouldBeEmpty()
        }

    // Scenario: Aviso de cierre abierto deshabilita el control de escuchar (spec: phrase-audio)
    @Test
    fun `given the close warning open when PlayAudio is dispatched then PlayPhraseAudio is not invoked`() = runTest {
        // Given: tanda A, B con A revelada y el aviso de cierre abierto
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val phrasePlayer = FakePhrasePlayer()
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"), phrasePlayer = phrasePlayer)
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)
        viewModel.onAction(PracticeSessionAction.RequestClose)
        advanceUntilIdle()

        // When
        viewModel.onAction(PracticeSessionAction.PlayAudio)

        // Then
        phrasePlayer.calls.shouldBeEmpty()
    }
}
