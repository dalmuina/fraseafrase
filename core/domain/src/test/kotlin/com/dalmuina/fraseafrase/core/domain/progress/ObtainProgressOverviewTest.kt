package com.dalmuina.fraseafrase.core.domain.progress

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObtainProgressOverviewTest {

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settings: UserSettings = UserSettings.DEFAULT,
    ) = ObtainProgressOverview(
        catalogRepository = FakeCatalogRepository(catalog),
        practiceRepository = practice,
        obtainUserSettings = ObtainUserSettings(FakeSettingsRepository(settings)),
    )

    private fun activeBatch(order: List<String>, progress: List<PhraseProgress>) = PracticeSnapshot(
        progress = progress,
        activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
        sessionCount = 0,
    )

    private fun loaded(vararg phrases: CatalogPhrase) = CatalogLoadResult.Loaded(phrases.toList())

    // Scenario: Catálogo con frases en varios niveles (spec: progress-overview)
    @Test
    fun `given phrases at several levels when opening progress then byLevel counts each level`() = runTest {
        // Given: catálogo de diez frases donde tres tienen progreso de nivel 2, una de nivel 5 y las
        // seis restantes no tienen progreso
        val levelTwoIds = listOf("A", "B", "C")
        val levelFiveId = "D"
        val withoutProgressIds = listOf("E", "F", "G", "H", "I", "J")
        val progress = levelTwoIds.map {
            aPhraseProgress(id = it, level = Level(2), dueAtSession = 5)
        } + aPhraseProgress(id = levelFiveId, level = Level(5), dueAtSession = 5)
        val practice = FakePracticeRepository(
            PracticeSnapshot(progress = progress, activeBatch = null, sessionCount = 0),
        )
        val catalog = loaded(
            *(levelTwoIds + levelFiveId + withoutProgressIds)
                .map { aCatalogPhrase(id = it, level = CefrLevel.A1) }.toTypedArray(),
        )

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.byLevel shouldContainExactly mapOf(
            Level(0) to 6,
            Level(1) to 0,
            Level(2) to 3,
            Level(3) to 0,
            Level(4) to 0,
            Level(5) to 1,
        )
    }

    // Scenario: Ninguna frase con progreso (spec: progress-overview)
    @Test
    fun `given a catalog without any progress when opening progress then byLevel counts everything at level zero`() =
        runTest {
            // Given: catálogo de ocho frases sin ningún progreso registrado
            val practice = FakePracticeRepository()
            val catalog = loaded(
                *(1..8).map { aCatalogPhrase(id = "phrase-$it", level = CefrLevel.A1) }.toTypedArray(),
            )

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
            available.byLevel shouldContainExactly mapOf(
                Level(0) to 8,
                Level(1) to 0,
                Level(2) to 0,
                Level(3) to 0,
                Level(4) to 0,
                Level(5) to 0,
            )
        }

    // Scenario: Frase de la tanda activa cuenta en nivel 0 (spec: progress-overview)
    @Test
    fun `given a batch phrase at level zero when opening progress then it counts in level zero`() = runTest {
        // Given: catálogo de cinco frases donde una está en la tanda activa con nivel 0 y racha 2, y
        // las otras cuatro no tienen progreso
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A"),
                progress = listOf(
                    aPhraseProgress(id = "A", level = Level(0), streak = Streak(2), answeredInBatch = true),
                ),
            ),
        )
        val catalog = loaded(
            *listOf("A", "B", "C", "D", "E").map { aCatalogPhrase(id = it, level = CefrLevel.A1) }.toTypedArray(),
        )

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.byLevel[Level(0)] shouldBe 5
    }

    // Scenario: Sin tanda activa muestra el total disponible, el nivel CEFR y el total por nivel
    // (spec: progress-overview)
    @Test
    fun `given no active batch when opening progress then it shows the total available and the cefr level`() =
        runTest {
            // Given: no hay tanda activa, un catálogo de seis frases sin progreso y el nivel CEFR B1 guardado
            val practice = FakePracticeRepository()
            val catalog = loaded(
                *(1..6).map { aCatalogPhrase(id = "phrase-$it", level = CefrLevel.B1) }.toTypedArray(),
            )
            val settings = UserSettings.DEFAULT.copy(cefrLevel = CefrLevel.B1)

            // When
            val result = useCase(practice, catalog, settings).invoke()

            // Then
            val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
            available.totalAvailable shouldBe 6
            available.cefrLevel shouldBe CefrLevel.B1
            available.byLevel[Level(0)] shouldBe 6
        }

    // Scenario: Catálogo ilegible al abrir el progreso (spec: progress-overview)
    @Test
    fun `given an unreadable catalog when opening progress then it returns catalog unavailable`() = runTest {
        // Given: un catálogo que no se puede leer
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)).invoke()

        // Then
        result shouldBe ProgressOverviewResult.CatalogUnavailable(CatalogLoadError.AssetUnreadable)
    }

    // Scenario: Las frases de otros niveles CEFR no se cuentan (spec: progress-overview)
    @Test
    fun `given phrases of another cefr level when opening progress then byLevel does not count them`() = runTest {
        // Given: el nivel CEFR B1 vigente y seis frases de nivel B1 sin progreso, más cuatro de nivel C1
        val b1Ids = (1..6).map { "b1-$it" }
        val c1Ids = (1..4).map { "c1-$it" }
        val practice = FakePracticeRepository()
        val catalog = loaded(
            *(
                b1Ids.map { aCatalogPhrase(id = it, level = CefrLevel.B1) } +
                    c1Ids.map { aCatalogPhrase(id = it, level = CefrLevel.C1) }
                ).toTypedArray(),
        )
        val settings = UserSettings.DEFAULT.copy(cefrLevel = CefrLevel.B1)

        // When
        val result = useCase(practice, catalog, settings).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.byLevel[Level(0)] shouldBe 6
    }

    // Scenario: El total no incluye frases de otros niveles CEFR (spec: progress-overview)
    @Test
    fun `given phrases of another cefr level when opening progress then the total excludes them`() = runTest {
        // Given: el nivel CEFR B1 guardado y seis frases de nivel B1 sin progreso, más cuatro de nivel C1
        val b1Ids = (1..6).map { "b1-$it" }
        val c1Ids = (1..4).map { "c1-$it" }
        val practice = FakePracticeRepository()
        val catalog = loaded(
            *(
                b1Ids.map { aCatalogPhrase(id = it, level = CefrLevel.B1) } +
                    c1Ids.map { aCatalogPhrase(id = it, level = CefrLevel.C1) }
                ).toTypedArray(),
        )
        val settings = UserSettings.DEFAULT.copy(cefrLevel = CefrLevel.B1)

        // When
        val result = useCase(practice, catalog, settings).invoke()

        // Then
        val available = result.shouldBeInstanceOf<ProgressOverviewResult.Available>()
        available.byLevel.values.sum() shouldBe 6
    }
}
