package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Generación de sesión en un único paso (spec: practice-batch, "Generar sesión avanza el contador y compone";
// ADR-0017).
class GenerateSessionTest {

    private fun generateSession(practice: FakePracticeRepository, catalogIds: List<String>) = GenerateSession(
        practiceRepository = practice,
        composeNextBatch = ComposeNextBatch(),
        catalogRepository = FakeCatalogRepository(
            CatalogLoadResult.Loaded(catalogIds.map { aCatalogPhrase(id = it) }),
        ),
        settingsRepository = FakeSettingsRepository(UserSettings.DEFAULT),
    )

    // Scenario: Generar sesión incrementa el contador y compone en un paso (spec: practice-batch)
    @Test
    fun `given the counter at 9 and a phrase due at session 10 when generating then the counter becomes 10`() =
        runTest {
            // Given: tanda completa de A, contador de sesiones en 9, y P pendiente a partir de la sesión 10
            val progress = listOf(
                aPhraseProgress(id = "A", answeredInBatch = true),
                aPhraseProgress(id = "P", level = Level(1), dueAtSession = 10),
            )
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = progress,
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(20)),
                    sessionCount = 9,
                ),
            )

            // When
            val result = generateSession(practice, catalogIds = listOf("A", "P")).invoke()

            // Then: el contador pasa a 10, disponible sin otra lectura adicional
            practice.read().sessionCount shouldBe 10

            // Then: la tanda siguiente ya está compuesta con la sesión 10 como actual, e incluye a P
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value } shouldContain "P"
            val order = practice.read().activeBatch?.order?.map { it.value }
            order.shouldNotBeNull() shouldContain "P"
        }

    // Scenario: Generar sesión reinicia el desglose para la tanda siguiente (spec: practice-session)
    @Test
    fun `given a completed batch outcome when generating session then the batch outcome resets to zero`() =
        runTest {
            // Given: tanda completa de A con un desglose de 2 aciertos y 1 fallo acumulado
            val progress = listOf(aPhraseProgress(id = "A", answeredInBatch = true))
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = progress,
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(20)),
                    sessionCount = 9,
                    batchOutcome = BatchOutcome(hits = 2, misses = 1, levelUps = 0),
                ),
            )

            // When
            generateSession(practice, catalogIds = listOf("A")).invoke()

            // Then: el desglose de la tanda anterior no se arrastra a la siguiente
            practice.read().batchOutcome shouldBe BatchOutcome(hits = 0, misses = 0, levelUps = 0)
        }
}
