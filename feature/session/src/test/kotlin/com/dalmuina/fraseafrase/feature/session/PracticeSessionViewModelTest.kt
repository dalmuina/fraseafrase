package com.dalmuina.fraseafrase.feature.session

import app.cash.turbine.test
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePhrasePlayer
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
            // Given: el catálogo tiene solo P, con nivel 1 y repaso pendiente a partir de la sesión 1; sin tanda
            // activa y sin sesiones generadas todavía (sessionCount 0)
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(aPhraseProgress(id = "P", level = Level(1), dueAtSession = 1)),
                    activeBatch = null,
                    sessionCount = 0,
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
    fun `given a batch of five when the generated next batch has eight phrases then the indicator shows 1 of 8`() =
        runTest {
            // Given: una tanda de cinco frases sin certificar (quedan arrastradas) cuya tanda generada tiene ocho
            val practice = FakePracticeRepository(
                activeBatchSnapshot(order = listOf("A", "B", "C", "D", "E")),
            )
            val catalog = catalogOf("A", "B", "C", "D", "E", "F", "G", "H")
            val viewModel = sessionViewModel(practice, catalog)
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.GenerateSession)
            advanceUntilIdle()

            // Then
            viewModel.practicing().index shouldBe 1
            viewModel.practicing().total shouldBe 8
        }

    // Scenario: Tanda completa muestra el mensaje positivo (spec: practice-session)
    @Test
    fun `given one phrase answered when answering the last unanswered phrase then the batch state is completed`() =
        runTest {
            // Given: tanda de dos frases, A ya respondida en la tanda, B sin responder; B revelada
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A", "B"),
                    progress = listOf(
                        aPhraseProgress(id = "A", answeredInBatch = true),
                        aPhraseProgress(id = "B"),
                    ),
                ),
            )
            val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When: el usuario autoevalúa la última frase sin responder
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()

            // Then: la tanda está completa, en vez de mostrar otra frase
            viewModel.practicing().completed shouldBe true
        }

    // Scenario: Una respuesta en curso ignora la petición de generar sesión (spec: practice-session)
    @Test
    fun `given a response being registered when GenerateSession is dispatched then the batch does not change`() =
        runTest {
            // Given: tanda A, B y C con A revelada y su autoevaluación encolada, todavía sin confirmar
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))

            // When: se pide generar sesión antes de que la respuesta termine de registrarse
            viewModel.onAction(PracticeSessionAction.GenerateSession)
            advanceUntilIdle()

            // Then: la petición se ignora y la tanda sigue siendo la original
            practice.activeOrder() shouldBe listOf("B", "C", "A")
        }

    // Scenario: Contador visible tras un acierto (spec: practice-session)
    @Test
    fun `given a level 0 phrase with streak 1 when revealing then it shows 2 remaining hits for Bronce`() = runTest {
        // Given: tanda con una frase de nivel 0 y racha 1
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A"),
                progress = listOf(aPhraseProgress(id = "A", streak = Streak(1), answeredInBatch = true)),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A"))
        advanceUntilIdle()

        // When
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then
        viewModel.practicing().remainingHitsForNextLevel shouldBe 2
        viewModel.practicing().nextLevelBadgeLabel shouldBe "Bronce"
    }

    // Scenario: Fallo reinicia el contador (spec: practice-session)
    @Test
    fun `given a level 2 phrase with streak 2 when answering with a miss then it shows 3 remaining hits for Plata`() =
        runTest {
            // Given: tanda con la frase A de nivel 2 y racha 2, y B sin responder (para no completar la tanda)
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A", "B"),
                    progress = listOf(
                        aPhraseProgress(
                            id = "A",
                            level = Level(2),
                            streak = Streak(2),
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

            // When: el usuario se autoevalúa con fallo; el fallo baja el nivel de A a 1 (ADR-0015), lo que muestra
            // el indicador de cambio de insignia (spec: add-level-change-animation) y hay que consumirlo para
            // liberar los controles
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.ConsumeBadgeChange)
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // Then: al mostrarse A de nuevo y revelarla
            viewModel.practicing().current.id.value shouldBe "A"
            viewModel.practicing().remainingHitsForNextLevel shouldBe 3
            viewModel.practicing().nextLevelBadgeLabel shouldBe "Plata"
        }

    // Scenario: Racha de tres sigue mostrando el contador en 0 (spec: practice-session)
    @Test
    fun `given a level 1 phrase with streak 3 when shown then it shows 0 remaining hits for Plata`() = runTest {
        // Given: tanda con una frase de nivel 1 y racha 3
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A"),
                progress = listOf(
                    aPhraseProgress(
                        id = "A",
                        level = Level(1),
                        streak = Streak(3),
                        answeredInBatch = true,
                        dueAtSession = 1,
                    ),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A"))

        // When
        advanceUntilIdle()

        // Then
        viewModel.practicing().remainingHitsForNextLevel shouldBe 0
        viewModel.practicing().nextLevelBadgeLabel shouldBe "Plata"
    }

    // Scenario: Nivel máximo sigue mostrando el contador (spec: practice-session)
    @Test
    fun `given a level 5 phrase with streak 3 when shown then it shows 0 remaining hits for Diamante`() = runTest {
        // Given: tanda con una frase de nivel 5 y racha 3
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A"),
                progress = listOf(
                    aPhraseProgress(
                        id = "A",
                        level = Level(5),
                        streak = Streak(3),
                        answeredInBatch = true,
                        dueAtSession = 1,
                    ),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A"))

        // When
        advanceUntilIdle()

        // Then
        viewModel.practicing().remainingHitsForNextLevel shouldBe 0
        viewModel.practicing().nextLevelBadgeLabel shouldBe "Diamante"
    }

    // Scenario: El contador cambia con la frase mostrada (spec: practice-session)
    @Test
    fun `given phrase A shown when answering and moving to phrase B then the counter matches B`() = runTest {
        // Given: tanda con A (nivel 0, racha 0) y B (nivel 2, racha 1), mostrando A
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(0), streak = Streak(0)),
                    aPhraseProgress(
                        id = "B",
                        level = Level(2),
                        streak = Streak(1),
                        answeredInBatch = true,
                        dueAtSession = 1,
                    ),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When: el usuario autoevalúa A y la pantalla pasa a mostrar B
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().current.id.value shouldBe "B"
        viewModel.practicing().remainingHitsForNextLevel shouldBe 2
        viewModel.practicing().nextLevelBadgeLabel shouldBe "Oro"
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

    // Scenario: Tanda completada deshabilita el control de escuchar (spec: phrase-audio, design D3)
    @Test
    fun `given a completed batch when PlayAudio is dispatched then PlayPhraseAudio is not invoked`() = runTest {
        // Given: tanda A, B con ambas certificadas, así que la tanda está completa
        val practice = FakePracticeRepository(
            activeBatchSnapshot(order = listOf("A", "B"), progress = listOf(certified("A"), certified("B"))),
        )
        val phrasePlayer = FakePhrasePlayer()
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"), phrasePlayer = phrasePlayer)
        advanceUntilIdle()
        viewModel.practicing().completed shouldBe true

        // When
        viewModel.onAction(PracticeSessionAction.PlayAudio)

        // Then
        phrasePlayer.calls.shouldBeEmpty()
    }

    // Scenario: Resume sin cambio de tanda conserva el estado revelado y la posición (design D4)
    @Test
    fun `given a revealed phrase not first when Resume is dispatched without a batch change then state is kept`() =
        runTest {
            // Given: tanda A, B y C; tras acertar A, B se muestra revelada en la posición 2 de 3
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val viewModel = sessionViewModel(practice, catalogOf("A", "B", "C"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            val before = viewModel.practicing()
            before.index shouldBe 2
            before.revealed shouldBe true

            // When: se vuelve a la pestaña de práctica sin que la tanda haya cambiado en el repositorio
            viewModel.onAction(PracticeSessionAction.Resume)
            advanceUntilIdle()

            // Then: el estado no cambia
            viewModel.practicing() shouldBe before
        }

    // Scenario: Resume refleja una tanda distinta cuando sí cambió en el repositorio (design D4)
    @Test
    fun `given a loaded batch when Resume is dispatched after it changed in the repository then it is shown`() =
        runTest {
            // Given: tanda activa A, B y C cargada; Ajustes vacía la tanda activa sin aviso (design D3)
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
            val catalog = catalogOf("A", "B", "C", "D", "E")
            val viewModel = sessionViewModel(practice, catalog)
            advanceUntilIdle()
            val before = viewModel.practicing()
            practice.transact { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }

            // When
            viewModel.onAction(PracticeSessionAction.Resume)
            advanceUntilIdle()

            // Then: la tanda leída es distinta y el indicador se reinicia a 1 de la tanda nueva
            viewModel.practicing().index shouldBe 1
            practice.activeOrder() shouldNotBe emptyList<String>()
            viewModel.practicing() shouldNotBe before
        }

    // Scenario: Un acierto que certifica muestra el indicador (spec: add-level-change-animation)
    @Test
    fun `given a level 2 phrase with streak 2 when answering with a hit then badgeChange shows UP to Oro`() =
        runTest {
            // Given: tanda A (nivel 2, racha 2) y B, con A revelada
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A", "B"),
                    progress = listOf(
                        aPhraseProgress(id = "A", level = Level(2), streak = Streak(2), answeredInBatch = true),
                        aPhraseProgress(id = "B"),
                    ),
                ),
            )
            val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()

            // Then
            val badgeChange = viewModel.practicing().badgeChange
            badgeChange?.direction shouldBe BadgeChange.Direction.UP
            badgeChange?.badgeLabel shouldBe "Oro"
        }

    // Scenario: Un fallo que baja el nivel muestra el indicador (spec: add-level-change-animation)
    @Test
    fun `given a level 3 phrase when answering with a miss then badgeChange shows DOWN to Plata`() = runTest {
        // Given: tanda A (nivel 3) y B, con A revelada
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(3), answeredInBatch = true),
                    aPhraseProgress(id = "B"),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
        advanceUntilIdle()

        // Then
        val badgeChange = viewModel.practicing().badgeChange
        badgeChange?.direction shouldBe BadgeChange.Direction.DOWN
        badgeChange?.badgeLabel shouldBe "Plata"
    }

    // Scenario: Un acierto que no certifica no muestra el indicador (spec: add-level-change-animation)
    @Test
    fun `given a level 1 phrase with streak 0 when answering with a hit then badgeChange is null`() = runTest {
        // Given: tanda A (nivel 1, racha 0) y B, con A revelada
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(1), streak = Streak(0), answeredInBatch = true),
                    aPhraseProgress(id = "B"),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().badgeChange shouldBe null
    }

    // Scenario: Un fallo en nivel 0 no muestra el indicador (spec: add-level-change-animation)
    @Test
    fun `given a level 0 phrase when answering with a miss then badgeChange is null`() = runTest {
        // Given: tanda A (nivel 0) y B, con A revelada
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(0), answeredInBatch = true),
                    aPhraseProgress(id = "B"),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.MISS))
        advanceUntilIdle()

        // Then
        viewModel.practicing().badgeChange shouldBe null
    }

    // Scenario: Certificar de nuevo en nivel máximo no muestra el indicador (spec: add-level-change-animation)
    @Test
    fun `given a level 5 phrase with streak 2 when answering with a hit then badgeChange is null`() = runTest {
        // Given: tanda A (nivel 5, racha 2) y B, con A revelada
        val practice = FakePracticeRepository(
            activeBatchSnapshot(
                order = listOf("A", "B"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(5), streak = Streak(2), answeredInBatch = true),
                    aPhraseProgress(id = "B"),
                ),
            ),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
        advanceUntilIdle()

        // Then
        viewModel.practicing().badgeChange shouldBe null
    }

    // Scenario: design.md, Decisión 3: `busy` permanece true mientras se anima el cambio de insignia
    @Test
    fun `given a badgeChange shown when ConsumeBadgeChange is dispatched then badgeChange is null and busy is false`() =
        runTest {
            // Given: tanda A (nivel 2, racha 2) y B, con A revelada y respondida con acierto que certifica
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A", "B"),
                    progress = listOf(
                        aPhraseProgress(id = "A", level = Level(2), streak = Streak(2), answeredInBatch = true),
                        aPhraseProgress(id = "B"),
                    ),
                ),
            )
            val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.onAction(PracticeSessionAction.Answer(SelfAssessment.HIT))
            advanceUntilIdle()
            viewModel.practicing().badgeChange shouldNotBe null
            viewModel.practicing().busy shouldBe true

            // When
            viewModel.onAction(PracticeSessionAction.ConsumeBadgeChange)

            // Then
            viewModel.practicing().badgeChange shouldBe null
            viewModel.practicing().busy shouldBe false
        }

    /** Frase certificada: racha 3 respondida en la tanda. */
    private fun certified(id: String): PhraseProgress =
        aPhraseProgress(id = id, streak = Streak(3), answeredInBatch = true)
}
