package com.dalmuina.fraseafrase.feature.session

import app.cash.turbine.test
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * `HideTranslation` (ADR-0007): vive en una clase propia, separada de [PracticeSessionViewModelTest], para no
 * superar el límite de tamaño de clase de detekt (mismo motivo que separa [PracticeSessionCloseTest] y
 * [PracticeSessionRevertTest]). Comparte los builders de estado con esas clases a través de
 * `PracticeSessionFixtures.kt`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionHideTranslationTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    // Scenario: no hay escenario de spec propio; corrección visual del hallazgo 4 de la tarea 11.3
    // (adopt-reference-mock-ui, tarea 12.3): ocultar de nuevo la traducción es el inverso de Reveal.
    @Test
    fun `given a revealed phrase when hiding the translation then the target text is hidden again`() = runTest {
        // Given: la pantalla muestra la frase A revelada
        val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
        val catalog = loaded(
            aCatalogPhrase(id = "A", source = "Buenos días", target = "Bom dia"),
            aCatalogPhrase(id = "B"),
        )
        val viewModel = sessionViewModel(practice, catalog)
        advanceUntilIdle()
        viewModel.onAction(PracticeSessionAction.Reveal)

        // When
        viewModel.onAction(PracticeSessionAction.HideTranslation)

        // Then
        viewModel.uiState.test {
            val state = awaitItem().shouldBeInstanceOf<PracticeSessionUiState.Practicing>()
            state.revealed shouldBe false
        }
    }
}
