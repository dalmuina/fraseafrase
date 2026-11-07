package com.dalmuina.fraseafrase.feature.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
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

    // Scenario: Guardar sin cambiar ningún valor se queda en ajustes (spec: settings-screen)
    @Test
    fun `given unchanged cefr level and batch size when Save is dispatched then it only confirms saving`() =
        runTest {
            // Given: la pantalla de ajustes abierta con el nivel CEFR B1 y N igual a 20, ambos persistidos
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.B1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario confirma el guardado sin cambiar ningún valor
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: se confirma el guardado, sin pedir cerrar la tanda activa ni navegar
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.saveConfirmationPending shouldBe true
            state.navigateToPracticePending shouldBe false
        }

    // Scenario: Cambiar el nivel CEFR no toca el progreso (spec: settings-screen)
    @Test
    fun `given a changed cefr level when Save is dispatched then it regenerates without touching progress`() =
        runTest {
            // Given: una tanda activa guardada, con una frase de racha 2, y el nivel CEFR A1 persistido
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(10)))
            val practice = FakePracticeRepository(
                activeBatchSnapshotWithProgress(level = Level(1), streak = Streak(2), dueAtSession = 5),
            )
            val catalog = CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "phrase-1", level = CefrLevel.A1)) +
                    (1..10).map { aCatalogPhrase(id = "new-b1-$it", level = CefrLevel.B1) },
            )
            val viewModel = settingsViewModel(repository, practice, catalog = catalog)
            advanceUntilIdle()

            // When: el usuario elige el nivel B1 y confirma el guardado
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: la tanda activa se recompone con frases nuevas de nivel B1 y la frase de racha 2 conserva su
            // racha, su nivel de consolidación y su dueAtSession sin cambios
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.saveConfirmationPending shouldBe true
            state.navigateToPracticePending shouldBe true
            practice.read().activeBatch shouldNotBe null
            val progress = practice.read().progress.first { it.id.value == "phrase-1" }
            progress.level shouldBe Level(1)
            progress.streak shouldBe Streak(2)
            progress.dueAtSession shouldBe 5
        }

    // Scenario: Cambiar N no toca el progreso (spec: settings-screen)
    @Test
    fun `given a changed batch size when Save is dispatched then it regenerates without touching progress`() =
        runTest {
            // Given: una tanda activa guardada y N igual a 20 persistido, con una frase de racha 2 ya pendiente
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val practice = FakePracticeRepository(
                activeBatchSnapshotWithProgress(level = Level(1), streak = Streak(2), dueAtSession = 0),
            )
            val catalog = CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "phrase-1", level = CefrLevel.A1)))
            val viewModel = settingsViewModel(repository, practice, catalog = catalog)
            advanceUntilIdle()

            // When: el usuario escribe 15 como tamaño de tanda y confirma el guardado
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: la tanda activa se recompone con tamaño 15 y la frase de racha 2 conserva su racha, su nivel
            // de consolidación y su dueAtSession sin cambios
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.saveConfirmationPending shouldBe true
            state.navigateToPracticePending shouldBe true
            practice.read().activeBatch shouldNotBe null
            val progress = practice.read().progress.first { it.id.value == "phrase-1" }
            progress.level shouldBe Level(1)
            progress.streak shouldBe Streak(2)
            progress.dueAtSession shouldBe 0
        }

    // Scenario: Cambiar N y el nivel CEFR a la vez tampoco toca el progreso (spec: settings-screen)
    @Test
    fun `given both changed when Save is dispatched then it regenerates without touching progress`() =
        runTest {
            // Given: una tanda activa guardada, el nivel CEFR A1 y N igual a 20, ambos persistidos
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val practice = FakePracticeRepository(
                activeBatchSnapshotWithProgress(level = Level(1), streak = Streak(2), dueAtSession = 5),
            )
            val catalog = CatalogLoadResult.Loaded(
                listOf(aCatalogPhrase(id = "phrase-1", level = CefrLevel.A1)) +
                    (1..10).map { aCatalogPhrase(id = "new-b1-$it", level = CefrLevel.B1) },
            )
            val viewModel = settingsViewModel(repository, practice, catalog = catalog)
            advanceUntilIdle()

            // When: el usuario elige el nivel B1, escribe 15 como tamaño de tanda y confirma el guardado
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: la tanda activa se recompone con nivel B1 y N igual a 15, sin certificar ni tocar el progreso
            // de la frase de racha 2
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.saveConfirmationPending shouldBe true
            state.navigateToPracticePending shouldBe true
            practice.read().activeBatch shouldNotBe null
            val progress = practice.read().progress.first { it.id.value == "phrase-1" }
            progress.level shouldBe Level(1)
            progress.streak shouldBe Streak(2)
            progress.dueAtSession shouldBe 5
        }

    // Scenario: Ningún cambio de ajustes incrementa el contador de sesiones (spec: settings-screen)
    @Test
    fun `given a session count when the cefr level and batch size change then Save does not increase it`() =
        runTest {
            // Given: el contador de sesiones generadas en 9
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val practice = FakePracticeRepository(activeBatchSnapshot(sessionCount = 9))
            val catalog = CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "phrase-1", level = CefrLevel.A1)))
            val viewModel = settingsViewModel(repository, practice, catalog = catalog)
            advanceUntilIdle()

            // When: el usuario cambia el nivel CEFR, N, y confirma el guardado
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: el contador de sesiones generadas sigue en 9
            practice.read().sessionCount shouldBe 9
        }

    // Scenario: Sin tanda activa no hay nada que recomponer (spec: settings-screen)
    @Test
    fun `given no active batch when a different cefr level is saved then it completes without error or warning`() =
        runTest {
            // Given: no hay tanda activa guardada, con el nivel CEFR A1 persistido
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(10)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario elige el nivel B1, mantiene la N persistida y confirma el guardado
            viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: el guardado se completa sin error, pidiendo confirmación pero sin aviso de cierre
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.saveConfirmationPending shouldBe true
        }

    // Scenario: Sin tanda activa no hay nada que recomponer (spec: settings-screen)
    @Test
    fun `given no active batch when the batch size is changed and saved then it completes without error or warning`() =
        runTest {
            // Given: no hay tanda activa guardada
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val viewModel = settingsViewModel(repository)
            advanceUntilIdle()

            // When: el usuario cambia N y confirma el guardado
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: el guardado se completa sin error y sin ningún aviso de cierre
            val state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.saveConfirmationPending shouldBe true
        }

    // Design D2: la base de comparación se actualiza en cada guardado, no se queda fija en la de `reload()`
    @Test
    fun `given two consecutive saves when the second changes the value again then it is also detected as changed`() =
        runTest {
            // Given: la pantalla de ajustes abierta con N igual a 20 persistido
            val repository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
            val practiceForFirstSave = FakePracticeRepository(activeBatchSnapshot())
            val catalog = CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "phrase-1", level = CefrLevel.A1)))
            val viewModel = settingsViewModel(repository, practiceForFirstSave, catalog = catalog)
            advanceUntilIdle()

            // When: un primer guardado cambia N a 15
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: se detecta como cambio
            var state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.navigateToPracticePending shouldBe true

            // When: con el mismo ViewModel, un segundo guardado repite el mismo valor (N = 15 otra vez)
            viewModel.onAction(SettingsAction.ConsumeNavigateToPractice)
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("15"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: NO se detecta como cambio
            state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.navigateToPracticePending shouldBe false

            // When: un tercer guardado cambia N a 20
            viewModel.onAction(SettingsAction.ChangeBatchSizeText("20"))
            viewModel.onAction(SettingsAction.Save)
            advanceUntilIdle()

            // Then: se vuelve a detectar como cambio
            state = viewModel.uiState.value.shouldBeInstanceOf<SettingsUiState.Loaded>()
            state.navigateToPracticePending shouldBe true
        }
}
