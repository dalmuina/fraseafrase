package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Generar sesión en un solo paso (design D3 de `replace-batch-closure-with-session-generation`). Los tests del
 * ViewModel usan los casos de uso reales.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionCloseTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    /** Frase certificada: racha 3 respondida en la tanda. */
    private fun certified(id: String): PhraseProgress =
        aPhraseProgress(id = id, streak = Streak(3), answeredInBatch = true)

    /** Frase ya certificada y en enfriamiento: no vence hasta mucho más adelante (spec: practice-batch, ADR-0017). */
    private fun certifiedWithFarCooldown(id: String): PhraseProgress =
        aPhraseProgress(id = id, level = Level(1), streak = Streak(0), answeredInBatch = true, dueAtSession = 100)

    // Scenario: Generar sesión muestra la tanda siguiente con sus arrastradas primero (spec: practice-session;
    // resultado nuevo: A, N1-N4, C)
    @Test
    fun `given C and A pending when generating session then the next batch interleaves new phrases between them`() =
        runTest {
            // Given: tanda C y A sin certificar; catálogo con A, C y 4 frases nuevas de A1; N igual a 10
            val newIds = (1..4).map { "N$it" }
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("C", "A")))
            val viewModel = sessionViewModel(
                practice,
                catalogOf("A", "C", *newIds.toTypedArray()),
                settings = UserSettings(cefrLevel = CefrLevel.A1, batchSize = BatchSize(10)),
            )
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.GenerateSession)
            advanceUntilIdle()

            // Then: pendientes en orden de catálogo (A antes que C) con 4 nuevas repartidas entre ellas
            viewModel.practicing().current.id.value shouldBe "A"
            practice.activeOrder() shouldBe listOf("A", "N1", "N2", "N3", "N4", "C")
        }

    // Scenario: Generar sesión con todas certificadas deja la tanda siguiente sin arrastradas (spec:
    // practice-session; desde ADR-0026 las certificadas sin vencer entran como repaso adelantado)
    @Test
    fun `given all phrases cooling down when generating session then they fill the next batch as early review`() =
        runTest {
            // Given: tanda de A y B certificadas, con enfriamiento lejos de vencer; el catálogo tiene N1 y N2 nuevas;
            // N igual a 10
            val certified = listOf(certifiedWithFarCooldown("A"), certifiedWithFarCooldown("B"))
            val practice = FakePracticeRepository(
                activeBatchSnapshot(order = listOf("A", "B"), progress = certified),
            )
            val viewModel = sessionViewModel(
                practice,
                catalogOf("A", "B", "N1", "N2"),
                settings = UserSettings(cefrLevel = CefrLevel.A1, batchSize = BatchSize(10)),
            )
            advanceUntilIdle()

            // When
            viewModel.onAction(PracticeSessionAction.GenerateSession)
            advanceUntilIdle()

            // Then: A y B conservan su nivel, su racha y su dueAtSession
            viewModel.practicing().current.id.value shouldBe "A"
            practice.activeOrder() shouldBe listOf("A", "N1", "N2", "B")
            practice.read().progress
                .filter { it.id.value in listOf("A", "B") }
                .map { listOf(it.id.value, it.level, it.streak, it.dueAtSession) } shouldBe
                certified.map { listOf(it.id.value, it.level, it.streak, it.dueAtSession) }
        }

    // Scenario: Catálogo ilegible al pedir el cierre (spec: practice-session, "Error de catálogo")
    @Test
    fun `given an unreadable catalog when generating session then the state is CatalogError`() = runTest {
        // Given: tanda activa con la frase A, ambas certificadas, y un catálogo que deja de poder leerse
        val practice = FakePracticeRepository(
            activeBatchSnapshot(order = listOf("A"), progress = listOf(certified("A"))),
        )
        val catalog = SwitchableCatalogRepository(catalogOf("A"))
        val viewModel = sessionViewModelWith(practice, catalog)
        advanceUntilIdle()
        catalog.result = CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)

        // When
        viewModel.onAction(PracticeSessionAction.GenerateSession)
        advanceUntilIdle()

        // Then: error de catálogo, sin mensaje de tanda completada
        viewModel.uiState.value shouldBe PracticeSessionUiState.CatalogError
    }

    // Scenario: Sin frases disponibles al pedir el cierre (spec: practice-session, "Estado vacío")
    @Test
    fun `given no active batch and no phrases left when generating session then the state is NothingToPractice`() =
        runTest {
            // Given: la tanda A ya no está activa; A tiene repaso futuro (sesión 2, cuando la siguiente sesión
            // generada es la 1) y el catálogo no tiene ninguna frase del nivel elegido (A1)
            val practice = FakePracticeRepository(
                activeBatchSnapshot(
                    order = listOf("A"),
                    progress = listOf(
                        aPhraseProgress(id = "A", level = Level(1), dueAtSession = 2),
                    ),
                ),
            )
            val viewModel = sessionViewModel(
                practice,
                loaded(aCatalogPhrase(id = "A", level = CefrLevel.B1)),
            )
            advanceUntilIdle()
            practice.transact { snapshot ->
                PracticeChange(next = snapshot.copy(activeBatch = null), result = Unit)
            }

            // When
            viewModel.onAction(PracticeSessionAction.GenerateSession)
            advanceUntilIdle()

            // Then: estado vacío
            viewModel.uiState.value shouldBe PracticeSessionUiState.NothingToPractice
        }

    // Scenario: Revelar se ignora con la tanda completada (design D3: guardas de Reveal)
    @Test
    fun `given a completed batch when revealing then the phrase stays hidden`() = runTest {
        // Given: tanda A y B, ambas certificadas, con la tanda completa
        val practice = FakePracticeRepository(
            activeBatchSnapshot(order = listOf("A", "B"), progress = listOf(certified("A"), certified("B"))),
        )
        val viewModel = sessionViewModel(practice, catalogOf("A", "B"))
        advanceUntilIdle()
        viewModel.practicing().completed shouldBe true

        // When: el usuario pide revelar con la tanda completada
        viewModel.onAction(PracticeSessionAction.Reveal)

        // Then: la frase sigue oculta
        viewModel.practicing().revealed shouldBe false
    }
}
