package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
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

    // Scenario: Se muestran los ajustes guardados, con el idioma destino (spec: settings-screen)
    @Test
    fun `given an active target locale and available languages when the screen opens then the ui state shows them`() =
        runTest {
            // Given: pt-PT como idioma destino activo con pt-BR también disponible
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(25)))

            // When: el usuario abre la pantalla de ajustes
            val viewModel = settingsViewModel(
                repository,
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("pt-BR")),
            )
            advanceUntilIdle()

            // Then: la pantalla muestra pt-PT como idioma destino activo y ambos idiomas como disponibles
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.activeTargetLocale shouldBe LanguageTag("pt-PT")
            state.availableTargetLocales shouldBe listOf(LanguageTag("pt-PT"), LanguageTag("pt-BR"))
        }

    // Scenario: Cambiar de idioma destino muestra sus propios ajustes (spec: settings-screen)
    @Test
    fun `given settings per locale when a different language is selected then the shown cefr and batch size update`() =
        runTest {
            // Given: pt-PT activo con CEFR B1/N=25, y pt-BR con CEFR A2/N=10
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(25)))
            repository.save(LanguageTag("pt-BR"), UserSettings(CefrLevel.A2, BatchSize(10)))
            val viewModel = settingsViewModel(
                repository,
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("pt-BR")),
            )
            advanceUntilIdle()

            // When: el usuario elige pt-BR en el desplegable
            viewModel.onAction(SettingsAction.SelectLanguage(LanguageTag("pt-BR")))
            advanceUntilIdle()

            // Then: la pantalla muestra el nivel CEFR A2 y N igual a 10
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.activeTargetLocale shouldBe LanguageTag("pt-BR")
            state.cefrLevel shouldBe CefrLevel.A2
            state.batchSizeText shouldBe "10"
        }

    // Scenario: Elegir un idioma destino lo activa sin salir de ajustes (spec: settings-screen, "Elegir un idioma
    // destino lo activa de inmediato")
    @Test
    fun `given an active target locale when a different language is selected then it is activated immediately`() =
        runTest {
            // Given: la pantalla de ajustes abierta con pt-PT como idioma destino activo
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(
                repository,
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("pt-BR")),
            )
            advanceUntilIdle()

            // When: el usuario elige pt-BR en el desplegable
            viewModel.onAction(SettingsAction.SelectLanguage(LanguageTag("pt-BR")))
            advanceUntilIdle()

            // Then: una lectura posterior del idioma destino activo devuelve pt-BR, sin ninguna acción adicional
            repository.activeTargetLocale() shouldBe LanguageTag("pt-BR")
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
    fun `given an invalid batch size text when changed then the ui state is invalid and it is not autosaved`() =
        runTest {
            // Given: la pantalla de ajustes abierta con N igual a 20 guardado
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario escribe 31 como tamaño de tanda
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("31"))
            advanceUntilIdle()

            // Then: la pantalla indica que el valor no es válido y los ajustes persistidos siguen en N igual a 20
            var state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.batchSizeText shouldBe "31"
            state.isBatchSizeValid shouldBe false
            repository.settings() shouldBe UserSettings(CefrLevel.A1, BatchSize(20))

            // When: el usuario escribe "veinte" como tamaño de tanda
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("veinte"))
            advanceUntilIdle()

            // Then: la pantalla indica que el valor no es válido y los ajustes persistidos siguen en N igual a 20
            state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.batchSizeText shouldBe "veinte"
            state.isBatchSizeValid shouldBe false
            repository.settings() shouldBe UserSettings(CefrLevel.A1, BatchSize(20))
        }

    // Scenario: N dentro de rango permite guardar (spec: settings-screen, "Guardado de los ajustes editados")
    @Test
    fun `given a batch size text in range when changed then a later read reflects it immediately`() = runTest {
        // Given: la pantalla de ajustes abierta
        val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository)
        advanceUntilIdle()

        // When: el usuario escribe 25 como tamaño de tanda
        viewModel.onAction(SettingsAction.ChangeBatchSizeText("25"))
        advanceUntilIdle()

        // Then: la pantalla lo refleja y una lectura posterior de los ajustes devuelve N igual a 25
        val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
        state.batchSizeText shouldBe "25"
        state.isBatchSizeValid shouldBe true
        repository.settings() shouldBe UserSettings(CefrLevel.A1, BatchSize(25))
    }

    // Scenario: Guardar persiste el nuevo nivel CEFR y N (spec: settings-screen, "Guardado de los ajustes editados")
    @Test
    fun `given a valid cefr level and batch size chosen when changed then a later read reflects them`() =
        runTest {
            // Given: la pantalla de ajustes abierta con nivel CEFR A1 y N igual a 20 guardados
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario elige el nivel B2 y escribe 15 como tamaño de tanda
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B2))
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            advanceUntilIdle()

            // Then: una lectura posterior de los ajustes devuelve el nivel CEFR B2 y N igual a 15, sin que el
            // usuario confirme ningún guardado
            repository.settings() shouldBe UserSettings(CefrLevel.B2, BatchSize(15))
        }

    // Scenario: Guardar tras cambiar de idioma destino persiste en el idioma destino elegido (spec: settings-screen)
    @Test
    fun `given a different target locale selected when cefr and batch size change then they persist for that locale`() =
        runTest {
            // Given: pt-PT activo con A1/20 por defecto, y pt-BR disponible
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(
                repository,
                availableTargetLocales = listOf(LanguageTag("pt-PT"), LanguageTag("pt-BR")),
            )
            advanceUntilIdle()

            // When: el usuario elige pt-BR, el nivel B2 y escribe 15 como tamaño de tanda
            viewModel.onAction(SettingsAction.SelectLanguage(LanguageTag("pt-BR")))
            advanceUntilIdle()
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B2))
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            advanceUntilIdle()

            // Then: una lectura posterior de pt-BR devuelve B2/15, pt-PT no cambia y pt-BR pasa a ser el activo
            repository.settings(LanguageTag("pt-BR")) shouldBe UserSettings(CefrLevel.B2, BatchSize(15))
            repository.settings(LanguageTag("pt-PT")) shouldBe UserSettings(CefrLevel.A1, BatchSize(20))
            repository.activeTargetLocale() shouldBe LanguageTag("pt-BR")
        }
}
