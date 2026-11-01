package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
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

    // Scenario: Aviso con frases no certificadas (spec: practice-session)
    @Test
    fun givenThreeUncertifiedPhrasesWhenCloseWarningIsOpenThenDialogShowsTheCount() {
        // Given: el aviso de cierre indica tres frases no certificadas
        setScreen(practicing(current = item(), revealed = true, closeWarning = 3))

        // When: el diálogo está abierto (estado dado)

        // Then: el diálogo muestra "3 frases pasarán a la siguiente tanda"
        composeTestRule.onNodeWithText("3 frases pasarán a la siguiente tanda").assertIsDisplayed()
    }

    // Scenario: Aviso sin frases no certificadas (spec: practice-session)
    @Test
    fun givenNoUncertifiedPhrasesWhenCloseWarningIsOpenThenDialogShowsZero() {
        // Given: el aviso de cierre indica cero frases no certificadas
        setScreen(practicing(current = item(), revealed = true, closeWarning = 0))

        // When: el diálogo está abierto (estado dado)

        // Then: el diálogo muestra "0 frases pasarán a la siguiente tanda"
        composeTestRule.onNodeWithText("0 frases pasarán a la siguiente tanda").assertIsDisplayed()
    }

    // Scenario: Aviso con una sola frase no certificada (spec: practice-session)
    @Test
    fun givenOneUncertifiedPhraseWhenCloseWarningIsOpenThenDialogUsesTheSingularForm() {
        // Given: el aviso de cierre indica una frase no certificada
        setScreen(practicing(current = item(), revealed = true, closeWarning = 1))

        // When: el diálogo está abierto (estado dado)

        // Then: el diálogo muestra la forma singular "1 frase pasará a la siguiente tanda"
        composeTestRule.onNodeWithText("1 frase pasará a la siguiente tanda").assertIsDisplayed()
    }

    // Scenario: Frase con racha 3 marcada como sabida (spec: practice-session)
    @Test
    fun givenPhraseWithStreakThreeWhenShowingItThenItIsMarkedAsKnown() {
        // Given: la frase A tiene racha 3 y está sin revelar
        setScreen(practicing(current = item(streak = 3, isKnown = true)))

        // When: la pantalla muestra A (estado dado)

        // Then: la pantalla marca A como sabida y muestra su racha
        composeTestRule.onNodeWithText("Sabida").assertIsDisplayed()
        composeTestRule.onNodeWithText("Racha 3").assertIsDisplayed()
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

    // Scenario: Cerrar mientras hay una respuesta en curso no abre el aviso (spec: practice-session)
    @Test
    fun givenAnswerInProgressWhenShowingTheScreenThenTheCloseButtonIsDisabled() {
        // Given: una frase revelada cuya autoevaluación se está registrando (busy = true)
        setScreen(practicing(current = item(), revealed = true, busy = true))

        // When: la pantalla muestra el estado con la respuesta en curso (estado dado)

        // Then: el botón de cerrar la tanda está deshabilitado, así que no se puede pedir el cierre
        composeTestRule.onNodeWithText("Cerrar tanda").assertIsNotEnabled()
    }

    private fun setScreen(uiState: PracticeSessionUiState) {
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionScreen(uiState = uiState, onAction = {})
            }
        }
    }

    private fun practicing(
        current: PracticeItem? = item(),
        revealed: Boolean = false,
        busy: Boolean = false,
        closeWarning: Int? = null,
    ): PracticeSessionUiState.Practicing = PracticeSessionUiState.Practicing(
        current = current,
        revealed = revealed,
        busy = busy,
        closeWarning = closeWarning,
    )

    /** Frase de presentación. `isKnown` lo decide el dominio (`ObtainPracticeSession`); aquí se pasa tal cual. */
    private fun item(
        source: String = "Buenos días",
        target: String = "Bom dia",
        streak: Int = 0,
        isKnown: Boolean = false,
    ): PracticeItem = PracticeItem(
        id = PhraseId("A"),
        source = source,
        target = target,
        streak = Streak(streak),
        isKnown = isKnown,
    )
}
