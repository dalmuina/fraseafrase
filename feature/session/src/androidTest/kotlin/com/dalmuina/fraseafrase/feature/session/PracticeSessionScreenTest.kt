package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI de [PracticeSessionScreen] (ADR-0007). Verifican lo que ve el usuario: textos, botones habilitados y
 * diálogo. La pantalla es sin estado, así que cada test construye el estado a mano.
 */
class PracticeSessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: Sin revelar no hay autoevaluación (spec: practice-session)
    @Test
    fun givenHiddenPhraseWhenShowingTheScreenThenNoAssessmentIsOffered() {
        // Given: la frase A sin revelar
        setScreen(practicing(current = item(source = "Buenos días", target = "Bom dia")))

        // When: la pantalla se muestra sin revelar (no hay acción que cambie el estado)

        // Then: se muestra la frase en español, no la traducción en portugués, y no se ofrece acierto ni fallo
        composeTestRule.onNodeWithText("Buenos días").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bom dia").assertDoesNotExist()
        composeTestRule.onNodeWithText("Acierto").assertDoesNotExist()
        composeTestRule.onNodeWithText("Fallo").assertDoesNotExist()
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session)
    @Test
    fun givenHiddenPhraseWhenRevealingThenPortugueseTranslationIsShownWithAssessment() {
        // Given: la pantalla muestra la frase A sin revelar; el estado se actualiza con la acción Reveal
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(current = item(source = "Buenos días", target = "Bom dia")),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action == PracticeSessionAction.Reveal) {
                            uiState = (uiState as PracticeSessionUiState.Practicing).copy(revealed = true)
                        }
                    },
                )
            }
        }

        // When: el usuario revela la frase
        composeTestRule.onNodeWithText("Mostrar traducción").performClick()

        // Then: se muestra "Bom dia" junto a "Buenos días", y se ofrece acierto y fallo
        composeTestRule.onNodeWithText("Buenos días").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bom dia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acierto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fallo").assertIsDisplayed()
    }

    // Scenario: Sin revelar no hay control de escuchar (spec: phrase-audio)
    @Test
    fun givenHiddenPhraseWhenShowingTheScreenThenNoListenControlIsShown() {
        // Given: la frase A sin revelar
        setScreen(practicing(current = item()))

        // When: la pantalla se muestra sin revelar (no hay acción que cambie el estado)

        // Then: no se muestra ningún control de escuchar
        composeTestRule.onNodeWithText("Escuchar").assertDoesNotExist()
    }

    // Scenario: Revelar muestra el control de escuchar / Escuchar reproduce la frase en su locale (spec: phrase-audio)
    @Test
    fun givenRevealedPhraseWhenActivatingTheListenControlThenOnActionReceivesPlayAudio() {
        // Given: la pantalla muestra la frase A revelada
        var receivedAction: PracticeSessionAction? = null
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = practicing(current = item(), revealed = true),
                    onAction = { action -> receivedAction = action },
                )
            }
        }

        // When: el usuario activa el control de escuchar
        composeTestRule.onNodeWithText("Escuchar").apply {
            assertIsDisplayed()
            performClick()
        }

        // Then: se despacha PlayAudio
        assertTrue(receivedAction == PracticeSessionAction.PlayAudio)
    }

    // Scenario: Ocupado deshabilita el control de escuchar (spec: phrase-audio)
    @Test
    fun givenAnswerInProgressWhenShowingTheScreenThenTheListenControlIsDisabled() {
        // Given: una frase revelada cuya autoevaluación se está registrando (busy = true)
        setScreen(practicing(current = item(), revealed = true, busy = true))

        // When: la pantalla muestra el estado con la respuesta en curso (estado dado)

        // Then: el control de escuchar está deshabilitado
        composeTestRule.onNodeWithText("Escuchar").assertIsNotEnabled()
    }

    // Scenario: Primera frase de una tanda de cinco (spec: practice-session)
    @Test
    fun givenPracticingState_whenScreenShown_thenPositionIndicatorIsDisplayed() {
        // Given: una tanda activa de cinco frases, mostrando la primera
        setScreen(practicing(current = item()).copy(index = 1, total = 5))

        // When: la pantalla se muestra (estado dado)

        // Then: la pantalla muestra "1 de 5"
        composeTestRule.onNodeWithText("1 de 5").assertIsDisplayed()
    }

    // Scenario: Nivel CEFR visible sin revelar / al revelar (spec: practice-session)
    @Test
    fun givenPracticingState_whenScreenShown_thenCefrLevelIsDisplayed() {
        // Given: una frase de nivel CEFR B1 sin revelar
        setScreen(practicing(current = item(cefrLevel = CefrLevel.B1), revealed = false))

        // When: la pantalla se muestra sin revelar (estado dado)

        // Then: la pantalla muestra "B1" junto a la frase
        composeTestRule.onNodeWithText("B1").assertIsDisplayed()
    }

    // Scenario: Insignia visible sin revelar (spec: practice-session)
    @Test
    fun givenPhraseWithConsolidationLevel2_whenScreenShown_thenSilverBadgeIsDisplayed() {
        // Given: una tanda activa cuya primera frase tiene nivel de consolidación 2
        setScreen(practicing(current = item().copy(level = Level(2)), revealed = false))

        // When: el usuario abre la pantalla de práctica (estado dado)

        // Then: la pantalla muestra la insignia "Plata" junto al nivel CEFR
        composeTestRule.onNodeWithText("Plata").assertIsDisplayed()
    }

    // Scenario: Insignia visible al revelar (spec: practice-session)
    @Test
    fun givenPhraseWithConsolidationLevel4_whenRevealing_thenPlatinumBadgeIsStillDisplayed() {
        // Given: una frase de nivel de consolidación 4 sin revelar; el estado se actualiza con Reveal
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(current = item().copy(level = Level(4)), revealed = false),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action == PracticeSessionAction.Reveal) {
                            uiState = (uiState as PracticeSessionUiState.Practicing).copy(revealed = true)
                        }
                    },
                )
            }
        }

        // When: el usuario revela la frase
        composeTestRule.onNodeWithText("Mostrar traducción").performClick()

        // Then: la pantalla sigue mostrando la insignia "Platino"
        composeTestRule.onNodeWithText("Platino").assertIsDisplayed()
    }

    // Scenario: La insignia cambia con la frase mostrada (spec: practice-session)
    @Test
    fun givenPhraseAShown_whenAssessingAndMovingToPhraseB_thenBadgeChangesWithIt() {
        // Given: una tanda con la frase A de nivel de consolidación 1 y la frase B de nivel de consolidación 5
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(current = item().copy(level = Level(1)), revealed = true),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action is PracticeSessionAction.Answer) {
                            uiState = practicing(current = item().copy(level = Level(5)), revealed = false)
                        }
                    },
                )
            }
        }
        composeTestRule.onNodeWithText("Bronce").assertIsDisplayed()

        // When: el usuario autoevalúa A y la pantalla pasa a mostrar B
        composeTestRule.onNodeWithText("Acierto").performClick()

        // Then: la pantalla muestra la insignia "Diamante"
        composeTestRule.onNodeWithText("Diamante").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bronce").assertDoesNotExist()
    }

    // Scenario: Un fallo que baja el nivel actualiza la insignia (spec: practice-session)
    @Test
    fun givenPhraseWithGoldBadge_whenMissedAndShownAgainWithLowerLevel_thenSilverBadgeIsDisplayed() {
        // Given: una frase con nivel de consolidación 3 (insignia "Oro"), revelada
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(current = item().copy(level = Level(3)), revealed = true),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        assertTrue(action == PracticeSessionAction.Answer(SelfAssessment.MISS))
                        uiState = practicing(current = item().copy(level = Level(2)), revealed = false)
                    },
                )
            }
        }
        composeTestRule.onNodeWithText("Oro").assertIsDisplayed()

        // When: el usuario se autoevalúa con fallo y la frase vuelve a mostrarse con su nivel ya bajado
        composeTestRule.onNodeWithText("Fallo").performClick()

        // Then: la pantalla muestra la insignia "Plata"
        composeTestRule.onNodeWithText("Plata").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oro").assertDoesNotExist()
    }

    // Scenario: Sin insignia en nivel 0 (spec: practice-session)
    @Test
    fun givenPhraseWithConsolidationLevel0_whenScreenShown_thenNoBadgeIsDisplayed() {
        // Given: una frase nueva con nivel de consolidación 0
        setScreen(practicing(current = item().copy(level = Level(0))))

        // When: la pantalla la muestra (estado dado)

        // Then: la pantalla no muestra ninguna insignia para esa frase
        composeTestRule.onNodeWithText("Bronce").assertDoesNotExist()
        composeTestRule.onNodeWithText("Plata").assertDoesNotExist()
        composeTestRule.onNodeWithText("Oro").assertDoesNotExist()
        composeTestRule.onNodeWithText("Platino").assertDoesNotExist()
        composeTestRule.onNodeWithText("Diamante").assertDoesNotExist()
    }

    // Scenario: El control de escuchar muestra un icono de audio (spec: phrase-audio)
    @Test
    fun givenPhraseRevealed_whenScreenShown_thenListenControlShowsAudioIcon() {
        // Given: la pantalla muestra una frase revelada
        setScreen(practicing(current = item(), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: el control de escuchar muestra un icono de audio junto a su texto
        composeTestRule.onNodeWithTag("listen_icon", useUnmergedTree = true).assertIsDisplayed()
    }

    // Scenario: Contador visible tras un acierto (spec: practice-session)
    @Test
    fun givenPhraseWithStreak1WhenShowingItThenTheRemainingHitsCounterIsDisplayed() {
        // Given: una frase de nivel 0 con racha 1, le faltan 2 aciertos para la insignia "Bronce"
        setScreen(practicing(current = item()).copy(remainingHitsForNextLevel = 2, nextLevelBadgeLabel = "Bronce"))

        // When: la pantalla la muestra (estado dado)

        // Then: la pantalla muestra el contador
        composeTestRule.onNodeWithText("Te faltan 2 aciertos para la insignia Bronce").assertIsDisplayed()
    }

    // Scenario: Fallo reinicia el contador (spec: practice-session)
    @Test
    fun givenPhraseWithLevel2WhenMissedAndShownAgainThenTheCounterShowsThreeForSilver() {
        // Given: la frase A de nivel de consolidación 2 y racha 2, revelada
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(current = item(), revealed = true).copy(
                remainingHitsForNextLevel = 1,
                nextLevelBadgeLabel = "Oro",
            ),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        assertTrue(action == PracticeSessionAction.Answer(SelfAssessment.MISS))
                        // El fallo bajó el nivel de consolidación de 2 a 1 (ADR-0015) antes de calcular el contador
                        uiState = practicing(current = item(), revealed = true).copy(
                            remainingHitsForNextLevel = 3,
                            nextLevelBadgeLabel = "Plata",
                        )
                    },
                )
            }
        }

        // When: el usuario se autoevalúa con fallo y A vuelve a mostrarse, revelada
        composeTestRule.onNodeWithText("Fallo").performClick()

        // Then: la pantalla muestra que le faltan 3 aciertos para la insignia "Plata"
        composeTestRule.onNodeWithText("Te faltan 3 aciertos para la insignia Plata").assertIsDisplayed()
    }

    // Scenario: Racha de tres sigue mostrando el contador en 0 (spec: practice-session)
    @Test
    fun givenPhraseWithStreak3WhenShowingItThenTheCounterShowsZero() {
        // Given: una frase de nivel 1 con racha 3, le faltan 0 aciertos para la insignia "Plata"
        setScreen(practicing(current = item()).copy(remainingHitsForNextLevel = 0, nextLevelBadgeLabel = "Plata"))

        // When: la pantalla la muestra (estado dado)

        // Then: la pantalla muestra el contador en 0
        composeTestRule.onNodeWithText("Te faltan 0 aciertos para la insignia Plata").assertIsDisplayed()
    }

    // Scenario: Nivel máximo sigue mostrando el contador (spec: practice-session)
    @Test
    fun givenPhraseAtMaxLevelWhenShowingItThenTheCounterStillShows() {
        // Given: una frase de nivel 5 con racha 3, le faltan 0 aciertos para la insignia "Diamante"
        setScreen(practicing(current = item()).copy(remainingHitsForNextLevel = 0, nextLevelBadgeLabel = "Diamante"))

        // When: la pantalla la muestra (estado dado)

        // Then: la pantalla sigue mostrando el contador
        composeTestRule.onNodeWithText("Te faltan 0 aciertos para la insignia Diamante").assertIsDisplayed()
    }

    // Scenario: Tanda completa muestra el mensaje positivo (spec: practice-session)
    @Test
    fun givenCompletedBatchWhenShowingTheScreenThenThePositiveMessageIsDisplayedInsteadOfAPhrase() {
        // Given: la tanda activa está completa
        setScreen(practicing(current = item(), completed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: se muestra el mensaje positivo con la opción de generar sesión, no la frase
        composeTestRule.onNodeWithText("¡Has completado la tanda!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Generar sesión").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buenos días").assertDoesNotExist()
    }

    // Scenario: Generar sesión muestra la tanda siguiente con sus arrastradas primero (spec: practice-session)
    @Test
    fun givenCompletedBatchWhenGeneratingSessionThenOnActionReceivesGenerateSession() {
        // Given: la tanda activa está completa, con el mensaje positivo visible
        var receivedAction: PracticeSessionAction? = null
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = practicing(current = item(), completed = true),
                    onAction = { action -> receivedAction = action },
                )
            }
        }

        // When: el usuario pide generar sesión
        composeTestRule.onNodeWithText("Generar sesión").performClick()

        // Then: se despacha GenerateSession
        assertTrue(receivedAction == PracticeSessionAction.GenerateSession)
    }

    // Scenario: Estado vacío sin frases disponibles (spec: practice-session)
    @Test
    fun givenNoPhrasesAvailableWhenShowingTheScreenThenItSaysThereIsNothingToPractice() {
        // Given: no hay frases disponibles

        // When: la pantalla se muestra en el estado NothingToPractice
        setScreen(PracticeSessionUiState.NothingToPractice)

        // Then: la pantalla indica que no hay frases para practicar y no muestra ninguna frase
        composeTestRule.onNodeWithText("No hay frases para practicar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mostrar traducción").assertDoesNotExist()
    }

    // Scenario: Mientras hay una respuesta en curso pedir generar sesión no tiene efecto (spec: practice-session)
    @Test
    fun givenAnswerInProgressWhenShowingTheScreenThenTheGenerateSessionButtonIsDisabled() {
        // Given: la tanda está completa y, además, una autoevaluación se está registrando (busy = true)
        setScreen(practicing(current = item(), busy = true, completed = true))

        // When: la pantalla muestra el estado con la respuesta en curso (estado dado)

        // Then: el botón de generar sesión está deshabilitado
        composeTestRule.onNodeWithText("Generar sesión").assertIsNotEnabled()
    }

    private fun setScreen(uiState: PracticeSessionUiState) {
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(uiState = uiState, onAction = {})
            }
        }
    }

    private fun practicing(
        current: PracticeItem = item(),
        revealed: Boolean = false,
        busy: Boolean = false,
        completed: Boolean = false,
    ): PracticeSessionUiState.Practicing = PracticeSessionUiState.Practicing(
        current = current,
        revealed = revealed,
        busy = busy,
        completed = completed,
        index = 1,
        total = 1,
        remainingHitsForNextLevel = 0,
        nextLevelBadgeLabel = "Bronce",
    )

    /** Frase de presentación. */
    private fun item(
        source: String = "Buenos días",
        target: String = "Bom dia",
        cefrLevel: CefrLevel = CefrLevel.A1,
        streak: Int = 0,
    ): PracticeItem = PracticeItem(
        id = PhraseId("A"),
        source = source,
        target = target,
        targetLocale = LanguageTag("pt-PT"),
        cefrLevel = cefrLevel,
        streak = Streak(streak),
        level = Level(0),
    )
}
