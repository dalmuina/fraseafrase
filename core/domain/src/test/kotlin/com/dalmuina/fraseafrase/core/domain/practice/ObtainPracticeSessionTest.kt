package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ObtainPracticeSessionTest {

    private val today = LocalDate.of(2026, 3, 10)

    private fun useCase(
        practice: FakePracticeRepository,
        catalog: CatalogLoadResult,
        settings: UserSettings = UserSettings.DEFAULT,
        clock: FixedClock = FixedClock(today),
    ) = ObtainPracticeSession(
        obtainActiveBatch = ObtainActiveBatch(
            composeNextBatch = ComposeNextBatch(clock),
            catalogRepository = FakeCatalogRepository(catalog),
            settingsRepository = FakeSettingsRepository(settings),
            practiceRepository = practice,
        ),
        catalogRepository = FakeCatalogRepository(catalog),
    )

    private fun activeBatch(
        order: List<String>,
        progress: List<PhraseProgress>,
    ) = PracticeSnapshot(
        progress = progress,
        activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
        carry = CarryQueue(emptyList()),
    )

    private fun loaded(vararg phrases: CatalogPhrase) = CatalogLoadResult.Loaded(phrases.toList())

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
                    aPhraseProgress(id = "C", streak = 2, answeredInBatch = true),
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

    // Scenario: Cambio de día con tanda activa no altera la tanda (spec: practice-session)
    @Test
    fun `given an active batch and a new day when opening the session then the same phrases are shown`() = runTest {
        // Given: tanda activa de cinco frases compuesta el 10-03 y P con repaso el 11-03 fuera de ella
        val batchPhrases = listOf("A", "B", "C", "D", "E")
        val practice = FakePracticeRepository(
            activeBatch(
                order = batchPhrases,
                progress = batchPhrases.map { aPhraseProgress(id = it) } +
                    aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 11)),
            ),
        )
        val catalog = loaded(*(batchPhrases + "P").map { aCatalogPhrase(id = it) }.toTypedArray())

        // When
        val result = useCase(practice, catalog, clock = FixedClock(LocalDate.of(2026, 3, 11))).invoke()

        // Then
        val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
        ready.items.map { it.id.value } shouldBe batchPhrases
    }

    // Scenario: Catálogo agotado tras cerrar (spec: practice-session)
    @Test
    fun `given a closed batch and an exhausted catalog when opening the session then nothing to practice`() = runTest {
        // Given: tanda de dos frases certificadas, con repaso futuro, y catálogo formado solo por ellas
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A", "B"),
                progress = listOf("A", "B").map {
                    aPhraseProgress(
                        id = it,
                        level = 1,
                        streak = 3,
                        answeredInBatch = true,
                        nextReview = today.plusDays(1),
                    )
                },
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"))
        CloseActiveBatch(practice, ResolveBatchClosure(FixedClock(today))).invoke(expectedUncertified = 0)

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result shouldBe PracticeSessionResult.NothingToPractice
    }

    // Scenario: Frases con repaso futuro sin tanda (spec: practice-session)
    @Test
    fun `given only future reviews and no active batch when opening the session then nothing to practice`() = runTest {
        // Given: catálogo cuya única frase P tiene nivel 1 con repaso el 11-03, y hoy es 10-03
        val practice = FakePracticeRepository(
            PracticeSnapshot(
                progress = listOf(aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 11))),
                activeBatch = null,
                carry = CarryQueue(emptyList()),
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "P"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result shouldBe PracticeSessionResult.NothingToPractice
    }

    private val tomorrow = today.plusDays(1)

    /** Frase certificada en la tanda: racha 3 respondida, con nivel y repaso futuros. */
    private fun certified(id: String) = aPhraseProgress(
        id = id,
        level = 1,
        streak = 3,
        answeredInBatch = true,
        nextReview = tomorrow,
    )

    // Scenario: Aviso con frases no certificadas (spec: practice-session, Aviso de cierre antes de confirmar)
    @Test
    fun `given five phrases with two certified when opening the session then the warning counts three uncertified`() =
        runTest {
            // Given: tanda de cinco frases, dos con racha 3 y tres sin certificar
            val batch = listOf(certified("A"), certified("B")) + listOf("C", "D", "E").map { aPhraseProgress(id = it) }
            val practice = FakePracticeRepository(
                activeBatch(order = listOf("A", "B", "C", "D", "E"), progress = batch),
            )
            val catalog = loaded(*listOf("A", "B", "C", "D", "E").map { aCatalogPhrase(id = it) }.toTypedArray())

            // When
            val result = useCase(practice, catalog).invoke()

            // Then
            result.shouldBeInstanceOf<PracticeSessionResult.Ready>().uncertifiedCount shouldBe 3
        }

    // Scenario: Aviso con una sola frase no certificada (spec: practice-session, Aviso de cierre antes de confirmar)
    @Test
    fun `given two phrases with one uncertified when opening the session then the warning counts one`() = runTest {
        // Given: tanda de dos frases, una con racha 3 y otra sin certificar
        val practice = FakePracticeRepository(
            activeBatch(order = listOf("A", "B"), progress = listOf(certified("A"), aPhraseProgress(id = "B"))),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result.shouldBeInstanceOf<PracticeSessionResult.Ready>().uncertifiedCount shouldBe 1
    }

    // Scenario: Aviso sin frases no certificadas (spec: practice-session, Aviso de cierre antes de confirmar)
    @Test
    fun `given two certified phrases when opening the session then the warning counts zero`() = runTest {
        // Given: tanda de dos frases, ambas con racha 3
        val practice = FakePracticeRepository(
            activeBatch(order = listOf("A", "B"), progress = listOf(certified("A"), certified("B"))),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result.shouldBeInstanceOf<PracticeSessionResult.Ready>().uncertifiedCount shouldBe 0
    }

    // Scenario: Frase con racha 3 marcada como sabida (spec: practice-session, Racha y frase sabida visibles)
    @Test
    fun `given a phrase with streak 3 when opening the session then it is marked as known`() = runTest {
        // Given: tanda de las frases A y B, donde A tiene racha 3
        val practice = FakePracticeRepository(
            activeBatch(order = listOf("A", "B"), progress = listOf(certified("A"), aPhraseProgress(id = "B"))),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"), aCatalogPhrase(id = "B"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        val ready = result.shouldBeInstanceOf<PracticeSessionResult.Ready>()
        ready.items.first { it.id.value == "A" }.isKnown shouldBe true
        ready.items.first { it.id.value == "A" }.streak shouldBe Streak(3)
    }

    // Scenario: Frase con racha 2 no se marca (spec: practice-session, Racha y frase sabida visibles)
    @Test
    fun `given a phrase with streak 2 when opening the session then it is not marked as known`() = runTest {
        // Given: tanda con una frase A de racha 2
        val practice = FakePracticeRepository(
            activeBatch(
                order = listOf("A"),
                progress = listOf(aPhraseProgress(id = "A", streak = 2, answeredInBatch = true)),
            ),
        )
        val catalog = loaded(aCatalogPhrase(id = "A"))

        // When
        val result = useCase(practice, catalog).invoke()

        // Then
        result.shouldBeInstanceOf<PracticeSessionResult.Ready>().items.single().isKnown shouldBe false
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
