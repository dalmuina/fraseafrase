package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

// Generación de sesión en un único paso (spec: practice-batch, "Generación de sesión"; design D3).
class GenerateSessionTest {

    private val today: LocalDate = LocalDate.of(2026, 3, 10)

    private fun generateSession(practice: FakePracticeRepository, catalogIds: List<String>) = GenerateSession(
        practiceRepository = practice,
        resolveBatchClosure = ResolveBatchClosure(FixedClock(today)),
        composeNextBatch = ComposeNextBatch(FixedClock(today)),
        catalogRepository = FakeCatalogRepository(
            CatalogLoadResult.Loaded(catalogIds.map { aCatalogPhrase(id = it) }),
        ),
        settingsRepository = FakeSettingsRepository(UserSettings.DEFAULT),
    )

    // Certificada: racha 3 respondida en la tanda. Sin certificar: nivel 0, nunca respondida.
    private fun certified(id: String) = aPhraseProgress(
        id = id,
        level = Level(1),
        streak = Streak(3),
        answeredInBatch = true,
        nextReview = today.plusDays(1),
    )

    private fun uncertified(id: String) = aPhraseProgress(id = id)

    private fun stateWith(
        progress: List<PhraseProgress>,
        carry: List<String> = emptyList(),
    ) = PracticeSnapshot(
        progress = progress,
        activeBatch = ActiveBatch(order = progress.map { it.id }, capacity = BatchSize(20)),
        carry = CarryQueue(carry.map { PhraseId(it) }),
    )

    // Scenario: Generar sesión resuelve y compone en un paso (spec: practice-batch)
    @Test
    fun `given certified and uncertified phrases when generating a session then it resolves and composes`() =
        runTest {
            // Given: tanda de A, B (certificadas) y C, D, E (no certificadas), sin cola sobrante
            val practice = FakePracticeRepository(
                stateWith(
                    listOf(certified("A"), certified("B"), uncertified("C"), uncertified("D"), uncertified("E")),
                ),
            )

            // When
            val result = generateSession(practice, catalogIds = listOf("A", "B", "C", "D", "E")).invoke()

            // Then: las certificadas suben de nivel
            val state = practice.read()
            state.progress.first { it.id.value == "A" }.level shouldBe Level(2)
            state.progress.first { it.id.value == "B" }.level shouldBe Level(2)

            // Then: las no certificadas quedan en la cola, en su orden de tanda, y ya forman la tanda siguiente
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value } shouldBe listOf("C", "D", "E")

            // Then: la tanda siguiente ya está disponible sin otra lectura adicional
            state.activeBatch?.order?.map { it.value } shouldBe listOf("C", "D", "E")
        }

    // Scenario: La cola sobrante va antes que las arrastradas de este cierre (spec: practice-batch)
    @Test
    fun `given a leftover queue when generating a session then the leftover goes before the closure's phrases`() =
        runTest {
            // Given: cola sobrante con Z y una tanda en el orden B, C, A, donde A y C no están certificadas y B sí
            val practice = FakePracticeRepository(
                PracticeSnapshot(
                    progress = listOf(certified("B"), uncertified("C"), uncertified("A"), uncertified("Z")),
                    activeBatch = ActiveBatch(
                        order = listOf(PhraseId("B"), PhraseId("C"), PhraseId("A")),
                        capacity = BatchSize(20),
                    ),
                    carry = CarryQueue(listOf(PhraseId("Z"))),
                ),
            )

            // When
            val result = generateSession(practice, catalogIds = listOf("Z", "B", "C", "A")).invoke()

            // Then: la tanda siguiente es Z, C, A
            val created = result.shouldBeInstanceOf<ActiveBatchResult.Created>()
            created.batch.entries.map { it.id.value } shouldBe listOf("Z", "C", "A")
        }
}
