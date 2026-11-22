package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.MainDispatcherExtension
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionSourceLanguageTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    private fun catalogs() = FakeCatalogRepository.withCatalogs(
        mapOf(
            LanguageTag("pt-PT") to catalogOf("A", "B"),
            LanguageTag("es-ES") to loaded(
                aCatalogPhrase(id = "A", target = "Hola"),
                aCatalogPhrase(id = "B", target = "Adios"),
            ),
            LanguageTag("en-GB") to loaded(
                aCatalogPhrase(id = "A", target = "Hello"),
                aCatalogPhrase(id = "B", target = "Goodbye"),
            ),
        ),
    )

    // Scenario: Volver a Practicar tras cambiar el idioma del dispositivo muestra el nuevo texto de origen
    @Test
    fun `given a loaded batch when the device language changes and Resume is dispatched then new source is shown`() =
        runTest {
            // Given: tanda A, B con el dispositivo en es-ES, frase revelada
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
            val device = FakeDeviceLocaleProvider(LanguageTag("es-ES"))
            val viewModel = sessionViewModelWith(practice, catalogs(), deviceLocaleProvider = device)
            advanceUntilIdle()
            viewModel.practicing().current.source shouldBe "Hola"
            viewModel.onAction(PracticeSessionAction.Reveal)

            // When
            device.locale = LanguageTag("en-GB")
            viewModel.onAction(PracticeSessionAction.Resume)
            advanceUntilIdle()

            // Then: texto en-GB, sin revelar, misma frase y posición
            val after = viewModel.practicing()
            after.current.source shouldBe "Hello"
            after.current.id shouldBe PhraseId("A")
            after.revealed shouldBe false
            after.index shouldBe 1
        }

    // Scenario: Volver a Practicar sin cambios conserva el estado
    @Test
    fun `given a revealed phrase when Resume is dispatched with the same device language then state is kept`() =
        runTest {
            // Given
            val practice = FakePracticeRepository(activeBatchSnapshot(order = listOf("A", "B")))
            val viewModel = sessionViewModelWith(practice, catalogs())
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            val before = viewModel.practicing()

            // When
            viewModel.onAction(PracticeSessionAction.Resume)
            advanceUntilIdle()

            // Then
            viewModel.practicing() shouldBe before
        }
}
