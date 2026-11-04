package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    // Scenario: Se muestran los ajustes guardados (spec: settings-screen)
    @Test
    fun `given settings stored when the screen opens then the ui state shows them`() = runTest {
        // Given: un nivel CEFR B1 y N igual a 25 guardados
        val repository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(25)))

        // When: el usuario abre la pantalla de ajustes
        val viewModel = settingsViewModel(repository)
        advanceUntilIdle()

        // Then: la pantalla muestra el nivel CEFR B1 y N igual a 25
        val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
        state.cefrLevel shouldBe CefrLevel.B1
        state.batchSizeText shouldBe "25"
        state.isBatchSizeValid shouldBe true
    }

    // Scenario: Elegir un nivel CEFR de la lista (spec: settings-screen)
    @Test
    fun `given a cefr level selected action when dispatched then the ui state shows the new level`() = runTest {
        // Given: la pantalla de ajustes abierta con nivel CEFR A1
        val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository)
        advanceUntilIdle()

        // When: el usuario elige el nivel B1
        viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))

        // Then: la pantalla muestra B1 como nivel CEFR seleccionado
        val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
        state.cefrLevel shouldBe CefrLevel.B1
    }

    // Scenarios: N fuera de rango impide guardar / N no numérico impide guardar (spec: settings-screen)
    @Test
    fun `given an invalid batch size text when changed then the ui state is invalid and saving is disabled`() =
        runTest {
            // Given: la pantalla de ajustes abierta
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario escribe 31 como tamaño de tanda
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("31"))

            // Then: la pantalla indica que el valor no es válido y la acción de guardar no está disponible
            var state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.batchSizeText shouldBe "31"
            state.isBatchSizeValid shouldBe false

            // When: el usuario escribe "veinte" como tamaño de tanda
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("veinte"))

            // Then: la pantalla indica que el valor no es válido y la acción de guardar no está disponible
            state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.batchSizeText shouldBe "veinte"
            state.isBatchSizeValid shouldBe false
        }

    // Scenario: N dentro de rango permite guardar (spec: settings-screen)
    @Test
    fun `given a batch size text in range when changed then saving is enabled`() = runTest {
        // Given: la pantalla de ajustes abierta
        val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository)
        advanceUntilIdle()

        // When: el usuario escribe 25 como tamaño de tanda
        viewModel.onAction(SettingsAction.ChangeBatchSizeText("25"))

        // Then: la acción de guardar está disponible
        val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
        state.batchSizeText shouldBe "25"
        state.isBatchSizeValid shouldBe true
    }

    // Scenario: Guardar persiste el nuevo nivel CEFR y N (spec: settings-screen)
    @Test
    fun `given a valid cefr level and batch size when Save is dispatched then a later read reflects them`() =
        runTest {
            // Given: la pantalla de ajustes abierta con nivel CEFR A1 y N igual a 20 guardados
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario elige el nivel B2, escribe 15 como tamaño de tanda y confirma el guardado
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B2))
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: una lectura posterior de los ajustes devuelve el nivel CEFR B2 y N igual a 15
            repository.settings() shouldBe UserSettings(CefrLevel.B2, BatchSize(15))
        }

    // Scenario: Salir sin guardar no persiste la edición (spec: settings-screen)
    @Test
    fun `given an unconfirmed batch size edit when discarded then a later read reflects the previous value`() =
        runTest {
            // Given: la pantalla de ajustes abierta con nivel CEFR A1 y N igual a 20 guardados, editando sin guardar
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))

            // When: el usuario sale de la pantalla de ajustes sin confirmar el guardado (el ViewModel se descarta
            // y uno nuevo lee los ajustes)
            val newViewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // Then: una lectura posterior de los ajustes devuelve N igual a 20
            val state = newViewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.batchSizeText shouldBe "20"
            repository.settings() shouldBe UserSettings(CefrLevel.A1, BatchSize(20))
        }
}
