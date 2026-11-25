package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
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
class PracticeSessionTargetLanguageTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension(StandardTestDispatcher())

    private val german = LanguageTag("de-DE")

    private fun catalogs() = FakeCatalogRepository.withCatalogs(
        mapOf(
            LanguageTag("pt-PT") to loaded(
                aCatalogPhrase(id = "A", target = "Bom dia"),
                aCatalogPhrase(id = "B"),
            ),
            german to loaded(
                aCatalogPhrase(id = "A", target = "Guten Morgen").copy(targetLocale = german),
                aCatalogPhrase(id = "B").copy(targetLocale = german),
            ),
        ),
    )

    // Scenario: Mismos ids y mismo origen en otro idioma destino muestran el texto del idioma nuevo
    @Test
    fun `given same ids and source in another target language when Resume is dispatched then new target is shown`() =
        runTest {
            // Given: tanda A, B en pt-PT y en de-DE, frase A revelada
            val snapshot = activeBatchSnapshot(order = listOf("A", "B"))
            val practice = FakePracticeRepository(snapshot)
            practice.transact(german) { PracticeChange(next = snapshot, result = Unit) }
            val settings = FakeSettingsRepository()
            val viewModel = sessionViewModelWith(practice, catalogs(), settingsRepository = settings)
            advanceUntilIdle()
            viewModel.onAction(PracticeSessionAction.Reveal)
            viewModel.practicing().current.target shouldBe "Bom dia"
            viewModel.practicing().current.source shouldBe "Buenos días"

            // When
            settings.setActiveTargetLocale(german)
            viewModel.onAction(PracticeSessionAction.Resume)
            advanceUntilIdle()

            // Then
            val after = viewModel.practicing()
            after.current.id shouldBe PhraseId("A")
            after.current.target shouldBe "Guten Morgen"
            after.revealed shouldBe false
            after.activeTargetLocale shouldBe german
        }
}
