package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Ciclo completo entre cierre y composición (spec practice-persistence, "Composición única tras vaciar la
// tanda activa"; ADR-0017). Persistencia en fake; la persistencia real en Room va en :core:data.
class CloseThenObtainTest {

    private val catalog = CatalogLoadResult.Loaded(
        listOf("A", "B", "C").map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
    )

    private fun obtain(practice: FakePracticeRepository, catalogResult: CatalogLoadResult = catalog) =
        ObtainActiveBatch(
            composeNextBatch = ComposeNextBatch(),
            catalogRepository = FakeCatalogRepository(catalogResult),
            settingsRepository = FakeSettingsRepository(UserSettings.DEFAULT),
            practiceRepository = practice,
        )

    private suspend fun close(practice: FakePracticeRepository) {
        practice.transact { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }
    }

    // Certificada con repaso muy en el futuro: no vuelve a entrar en la tanda siguiente
    private fun certified(id: String) =
        aPhraseProgress(id = id, level = Level(1), streak = Streak(3), dueAtSession = 100)

    // Scenario: Cierre sin composición posible deja la tanda sin activar (spec: practice-persistence)
    @Test
    fun `given a closed batch and no new phrases left when obtaining then returns NothingAvailable`() = runTest {
        // Given: tanda con A y B certificadas y el catálogo solo tiene esas dos frases
        val onlyAB = CatalogLoadResult.Loaded(listOf("A", "B").map { aCatalogPhrase(id = it, level = CefrLevel.A1) })
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(certified("A"), certified("B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 0,
            ),
        )
        close(practice)

        // When
        val result = obtain(practice, onlyAB).invoke()

        // Then
        result shouldBe ActiveBatchResult.NothingAvailable
        practice.read().activeBatch shouldBe null
    }

    // Scenario: Obtener dos veces tras el cierre compone una sola vez (spec: practice-persistence)
    @Test
    fun `given a closed batch when obtaining twice then the next batch is composed once`() = runTest {
        // Given: tanda con A y B certificadas; C es nueva y está en el catálogo
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(certified("A"), certified("B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 0,
            ),
        )
        close(practice)

        // When
        val first = obtain(practice).invoke()
        val stateAfterFirst = practice.read()
        val second = obtain(practice).invoke()

        // Then
        first.shouldBeInstanceOf<ActiveBatchResult.Created>()
        first.batch.entries.map { it.id.value } shouldContainExactly listOf("C")
        second.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        second.batch.entries.map { it.id.value } shouldContainExactly listOf("C")
        practice.read() shouldBe stateAfterFirst
    }
}
