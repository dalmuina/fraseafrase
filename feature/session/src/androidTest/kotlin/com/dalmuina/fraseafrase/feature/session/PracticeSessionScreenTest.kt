package com.dalmuina.fraseafrase.feature.session

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.TextUnit
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Tests de UI de [PracticeSessionScreen] (ADR-0007). Verifican lo que ve el usuario: textos, botones habilitados y
 * diálogo. La pantalla es sin estado, así que cada test construye el estado a mano.
 */
class PracticeSessionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        /** Margen bajo "la mitad" al comparar anchos (design D2): da cabida al espaciado entre botones. */
        private const val HALF_WIDTH_RATIO_TOLERANCE = 0.35f

        /** Margen bajo "todo el ancho" al comparar anchos (design D2): da cabida al padding de la pantalla. */
        private const val FULL_WIDTH_RATIO_TOLERANCE = 0.8f

        /** Tag del contenedor desplazable con la frase y la traducción, ver [PracticeSessionScreen]. */
        private const val SCROLLABLE_CONTENT_TAG = "practice_scrollable_content"

        /**
         * Tiempo intermedio entre la duración de visibilidad de DOWN (750 ms) y la de UP (950 ms) con los
         * tokens por defecto de [com.dalmuina.fraseafrase.core.designsystem.theme.Motion], ver `visibleDurationMillis`
         * en [PracticeSessionScreen].
         */
        private const val INTERMEDIATE_DURATION_MILLIS = 850L

        /**
         * Margen al comparar la altura del indicador de cambio de insignia con la de la etiqueta del nivel
         * CEFR (design D2 de `new-badge-animation`): comparten fila, así que deben quedar a una altura
         * similar, muy lejos de estar centrados en toda la pantalla.
         */
        private const val ROW_ALIGNMENT_TOLERANCE_DP = 24f
    }

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
    fun givenPhraseWithConsolidationLevel0_whenScreenShown_thenNoRankBadgeIsDisplayed() {
        // Given: una frase nueva con nivel de consolidación 0
        setScreen(practicing(current = item().copy(level = Level(0))))

        // When: la pantalla la muestra (estado dado)

        // Then: la pantalla muestra la insignia "Sin rango" para esa frase, no ninguna de rango
        composeTestRule.onNodeWithText("Sin rango").assertIsDisplayed()
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

    // Scenario: Posición en la tanda visible (spec: practice-session), cabecera fija (design D1,
    // fix-style-pass-two)
    @Test
    fun givenPracticingState_whenShowingTheScreen_thenTitleAndProgressPillAreDisplayed() {
        // Given: una tanda activa de cinco frases, mostrando la segunda
        setScreen(practicing(current = item(source = "Buenos días")).copy(index = 2, total = 5))

        // When: la pantalla se muestra (estado dado)

        // Then: la cabecera muestra el título "Practicar" y la píldora "2 de 5" a la vez que la frase actual
        composeTestRule.onNodeWithText("Practicar").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 de 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buenos días").assertIsDisplayed()
    }

    // Scenario: Posición en la tanda visible (spec: practice-session), barra de progreso (design D1,
    // fix-style-pass-two)
    @Test
    fun givenPracticingState_whenShowingTheScreen_thenProgressIndicatorIsDisplayed() {
        // Given: una tanda activa de cinco frases, mostrando la segunda
        setScreen(practicing(current = item()).copy(index = 2, total = 5))

        // When: la pantalla se muestra (estado dado)

        // Then: la cabecera muestra una barra de progreso lineal
        composeTestRule.onNodeWithTag("practice_progress_indicator").assertIsDisplayed()
    }

    // Scenario: Estado vacío sin frases disponibles (spec: practice-session), sin cabecera (design D1,
    // fix-style-pass-two)
    @Test
    fun givenNothingToPracticeState_whenShowingTheScreen_thenTitleAndPillAreNotDisplayed() {
        // Given: no hay frases disponibles

        // When: la pantalla se muestra en el estado NothingToPractice
        setScreen(PracticeSessionUiState.NothingToPractice)

        // Then: no aparecen el título ni la píldora de posición ni la barra de progreso
        composeTestRule.onNodeWithText("Practicar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("practice_progress_indicator").assertDoesNotExist()
    }

    // Scenario: Nivel CEFR de la frase visible (spec: practice-session), etiqueta pequeña consistente
    // (design D4, fix-style-pass-two)
    @Test
    fun givenPracticingState_whenShowingTheScreen_thenCefrLevelLabelUsesLabelMediumTypography() {
        // Given: se captura el tamaño de fuente esperado para labelMedium del tema actual, y una frase de nivel B1
        var expectedFontSize: TextUnit = TextUnit.Unspecified
        composeTestRule.setContent {
            AppTheme {
                expectedFontSize = MaterialTheme.typography.labelMedium.fontSize
                // When: la pantalla muestra la frase sin revelar
                PracticeSessionScreen(
                    uiState = practicing(current = item(cefrLevel = CefrLevel.B1)),
                    onAction = {},
                )
            }
        }

        // Then: el tamaño de fuente del nivel CEFR coincide con labelMedium
        val layoutResults = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText("B1")
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)

        assertEquals(expectedFontSize, layoutResults.first().layoutInput.style.fontSize)
    }

    // Scenario: Revelar muestra la traducción en portugués (spec: practice-session), botones de
    // autoevaluación a ancho completo (design D2, fix-style-pass-two)
    @Test
    fun givenRevealedState_whenShowingTheScreen_thenHitAndMissButtonsAreFullWidth() {
        // Given: la pantalla muestra una frase revelada
        setScreen(practicing(current = item(), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: cada botón de autoevaluación ocupa aproximadamente la mitad del ancho disponible
        val rootWidth = composeTestRule.onRoot().getUnclippedBoundsInRoot().widthValue()
        val missWidth = composeTestRule.onNodeWithText("Fallo").getUnclippedBoundsInRoot().widthValue()
        val hitWidth = composeTestRule.onNodeWithText("Acierto").getUnclippedBoundsInRoot().widthValue()

        assertTrue(missWidth >= rootWidth * HALF_WIDTH_RATIO_TOLERANCE)
        assertTrue(hitWidth >= rootWidth * HALF_WIDTH_RATIO_TOLERANCE)
    }

    // Scenario: Sin revelar no hay autoevaluación (spec: practice-session), botón de revelar a
    // ancho completo (design D2, fix-style-pass-two)
    @Test
    fun givenHiddenState_whenShowingTheScreen_thenRevealButtonIsFullWidth() {
        // Given: la pantalla muestra una frase sin revelar
        setScreen(practicing(current = item(), revealed = false))

        // When: la pantalla se muestra (estado dado)

        // Then: el botón "Mostrar traducción" ocupa aproximadamente todo el ancho disponible
        val rootWidth = composeTestRule.onRoot().getUnclippedBoundsInRoot().widthValue()
        val revealWidth = composeTestRule.onNodeWithText("Mostrar traducción").getUnclippedBoundsInRoot().widthValue()

        assertTrue(revealWidth >= rootWidth * FULL_WIDTH_RATIO_TOLERANCE)
    }

    // Scenario: El control de revelar es visible con una frase muy larga (spec: practice-session)
    @Test
    fun givenVeryLongPhrase_whenScreenIsShown_thenRevealControlIsDisplayed() {
        // Given: la pantalla muestra una frase sin revelar cuyo texto desborda la pantalla
        setScreen(practicing(current = item(source = veryLongText())))

        // When: la pantalla se muestra (estado dado)

        // Then: el control de revelar la traducción está visible sin necesitar desplazamiento
        composeTestRule.onNodeWithText("Mostrar traducción").assertIsDisplayed()
    }

    // Scenario: Los controles de acierto y fallo son visibles con una traducción muy larga (spec: practice-session)
    @Test
    fun givenVeryLongTranslation_whenScreenIsShown_thenHitAndMissControlsAreDisplayed() {
        // Given: la pantalla muestra una frase revelada cuya traducción desborda la pantalla
        setScreen(practicing(current = item(target = veryLongText()), revealed = true))

        // When: la pantalla se muestra (estado dado)

        // Then: los controles de acierto y fallo están visibles sin necesitar desplazamiento
        composeTestRule.onNodeWithText("Acierto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fallo").assertIsDisplayed()
    }

    // Scenario: La frase y la traducción muy largas se pueden desplazar (spec: practice-session)
    @Test
    fun givenVeryLongPhraseAndTranslation_whenContentIsScrolled_thenHitAndMissControlsStayDisplayed() {
        // Given: la pantalla muestra una frase revelada cuya frase y traducción juntas desbordan la pantalla
        setScreen(
            practicing(
                current = item(source = veryLongText("frase"), target = veryLongText("traduccion")),
                revealed = true,
            ),
        )
        val missBoundsBeforeScroll = composeTestRule.onNodeWithText("Fallo").getUnclippedBoundsInRoot()

        // When: el usuario desplaza el contenido de la frase y la traducción
        composeTestRule.onNodeWithTag(SCROLLABLE_CONTENT_TAG)
            .performScrollToNode(hasText(veryLongText("traduccion"), substring = true))

        // Then: los controles de acierto y fallo permanecen visibles y pulsables en el mismo lugar
        composeTestRule.onNodeWithText("Acierto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fallo").assertIsDisplayed()
        val missBoundsAfterScroll = composeTestRule.onNodeWithText("Fallo").getUnclippedBoundsInRoot()
        assertEquals(missBoundsBeforeScroll.top, missBoundsAfterScroll.top)
    }

    // Scenario: La subida se muestra con halo (spec: add-level-change-animation) / El control de revelar
    // sigue presente mientras se anima el cambio de insignia (spec: add-level-change-animation)
    @Test
    fun givenUpBadgeChange_whenScreenShown_thenHaloIsShownAndRevealControlStaysPresent() {
        // Given: una autoevaluación que certifica cambia el nivel de la frase revelada, respondida (busy)
        setScreen(
            practicing(
                current = item(),
                revealed = true,
                busy = true,
                badgeChange = BadgeChange(BadgeChange.Direction.UP, "Oro", fromLevel = 2, toLevel = 3),
            ),
        )

        // When: la pantalla se muestra (estado dado)

        // Then: el halo del indicador está presente, la traducción sigue visible y los controles de
        // acierto/fallo de esa frase siguen presentes, deshabilitados por la respuesta en curso
        composeTestRule.onNodeWithTag("ds-level-badge-change-halo").assertExists()
        composeTestRule.onNodeWithText("Bom dia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acierto").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Fallo").assertIsNotEnabled()
    }

    // Scenario: La bajada se muestra sin halo (spec: add-level-change-animation)
    @Test
    fun givenDownBadgeChange_whenScreenShown_thenHaloIsAbsent() {
        // Given: una autoevaluación que baja el nivel cambia el nivel de la frase revelada, respondida (busy)
        setScreen(
            practicing(
                current = item(),
                revealed = true,
                busy = true,
                badgeChange = BadgeChange(BadgeChange.Direction.DOWN, "Plata", fromLevel = 3, toLevel = 2),
            ),
        )

        // When: la pantalla se muestra (estado dado)

        // Then: el indicador de cambio de insignia está presente sin halo, la traducción sigue visible y
        // los controles de acierto/fallo de esa frase siguen presentes, deshabilitados por la respuesta
        // en curso
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertExists()
        composeTestRule.onNodeWithTag("ds-level-badge-change-halo").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bom dia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Acierto").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Fallo").assertIsNotEnabled()
    }

    // Scenario: Un acierto que no certifica no muestra el indicador (spec: add-level-change-animation)
    @Test
    fun givenNoBadgeChange_whenScreenShown_thenBadgeChangeIndicatorIsNotShown() {
        // Given: una autoevaluación sin cambio de nivel visible
        setScreen(practicing(current = item(), badgeChange = null))

        // When: la pantalla se muestra (estado dado)

        // Then: no se pinta ningún indicador de cambio de insignia
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertDoesNotExist()
    }

    // Scenario: El indicador desaparece sin intervención del usuario (spec: add-level-change-animation)
    @Test
    fun givenBadgeChange_whenDurationElapses_thenConsumeBadgeChangeIsDispatchedAutomatically() {
        // Given: la pantalla muestra el indicador de cambio de insignia, con el reloj de composición controlado
        composeTestRule.mainClock.autoAdvance = false
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(
                current = item(),
                busy = true,
                badgeChange = BadgeChange(BadgeChange.Direction.DOWN, "Plata", fromLevel = 3, toLevel = 2),
            ),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action == PracticeSessionAction.ConsumeBadgeChange) {
                            uiState = (uiState as PracticeSessionUiState.Practicing).copy(
                                badgeChange = null,
                                busy = false,
                            )
                        }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertExists()

        // When: transcurre tiempo de sobra, sin que el usuario interactúe con la pantalla
        composeTestRule.mainClock.advanceTimeBy(5_000)
        composeTestRule.waitForIdle()

        // Then: el indicador se ocultó por sí solo
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertDoesNotExist()
    }

    // Scenario: La bajada permanece visible menos tiempo que la subida (spec: add-level-change-animation)
    @Test
    fun givenUpBadgeChange_whenIntermediateTimeElapses_thenIndicatorStaysVisible() {
        // Given: el indicador de cambio de insignia con dirección UP, con el reloj de composición controlado
        composeTestRule.mainClock.autoAdvance = false
        setScreen(
            practicing(
                current = item(),
                busy = true,
                badgeChange = BadgeChange(BadgeChange.Direction.UP, "Oro", fromLevel = 2, toLevel = 3),
            ),
        )
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertExists()

        // When: transcurre un tiempo intermedio, mayor que la duración de DOWN pero menor que la de UP
        composeTestRule.mainClock.advanceTimeBy(INTERMEDIATE_DURATION_MILLIS)

        // Then: el indicador sigue presente, porque UP dura más que ese tiempo intermedio
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertExists()
    }

    // Scenario: La bajada permanece visible menos tiempo que la subida (spec: add-level-change-animation)
    @Test
    fun givenDownBadgeChange_whenIntermediateTimeElapses_thenIndicatorIsGone() {
        // Given: el indicador de cambio de insignia con dirección DOWN, con el reloj de composición controlado
        composeTestRule.mainClock.autoAdvance = false
        var uiState: PracticeSessionUiState by mutableStateOf(
            practicing(
                current = item(),
                busy = true,
                badgeChange = BadgeChange(BadgeChange.Direction.DOWN, "Plata", fromLevel = 3, toLevel = 2),
            ),
        )
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(
                    uiState = uiState,
                    onAction = { action ->
                        if (action == PracticeSessionAction.ConsumeBadgeChange) {
                            uiState = (uiState as PracticeSessionUiState.Practicing).copy(
                                badgeChange = null,
                                busy = false,
                            )
                        }
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertExists()

        // When: transcurre el mismo tiempo intermedio, mayor que la duración de DOWN
        composeTestRule.mainClock.advanceTimeBy(INTERMEDIATE_DURATION_MILLIS)
        composeTestRule.waitForIdle()

        // Then: el indicador ya no está presente, porque DOWN dura menos que ese tiempo intermedio
        composeTestRule.onNodeWithTag("practice_badge_change_indicator").assertDoesNotExist()
    }

    // Scenario: La transición no oculta el resto de la tarjeta (spec: new-badge-animation)
    @Test
    fun givenBadgeChange_whenScreenShown_thenAnsweredPhraseStaysVisibleAndIndicatorIsAtCefrLabelHeight() {
        // Given: una autoevaluación que cambia el nivel de consolidación de la frase respondida, revelada
        setScreen(
            practicing(
                current = item(source = "Buenos días", cefrLevel = CefrLevel.B1),
                revealed = true,
                busy = true,
                badgeChange = BadgeChange(BadgeChange.Direction.UP, "Oro", fromLevel = 2, toLevel = 3),
            ),
        )

        // When: la pantalla muestra la transición de insignia sobre esa frase (estado dado)

        // Then: la frase respondida sigue visible (no una frase siguiente), y el indicador queda a la
        // misma altura que la etiqueta del nivel CEFR, no centrado en toda la pantalla
        composeTestRule.onNodeWithText("Buenos días").assertIsDisplayed()
        val cefrLabelTop = composeTestRule.onNodeWithText("B1").getUnclippedBoundsInRoot().top
        val indicatorTop = composeTestRule.onNodeWithTag("practice_badge_change_indicator")
            .getUnclippedBoundsInRoot().top
        assertTrue(abs((indicatorTop - cefrLabelTop).value) <= ROW_ALIGNMENT_TOLERANCE_DP)
    }

    /** Texto lo bastante largo para desbordar la pantalla del test en cualquiera de sus tamaños de letra. */
    private fun veryLongText(prefix: String = "palabra"): String =
        (1..40).joinToString(separator = " ") { "$prefix$it" }

    /** Ancho de un [DpRect], no expuesto como propiedad por la API de Compose UI. */
    private fun DpRect.widthValue(): Dp = right - left

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
        badgeChange: BadgeChange? = null,
    ): PracticeSessionUiState.Practicing = PracticeSessionUiState.Practicing(
        current = current,
        revealed = revealed,
        busy = busy,
        completed = completed,
        index = 1,
        total = 1,
        remainingHitsForNextLevel = 0,
        nextLevelBadgeLabel = "Bronce",
        badgeChange = badgeChange,
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
