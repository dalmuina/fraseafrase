package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainCatalogPhrasesForActiveLanguage
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatchResult
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeDeviceLocaleProvider
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Escenarios "Vaciar la tanda activa no compone la tanda siguiente por sí solo" y "Obtener dos veces tras vaciar
// compone una sola vez" de practice-persistence, con persistencia real en Room (ADR-0017).
@OptIn(ExperimentalCoroutinesApi::class)
class RoomPracticeCycleTest {

    private lateinit var database: PracticeDatabase
    private lateinit var source: RoomPracticeDataSource
    private val ptPt = LanguageTag("pt-PT")

    private val catalog = CatalogLoadResult.Loaded(
        listOf("A", "B", "C", "Z").map { aCatalogPhrase(id = it, level = CefrLevel.A1) },
    )

    @BeforeEach
    fun openDatabase() {
        database = inMemoryPracticeDatabase()
        source = RoomPracticeDataSource(database, UnconfinedTestDispatcher())
    }

    @AfterEach
    fun closeDatabase() {
        database.close()
    }

    private suspend fun obtain() = ObtainActiveBatch(
        composeNextBatch = ComposeNextBatch(),
        obtainCatalogPhrases = ObtainCatalogPhrasesForActiveLanguage(
            deviceLocaleProvider = FakeDeviceLocaleProvider(ptPt),
            catalogRepository = FakeCatalogRepository(catalog, targetLocale = ptPt),
        ),
        settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(10))),
        practiceRepository = source,
    ).invoke(ptPt)

    // Scenario: Vaciar la tanda activa no compone la tanda siguiente por sí solo
    @Test
    fun `given an active batch when emptying it without composing then no active batch or new phrases appear`() =
        runTest {
            // Given: tanda activa con A y B, y una sesión ya generada
            val initial = PracticeSnapshot(
                progress = listOf(aPhraseProgress(id = "A", level = Level(1)), aPhraseProgress(id = "B")),
                activeBatch = ActiveBatch(order = listOf(PhraseId("A"), PhraseId("B")), capacity = BatchSize(10)),
                sessionCount = 5,
            )
            source.transact(ptPt) { PracticeChange(initial, Unit) }

            // When: se vacía la tanda activa sin componer
            source.transact(ptPt) { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }

            // Then
            val emptied = source.read(ptPt)
            emptied.activeBatch.shouldBeNull()
            emptied.progress.map { it.id.value } shouldContainExactly listOf("A", "B")
        }

    // Scenario: Obtener dos veces tras vaciar compone una sola vez
    @Test
    fun `given an emptied active batch when obtaining it twice then it composes once`() = runTest {
        // Given: tanda vaciada, con progreso ya vencido para A y B, y composición posible a partir del catálogo
        val initial = PracticeSnapshot(
            progress = listOf(aPhraseProgress(id = "A", level = Level(1)), aPhraseProgress(id = "B")),
            activeBatch = null,
            sessionCount = 5,
        )
        source.transact(ptPt) { PracticeChange(initial, Unit) }

        // When: se obtiene la tanda activa dos veces seguidas
        val first = obtain()
        val stateAfterFirst = source.read(ptPt)
        val second = obtain()

        // Then
        first.shouldBeInstanceOf<ActiveBatchResult.Created>()
        first.batch.entries.map { it.id.value } shouldContainExactly listOf("A", "B", "C", "Z")
        second.shouldBeInstanceOf<ActiveBatchResult.Existing>()
        second.batch.entries.map { it.id.value } shouldContainExactly listOf("A", "B", "C", "Z")
        source.read(ptPt) shouldBe stateAfterFirst
    }

    // Scenario: El primer uso de un idioma destino no crea tanda para otro (spec: practice-persistence)
    @Test
    fun `given pt-PT with an active batch when obtaining pt-BR then pt-PT is not modified`() = runTest {
        // Given: pt-PT ya tiene tanda activa compuesta
        obtain()
        val ptPtBefore = source.read(ptPt)
        val ptBr = LanguageTag("pt-BR")
        val source2 = RoomPracticeDataSource(database, UnconfinedTestDispatcher())
        val obtainPtBr = ObtainActiveBatch(
            composeNextBatch = ComposeNextBatch(),
            obtainCatalogPhrases = ObtainCatalogPhrasesForActiveLanguage(
                deviceLocaleProvider = FakeDeviceLocaleProvider(ptBr),
                catalogRepository = FakeCatalogRepository(catalog, targetLocale = ptBr),
            ),
            settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(10))),
            practiceRepository = source2,
        )

        // When: se obtiene la tanda activa de pt-BR por primera vez
        val ptBrResult = obtainPtBr.invoke(ptBr)

        // Then
        ptBrResult.shouldBeInstanceOf<ActiveBatchResult.Created>()
        source.read(ptPt) shouldBe ptPtBefore
    }
}
