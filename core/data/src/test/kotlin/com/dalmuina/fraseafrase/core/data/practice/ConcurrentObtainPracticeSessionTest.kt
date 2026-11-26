package com.dalmuina.fraseafrase.core.data.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSessionResult
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.practice.RegenerateActiveBatchForLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import com.dalmuina.fraseafrase.core.testing.anObtainCatalogPhrases
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

// Escenario "Dos obtenciones simultáneas sin tanda activa componen una sola tanda" de practice-persistence, con
// persistencia real en Room.
@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrentObtainPracticeSessionTest {

    private lateinit var database: PracticeDatabase
    private lateinit var source: RoomPracticeDataSource
    private val ptPt = LanguageTag("pt-PT")

    private val catalog = CatalogLoadResult.Loaded(
        (1..30).map { aCatalogPhrase(id = "a1-%02d".format(it), level = CefrLevel.A1) },
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

    private fun obtainPracticeSession(repository: PracticeRepository): ObtainPracticeSession {
        val obtainCatalogPhrases = anObtainCatalogPhrases(FakeCatalogRepository(catalog, targetLocale = ptPt))
        return ObtainPracticeSession(
            obtainActiveBatch = ObtainActiveBatch(
                settingsRepository = FakeSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(10))),
                practiceRepository = repository,
                regenerateActiveBatchForLevel = RegenerateActiveBatchForLevel(
                    practiceRepository = repository,
                    composeNextBatch = ComposeNextBatch(),
                    obtainCatalogPhrases = obtainCatalogPhrases,
                ),
            ),
            obtainCatalogPhrases = obtainCatalogPhrases,
            practiceRepository = repository,
        )
    }

    // Scenario: Dos obtenciones simultáneas sin tanda activa componen una sola tanda
    @Test
    fun `given no active batch when two obtentions read before either writes then one batch is composed`() =
        runTest {
            // Given: sin tanda activa y 4 frases A1 de nivel 1 con dueAtSession 5
            val previousIds = (1..4).map { "a1-%02d".format(it) }
            val initial = PracticeSnapshot(
                progress = previousIds.map { aPhraseProgress(id = it, level = Level(1), dueAtSession = 5) },
                activeBatch = null,
                sessionCount = 0,
            )
            source.transact(ptPt) { PracticeChange(initial, Unit) }
            val obtain = obtainPracticeSession(BarrierPracticeRepository(source, parties = 2))

            // When
            val first = async { obtain(ptPt) }
            val second = async { obtain(ptPt) }
            val firstItems = first.await().shouldBeInstanceOf<PracticeSessionResult.Ready>().items.map { it.id }
            val secondItems = second.await().shouldBeInstanceOf<PracticeSessionResult.Ready>().items.map { it.id }

            // Then
            val stored = source.read(ptPt)
            val progressIds = stored.progress.map { it.id }
            assertSoftly {
                firstItems shouldHaveSize 10
                secondItems shouldBe firstItems
                stored.activeBatch?.order shouldBe firstItems
                progressIds shouldHaveSize 10
                firstItems shouldContainAll initial.progress.map { it.id }
                stored.sessionCount shouldBe 0
            }
        }

    /** Holds every `read()` back until [parties] readers have taken their snapshot. */
    private class BarrierPracticeRepository(
        private val delegate: PracticeRepository,
        private val parties: Int,
    ) : PracticeRepository by delegate {

        private val arrived = AtomicInteger()
        private val allRead = CompletableDeferred<Unit>()

        override suspend fun read(targetLocale: LanguageTag): PracticeSnapshot {
            val snapshot = delegate.read(targetLocale)
            if (arrived.incrementAndGet() == parties) allRead.complete(Unit)
            allRead.await()
            return snapshot
        }
    }
}
