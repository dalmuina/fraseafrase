package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    /** Frase certificada en la tanda: racha 3 respondida. */
    private fun certified(id: String) = aPhraseProgress(id = id, streak = Streak(3), answeredInBatch = true)

    private fun loaded(vararg ids: String) = CatalogLoadResult.Loaded(ids.map { aCatalogPhrase(id = it) })

    private fun activeBatch(order: List<String>, progress: List<PhraseProgress>) =
        PracticeSnapshot(
            progress = progress,
            activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
            carry = CarryQueue(emptyList()),
        )

    // Scenario: Tanda con certificadas y no certificadas (spec: progress-overview)
    @Test
    fun `given certified and uncertified phrases when opening progress then it shows the certified count`() =
        runTest {
            // Given: tanda activa de cinco frases, dos con racha 3 y tres sin certificar
            val progress = listOf(certified("A"), certified("B")) +
                listOf("C", "D", "E").map { aPhraseProgress(id = it) }
            val practice = FakePracticeRepository(
                activeBatch(order = listOf("A", "B", "C", "D", "E"), progress = progress),
            )
            val catalog = loaded("A", "B", "C", "D", "E")

            // When: el usuario abre la pantalla de progreso
            val viewModel = progressViewModel(catalog, practice)
            advanceUntilIdle()

            // Then: la pantalla muestra 2 de 5 frases certificadas
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.certifiedInBatch shouldBe CertifiedCount(certified = 2, total = 5)
        }

    // Scenario: Sin tanda activa muestra solo el total por nivel (spec: progress-overview)
    @Test
    fun `given no active batch when opening progress then it shows a null certified count and the total by level`() =
        runTest {
            // Given: no hay tanda activa y un catálogo de seis frases sin progreso
            val catalog = loaded("A", "B", "C", "D", "E", "F")

            // When: el usuario abre la pantalla de progreso
            val viewModel = progressViewModel(catalog)
            advanceUntilIdle()

            // Then: la pantalla no muestra certificadas y muestra el nivel 0 con 6 frases
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.certifiedInBatch shouldBe null
            state.byLevel shouldContainExactly mapOf(
                Level(0) to 6,
                Level(1) to 0,
                Level(2) to 0,
                Level(3) to 0,
                Level(4) to 0,
                Level(5) to 0,
            )
        }

    // Scenario: Catálogo ilegible al abrir el progreso (spec: progress-overview)
    @Test
    fun `given an unreadable catalog when opening progress then the ui state is a catalog error`() = runTest {
        // Given: un catálogo que no se puede leer
        val catalog = CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)

        // When: el usuario abre la pantalla de progreso
        val viewModel = progressViewModel(catalog)
        advanceUntilIdle()

        // Then: la pantalla muestra un estado de error de catálogo
        viewModel.uiState.value shouldBe ProgressUiState.CatalogError
    }
}
