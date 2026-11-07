package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.FakeSettingsRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObtainPracticeSessionTest {

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settings: UserSettings = UserSettings.DEFAULT,
    ) = ObtainPracticeSession(
        obtainActiveBatch = ObtainActiveBatch(
            composeNextBatch = ComposeNextBatch(),
            catalogRepository = FakeCatalogRepository(catalog),
            settingsRepository = FakeSettingsRepository(settings),
            practiceRepository = practice,
        ),
        catalogRepository = FakeCatalogRepository(catalog),
    )

    private fun activeBatch(
        order: List<String>,
        progress: List<PhraseProgress>,
        sessionCount: Int = 0,
    ) = PracticeSnapshot(
        progress = progress,
        activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
        sessionCount = sessionCount,
    )

    private fun loaded(vararg phrases: CatalogPhrase) = CatalogLoadResult.Loaded(phrases.toList())

    private suspend fun clearActiveBatch(practice: FakePracticeRepository) {
        practice.transact { snapshot -> PracticeChange(snapshot.copy(activeBatch = null), Unit) }
    }

    // Scenario: Al abrir la práctica se muestra la primera frase en español (spec: practice-session)
    @Test
    fun `given an active batch when opening the session then the first phrase has its source and target texts`() =
        runTest {
            // Given: tanda activa A, B y C en ese orden; A con "Buenos días" y "Bom dia"
            val practice = FakePracticeRepository(
                activeBatch(
                    order = listOf("A", "B", "C"),
                    progress = listOf("A", "B", "C").map { aPhraseProgress(id = it) },
                ),
            )
            val catalog = loaded(
                aCatalogPhrase(id = "A", source = "Buenos días", target = "Bom dia"),
                aCatalogPhrase(id = "B"),
                aCatalogPhrase(id = "C"),
            )

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
            ready.items.map { it.id.value } shouldBe listOf("A", "B", "C")
            ready.items.first().source shouldBe "Buenos días"
            ready.items.first().target shouldBe "Bom dia"
        }

    // Scenario: Nivel CEFR visible sin revelar (spec: practice-session)
    @Test
    fun `given a catalog phrase with a cefr level when opening the session then the item carries it`() =
        runTest {
            // Given: tanda activa con una frase A de nivel B1 en el catálogo
            val practice = FakePracticeRepository(
                activeBatch(order = listOf("A"), progress = listOf(aPhraseProgress(id = "A"))),
            )
            val catalog = loaded(aCatalogPhrase(id = "A", level = CefrLevel.B1))

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
            ready.items.single().cefrLevel shouldBe CefrLevel.B1
        }

    // Scenario: Insignia visible sin revelar (spec: practice-session)
    @Test
    fun `given a phrase progress with level 3 when opening the session then the item carries that level`() =
        runTest {
            // Given: tanda activa con una frase A cuyo progreso tiene nivel 3
            val practice = FakePracticeRepository(
                activeBatch(
                    order = listOf("A"),
                    progress = listOf(aPhraseProgress(id = "A", level = Level(3), dueAtSession = 5)),
                ),
            )
            val catalog = loaded(aCatalogPhrase(id = "A"))

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
            ready.items.single().level shouldBe Level(3)
        }

    // Scenario: Reproducción del audio en el locale de la frase (spec: phrase-audio)
    @Test
    fun `given a catalog phrase with a target locale when read then the practice item carries that locale`() =
        runTest {
            // Given: tanda activa con una frase A cuyo locale de destino es "pt-PT"
            val practice = FakePracticeRepository(
                activeBatch(order = listOf("A"), progress = listOf(aPhraseProgress(id = "A"))),
            )
            val catalog = loaded(aCatalogPhrase(id = "A"))

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
            ready.items.single().targetLocale shouldBe LanguageTag("pt-PT")
        }

    // Scenario: Reabrir devuelve la misma tanda (spec: practice-session, Reanudar la tanda activa)
    @Test
    fun `given a saved batch in another order when opening the session then keeps that order and streaks`() = runTest {
        // Given: tanda activa en el orden B, C, A y la racha de C igual a 2
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("B", "C", "A"),
                progress = listOf(
                    aPhraseProgress(id = "A"),
                    aPhraseProgress(id = "B"),
                    aPhraseProgress(id = "C", streak = Streak(2), answeredInBatch = true),
                ),
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"), aCatalogPhrase(id = "C"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
        ready.items.map { it.id.value } shouldBe listOf("B", "C", "A")
        ready.items[1].streak shouldBe Streak(2)
    }

    // Scenario: Primer uso muestra la primera frase del catálogo (spec: practice-session)
    @Test
    fun `given an empty database when opening the session then the first catalog phrase is shown with streak 0`() =
        runTest {
            // Given: base de datos vacía, quince frases A1, nivel A1, N igual a 20
            val practice = FakePracticeRepository()
            val catalog = loaded(*(1..15).map { aCatalogPhrase(id = "a1-%02d".format(it)) }.toTypedArray())

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
            ready.items.first().id.value shouldBe "a1-01"
            ready.items.first().streak shouldBe Streak(0)
        }

    // Scenario: Tanda activa se muestra sin cambios, al margen de otras frases pendientes (spec: practice-session)
    @Test
    fun `given an active batch when opening the session then the same phrases are shown regardless of other phrases`() =
        runTest {
            // Given: tanda activa de cinco frases, y P con dueAtSession futuro fuera de ella
            val batchPhrases = listOf("A", "B", "C", "D", "E")
            val practice = FakePracticeRepository(
                activeBatch(
                    order = batchPhrases,
                    progress = batchPhrases.map { aPhraseProgress(id = it) } +
                        aPhraseProgress(id = "P", level = Level(1), dueAtSession = 5),
                ),
            )
            val catalog = loaded(*(batchPhrases + "P").map { aCatalogPhrase(id = it) }.toTypedArray())

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
            ready.items.map { it.id.value } shouldBe batchPhrases
        }

    // Scenario: Catálogo agotado tras cerrar (spec: practice-session)
    @Test
    fun `given a closed batch and an exhausted catalog when opening the session then nothing to practice`() = runTest {
        // Given: tanda de dos frases certificadas, con repaso muy futuro, y catálogo formado solo por ellas
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A", "B"),
                progress = listOf("A", "B").map {
                    aPhraseProgress(id = it, level = Level(1), streak = Streak(3), dueAtSession = 100)
                },
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"))
        clearActiveBatch(practice)

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result shouldBe PracticeSessionResult.NothingToPractice
    }

    // Scenario: Frases con repaso futuro sin tanda (spec: practice-session)
    @Test
    fun `given only future reviews and no active batch when opening the session then nothing to practice`() = runTest {
        // Given: catálogo cuya única frase P tiene nivel 1 con dueAtSession futuro y sin tanda activa
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(aPhraseProgress(id = "P", level = Level(1), dueAtSession = 5)),
                activeBatch = null,
                sessionCount = 0,
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "P"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result shouldBe PracticeSessionResult.NothingToPractice
    }

    // Scenario: Tanda completa tras responder a todas sus frases (spec: practice-batch, Tanda completa)
    @Test
    fun `given three phrases when all of them have been answered then the batch is complete`() = runTest {
        // Given: tanda de tres frases A, B y C, todas ya respondidas
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A", "B", "C"),
                progress = listOf("A", "B", "C").map { aPhraseProgress(id = it, answeredInBatch = true) },
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"), aCatalogPhrase(id = "C"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result.shouldBeInstanceOf<PracticeSessionResult.Ready>().isComplete shouldBe true
    }

    // Scenario: Tanda incompleta con una frase sin responder (spec: practice-batch, Tanda completa)
    @Test
    fun `given three phrases when one of them has not been answered then the batch is not complete`() = runTest {
        // Given: tanda de tres frases A, B y C, donde A y B ya fueron respondidas y C no
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A", "B", "C"),
                progress = listOf(
                    aPhraseProgress(id = "A", answeredInBatch = true),
                    aPhraseProgress(id = "B", answeredInBatch = true),
                    aPhraseProgress(id = "C"),
                ),
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"), aCatalogPhrase(id = "C"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result.shouldBeInstanceOf<PracticeSessionResult.Ready>().isComplete shouldBe false
    }

    // Scenario: Asset de catálogo ilegible (spec: practice-session, Error de catálogo)
    @Test
    fun `given an unreadable catalog asset when opening the session then returns a catalog failure`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)).invoke()

        // Then
        result shouldBe PracticeSessionResult.CatalogFailed(CatalogLoadError.AssetUnreadable)
    }

    // Scenario: Documento de catálogo malformado (spec: practice-session, Error de catálogo)
    @Test
    fun `given a malformed catalog document when opening the session then returns a catalog failure`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        val result = useCase(practice, CatalogLoadResult.Failed(CatalogLoadError.MalformedDocument)).invoke()

        // Then
        result shouldBe PracticeSessionResult.CatalogFailed(CatalogLoadError.MalformedDocument)
    }

    // Scenario: Tanda activa sin frases (design.md D1; el escenario no está en la spec)
    @Test
    fun `given an active batch with no entries when opening the session then nothing to practice`() = runTest {
        // Given: tanda activa sin frases
        val practice = FakePracticeRepository(activeBatch(order = emptyList(), progress = emptyList()))
        val catalog = loaded(aCatalogPhrase(id = "A"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result shouldBe PracticeSessionResult.NothingToPractice
    }

    // Scenario: Frase de la tanda ausente del catálogo (design.md D1; el escenario no está en la spec)
    @Test
    fun `given a batch phrase missing from the catalog when opening the session then fails with its id`() =
        runTest {
            // Given: tanda con las frases A y ghost-phrase, pero el catálogo solo contiene A
            val ids = listOf("A", "ghost-phrase")
            val practice = FakePracticeRepository(
                activeBatch(order = ids, progress = ids.map { aPhraseProgress(id = it) }),
            )
            val catalog = loaded(aCatalogPhrase(id = "A"))

            // When
            val error = shouldThrow<IllegalArgumentException> { useCase(practice, catalog).invoke() }

            // Then
            error.message shouldContain "ghost-phrase"
        }
}
