package com.dalmuina.fraseafrase.feature.progress

import app.cash.turbine.test
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
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

    // Scenario: El icono muestra el idioma activo en Progreso (spec: language-indicator)
    @Test
    fun `given en-GB as the active target locale when opening progress then the ui state exposes it`() = runTest {
        // Given: en-GB como idioma destino activo, con un catálogo de seis frases
        val catalog = loaded("A", "B", "C", "D", "E", "F")

        // When: el usuario abre la pantalla de progreso
        val viewModel = progressViewModel(catalog, activeTargetLocale = LanguageTag("en-GB"))
        advanceUntilIdle()

        // Then: el estado expone en-GB como idioma destino activo
        val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
        state.activeTargetLocale shouldBe LanguageTag("en-GB")
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

    // Scenario: Volver a Progreso tras cambiar el idioma destino activo (spec: progress-overview)
    @Test
    fun `given progress opened when the active language changes and it resumes then it shows the new language`() =
        runTest {
            // Given: pt-PT con seis frases y en-GB con cuatro, y progreso abierto en pt-PT
            val ptPt = LanguageTag("pt-PT")
            val enGb = LanguageTag("en-GB")
            val settingsRepository = FakeSettingsRepository()
            val catalogRepository = FakeCatalogRepository.withCatalogs(
                mapOf(
                    ptPt to loaded("A", "B", "C", "D", "E", "F"),
                    enGb to loaded("G", "H", "I", "J"),
                ),
            )
            val viewModel = progressViewModel(settingsRepository, catalogRepository)
            advanceUntilIdle()

            // When: se cambia el idioma activo a en-GB y se vuelve a mostrar la pantalla
            settingsRepository.setActiveTargetLocale(enGb)
            viewModel.onAction(ProgressAction.Resume)
            advanceUntilIdle()

            // Then: 4 frases disponibles, 4 en el nivel 0 y en-GB como idioma activo
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.totalAvailable shouldBe 4
            state.byLevel[Level(0)] shouldBe 4
            state.activeTargetLocale shouldBe enGb
        }

    // Scenario: Volver a Progreso tras aplicar otro nivel CEFR (spec: progress-overview)
    @Test
    fun `given progress opened when another cefr level is saved and it resumes then it shows the new level`() =
        runTest {
            // Given: B1 vigente, seis frases B1 y cuatro C1, y progreso abierto
            val settingsRepository = FakeSettingsRepository(
                UserSettings(cefrLevel = CefrLevel.B1, batchSize = UserSettings.DEFAULT.batchSize),
            )
            val b1Phrases = listOf("A", "B", "C", "D", "E", "F").map { aCatalogPhrase(id = it, level = CefrLevel.B1) }
            val c1Phrases = listOf("G", "H", "I", "J").map { aCatalogPhrase(id = it, level = CefrLevel.C1) }
            val phrases = b1Phrases + c1Phrases
            val catalogRepository = FakeCatalogRepository.withCatalogs(
                mapOf(FakeSettingsRepository.DEFAULT_LOCALE to CatalogLoadResult.Loaded(phrases)),
            )
            val viewModel = progressViewModel(settingsRepository, catalogRepository)
            advanceUntilIdle()

            // When: se guarda C1 y se vuelve a mostrar la pantalla
            settingsRepository.save(UserSettings(cefrLevel = CefrLevel.C1, batchSize = UserSettings.DEFAULT.batchSize))
            viewModel.onAction(ProgressAction.Resume)
            advanceUntilIdle()

            // Then: 4 frases disponibles y nivel C1
            val state = viewModel.uiState.value.shouldBeInstanceOf<ProgressUiState.Content>()
            state.totalAvailable shouldBe 4
            state.cefrLevel shouldBe CefrLevel.C1
        }

    // Scenario: Volver a Progreso sin cambios no pasa por el estado de carga (spec: progress-overview)
    @Test
    fun `given progress opened when it resumes without changes then no new state is emitted`() = runTest {
        // Given: progreso abierto con seis frases
        val viewModel = progressViewModel(loaded("A", "B", "C", "D", "E", "F"))
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem().shouldBeInstanceOf<ProgressUiState.Content>()

            // When: se vuelve a mostrar la pantalla sin cambiar nada
            viewModel.onAction(ProgressAction.Resume)
            advanceUntilIdle()

            // Then: no se emite ningún estado (ni Loading)
            expectNoEvents()
        }
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
