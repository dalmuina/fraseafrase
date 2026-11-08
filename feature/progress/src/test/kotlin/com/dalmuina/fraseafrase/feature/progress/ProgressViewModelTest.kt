package com.dalmuina.fraseafrase.feature.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
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

    private fun loaded(vararg ids: String, level: CefrLevel = CefrLevel.A1) =
        CatalogLoadResult.Loaded(ids.map { aCatalogPhrase(id = it, level = level) })

    // Scenario: El total coincide con la suma de los niveles (spec: progress-overview)
    @Test
    fun `given a catalog with phrases in several levels when opening progress then the total matches the sum`() =
        runTest {
            // Given: un catálogo de diez frases y el nivel CEFR B1 guardado
            val catalog = loaded("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", level = CefrLevel.B1)
            val settings = UserSettings(cefrLevel = CefrLevel.B1, batchSize = UserSettings.DEFAULT.batchSize)

            // When: el usuario abre la pantalla de progreso
            val viewModel = progressViewModel(catalog, settings = settings)
            advanceUntilIdle()

            // Then: la tarjeta destacada muestra 10 frases disponibles y el nivel CEFR B1
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.totalAvailable shouldBe 10
            state.cefrLevel shouldBe CefrLevel.B1
            state.byLevel.values.sum() shouldBe state.totalAvailable
        }

    // Scenario: El total se actualiza si cambia el catálogo disponible (spec: progress-overview)
    @Test
    fun `given a catalog of six phrases when opening progress then the featured card shows six phrases available`() =
        runTest {
            // Given: un catálogo de seis frases sin progreso
            val catalog = loaded("A", "B", "C", "D", "E", "F")

            // When: el usuario abre la pantalla de progreso
            val viewModel = progressViewModel(catalog)
            advanceUntilIdle()

            // Then: la tarjeta destacada muestra 6 frases disponibles
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.totalAvailable shouldBe 6
        }

    // Scenario: Sin tanda activa muestra solo el total por nivel (spec: progress-overview)
    @Test
    fun `given no active batch when opening progress then it shows the total the cefr level and the levels`() =
        runTest {
            // Given: no hay tanda activa, un catálogo de seis frases sin progreso y el nivel CEFR B1 guardado
            val catalog = loaded("A", "B", "C", "D", "E", "F", level = CefrLevel.B1)
            val settings = UserSettings(cefrLevel = CefrLevel.B1, batchSize = UserSettings.DEFAULT.batchSize)

            // When: el usuario abre la pantalla de progreso
            val viewModel = progressViewModel(catalog, settings = settings)
            advanceUntilIdle()

            // Then: la pantalla muestra 6 frases disponibles, el nivel CEFR B1 y el nivel 0 con 6 frases
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.totalAvailable shouldBe 6
            state.cefrLevel shouldBe CefrLevel.B1
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
