package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.testing.FixedClock
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ComposeNextBatchTest {

    private val today = LocalDate.of(2026, 3, 10)

    // Scenario: Arrastradas van antes que pendientes (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given a carried and a pending phrase when composing then the carried one comes first`() {
        // Given
        val carried = aPhraseProgress(id = "X")
        val pending = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 9))
        val catalog = listOf(aCatalogPhrase(id = "X"), aCatalogPhrase(id = "P"))

        // When
        val result = compose(
            carry = CarryQueue(ids("X")),
            progress = listOf(carried, pending),
            catalog = catalog,
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("X", "P")
    }

    // Scenario: Arrastrada entra primera en la siguiente tanda (spec: Cola de arrastradas ordenada)
    @Test
    fun `given a leftover carried phrase and a pending one when composing then the carried one enters first`() {
        // Given
        val leftover = aPhraseProgress(id = "Z")
        val pending = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 9))
        val catalog = listOf(aCatalogPhrase(id = "Z"), aCatalogPhrase(id = "P"))

        // When
        val result = compose(
            carry = CarryQueue(ids("Z")),
            progress = listOf(leftover, pending),
            catalog = catalog,
            size = BatchSize(10),
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("Z", "P")
    }

    // Scenario: Arrastrada que no cabe conserva su progreso (spec: Cola de arrastradas ordenada)
    @Test
    fun `given eleven carried phrases with size 10 when composing then the eleventh stays first in the leftover`() {
        // Given
        val carriedIds = (1..10).map { "X$it" } + "P"
        val progress = carriedIds.map { id ->
            if (id == "P") {
                aPhraseProgress(id = id, level = 3, nextReview = LocalDate.of(2026, 3, 20))
            } else {
                aPhraseProgress(id = id)
            }
        }
        val catalog = carriedIds.map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            carry = CarryQueue(ids(*carriedIds.toTypedArray())),
            progress = progress,
            catalog = catalog,
            size = BatchSize(10),
        )

        // Then
        val composed = result.composed()
        composed.batch.entries.map { it.id.value } shouldBe (1..10).map { "X$it" }
        composed.leftover shouldBe CarryQueue(ids("P"))
    }

    // Scenario: Pendientes por nivel ascendente (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given two pending phrases of different levels when composing then the lower level comes first`() {
        // Given
        val higher = aPhraseProgress(id = "P1", level = 3, nextReview = LocalDate.of(2026, 3, 1))
        val lower = aPhraseProgress(id = "P2", level = 1, nextReview = LocalDate.of(2026, 3, 9))
        val catalog = listOf(aCatalogPhrase(id = "P1"), aCatalogPhrase(id = "P2"))

        // When
        val result = compose(progress = listOf(higher, lower), catalog = catalog)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("P2", "P1")
    }

    // Scenario: A igual nivel, la fecha de repaso más antigua primero (spec: Composición por prioridad)
    @Test
    fun `given two level 2 pending phrases with different dates when composing then the older one comes first`() {
        // Given
        val later = aPhraseProgress(id = "P1", level = 2, nextReview = LocalDate.of(2026, 3, 8))
        val earlier = aPhraseProgress(id = "P2", level = 2, nextReview = LocalDate.of(2026, 3, 5))
        val catalog = listOf(aCatalogPhrase(id = "P1"), aCatalogPhrase(id = "P2"))

        // When
        val result = compose(progress = listOf(later, earlier), catalog = catalog)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("P2", "P1")
    }

    // Scenario: A igual nivel y fecha, orden de catálogo (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given two level 2 pending phrases with the same date when composing then catalog order decides`() {
        // Given
        val first = aPhraseProgress(id = "P1", level = 2, nextReview = LocalDate.of(2026, 3, 5))
        val second = aPhraseProgress(id = "P2", level = 2, nextReview = LocalDate.of(2026, 3, 5))
        val catalog = listOf(aCatalogPhrase(id = "P2"), aCatalogPhrase(id = "P1"))

        // When
        val result = compose(progress = listOf(first, second), catalog = catalog)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("P2", "P1")
    }

    // Scenario: Nuevas solo si queda hueco (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given six progress phrases and four new phrases with size 10 when composing then new ones fill the gap`() {
        // Given
        val carried = listOf("A", "B", "C", "D").map { aPhraseProgress(id = it) }
        val pending = listOf("E", "F").map {
            aPhraseProgress(id = it, level = 1, nextReview = LocalDate.of(2026, 3, 9))
        }
        val catalog = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            carry = CarryQueue(ids("A", "B", "C", "D")),
            progress = carried + pending,
            catalog = catalog,
            size = BatchSize(10),
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe
            listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
    }

    // Scenario: Sin nuevas mientras queden pendientes sin caber (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given twelve pending phrases and a new phrase with size 10 when composing then no new phrase enters`() {
        // Given
        val progress = twelvePendingPhrases()
        val catalog = twelvePendingIds().map { aCatalogPhrase(id = it) } + aCatalogPhrase(id = "N1")

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10))

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe twelvePendingIds().take(10)
    }

    // Scenario: Pendientes que no caben esperan sin perder nivel (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given twelve pending phrases with size 10 when composing then the last two stay out of the batch`() {
        // Given
        val progress = twelvePendingPhrases()
        val catalog = twelvePendingIds().map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10))

        // Then
        val ids = result.composed().batch.entries.map { it.id.value }
        ids shouldNotContain "P11"
        ids shouldNotContain "P12"
    }

    // Scenario: Arrastrada y pendiente aparece una sola vez (spec: Frase en un único lugar)
    @Test
    fun `given a phrase both carried and pending when composing then it appears once at position 0`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 9))
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When
        val result = compose(
            carry = CarryQueue(ids("P")),
            progress = listOf(phrase),
            catalog = catalog,
            size = BatchSize(10),
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("P")
    }

    // Scenario: Frase programada no entra como nueva (spec: Frase en un único lugar)
    @Test
    fun `given a scheduled phrase of the chosen level when composing then there is no batch`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = 2, nextReview = LocalDate.of(2026, 3, 13))
        val catalog = listOf(aCatalogPhrase(id = "P", level = CefrLevel.B1))

        // When
        val result = compose(progress = listOf(phrase), catalog = catalog, cefrLevel = CefrLevel.B1)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: El filtro CEFR no afecta a arrastradas ni pendientes (spec: Composición por prioridad)
    @Test
    fun `given a carried B2 and a pending C1 phrase with chosen level A1 when composing then both enter`() {
        // Given
        val carried = aPhraseProgress(id = "X")
        val pending = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 9))
        val catalog = listOf(
            aCatalogPhrase(id = "X", level = CefrLevel.B2),
            aCatalogPhrase(id = "P", level = CefrLevel.C1),
        )

        // When
        val result = compose(
            carry = CarryQueue(ids("X")),
            progress = listOf(carried, pending),
            catalog = catalog,
            cefrLevel = CefrLevel.A1,
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("X", "P")
    }

    // Scenario: Primera tanda con catálogo disponible (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given no progress and fifteen A1 phrases when composing then the first batch has all of them`() {
        // Given
        val catalog = (1..15).map { aCatalogPhrase(id = "A1-%02d".format(it)) }

        // When
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.A1, size = BatchSize.DEFAULT)

        // Then
        val entries = result.composed().batch.entries
        entries.map { it.id.value } shouldBe catalog.map { it.id.value }
        entries.forEach { entry ->
            entry.level shouldBe Level(0)
            entry.streak shouldBe Streak(0)
            entry.answeredInBatch shouldBe false
            entry.nextReview shouldBe null
        }
    }

    // Scenario: Tanda menor que N si faltan frases (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given three carried phrases and size 10 with nothing else available then the batch has three`() {
        // Given
        val progress = listOf("X", "Y", "Z").map { aPhraseProgress(id = it) }
        val catalog = listOf("X", "Y", "Z").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            carry = CarryQueue(ids("X", "Y", "Z")),
            progress = progress,
            catalog = catalog,
            size = BatchSize(10),
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("X", "Y", "Z")
    }

    // Scenario: Catálogo vacío sin progreso no crea tanda (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given an empty catalog and no progress when composing then there is no batch`() {
        // When
        val result = compose(catalog = emptyList())

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Catálogo sin frases del nivel elegido no crea tanda (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given a catalog without phrases of the chosen level when composing then there is no batch`() {
        // Given
        val catalog = listOf(aCatalogPhrase(id = "A1-1", level = CefrLevel.A1))

        // When
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.B1)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Catálogo agotado sin pendientes no crea tanda (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given an exhausted B1 catalog with scheduled phrases and no carry then there is no batch`() {
        // Given
        val progress = listOf("B1-1", "B1-2", "B1-3").map {
            aPhraseProgress(id = it, level = 2, nextReview = LocalDate.of(2026, 3, 13))
        }
        val catalog = listOf("B1-1", "B1-2", "B1-3").map { aCatalogPhrase(id = it, level = CefrLevel.B1) }

        // When
        val result = compose(progress = progress, catalog = catalog, cefrLevel = CefrLevel.B1)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Solo nuevas del nivel elegido (spec: Frases nuevas por nivel CEFR)
    @Test
    fun `given A1 and B1 new phrases with chosen level A1 when composing then only A1 phrases enter`() {
        // Given
        val catalog = listOf(
            aCatalogPhrase(id = "A1-1", level = CefrLevel.A1),
            aCatalogPhrase(id = "A1-2", level = CefrLevel.A1),
            aCatalogPhrase(id = "B1-1", level = CefrLevel.B1),
        )

        // When
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.A1, size = BatchSize.DEFAULT)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("A1-1", "A1-2")
    }

    // Scenario: Certificada que vuelve como pendiente entra con racha 0 (spec: Racha inicial en cada tanda nueva)
    @Test
    fun `given a certified phrase that comes back as pending when composing then it enters with streak 0`() {
        // Given
        val certified = aPhraseProgress(id = "C", level = 0, streak = 3, answeredInBatch = true)
        val closed = ResolveBatchClosure(FixedClock(today))(listOf(certified)).single().progress
        val catalog = listOf(aCatalogPhrase(id = "C"))

        // When
        val result = ComposeNextBatch(FixedClock(LocalDate.of(2026, 3, 11)))(
            carry = CarryQueue(emptyList()),
            progress = listOf(closed),
            catalog = catalog,
            cefrLevel = CefrLevel.A1,
            size = BatchSize.DEFAULT,
        )

        // Then
        val entry = result.composed().batch.entries.single()
        entry.level shouldBe Level(1)
        entry.streak shouldBe Streak(0)
        entry.answeredInBatch shouldBe false
    }

    // Scenario: Frase con repaso futuro no entra antes de su fecha (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given a phrase scheduled for tomorrow when composing today then there is no batch`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 11))
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When
        val result = compose(progress = listOf(phrase), catalog = catalog)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Frase con repaso de hoy entra en su fecha (spec: Composición de la tanda nueva por prioridad)
    @Test
    fun `given a phrase scheduled for today when composing on that day then it enters the batch`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 11))
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When
        val result = ComposeNextBatch(FixedClock(LocalDate.of(2026, 3, 11)))(
            carry = CarryQueue(emptyList()),
            progress = listOf(phrase),
            catalog = catalog,
            cefrLevel = CefrLevel.A1,
            size = BatchSize.DEFAULT,
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("P")
    }

    // Scenario: Tanda activa conserva su tamaño al cambiar N (spec: Tamaño de tanda N)
    @Test
    fun `given a closed batch of 30 and a new size 10 when composing then the batch keeps 30 and the next has 10`() {
        // Given
        val ids = (1..30).map { "F%02d".format(it) }
        val active = PracticeBatch(ids.map { aPhraseProgress(id = it) }, BatchSize(30))
        val closed = ResolveBatchClosure(FixedClock(today))(active.entries)
        val carried = closed.filterIsInstance<BatchClosureOutcome.Carried>().map { it.progress.id }
        val catalog = ids.map { aCatalogPhrase(id = it) }

        // When
        val result = ComposeNextBatch(FixedClock(today))(
            carry = CarryQueue(carried),
            progress = closed.map { it.progress },
            catalog = catalog,
            cefrLevel = CefrLevel.A1,
            size = BatchSize(10),
        )

        // Then
        val closedBatch = PracticeBatch(closed.map { it.progress }, BatchSize(30))
        closedBatch.entries.size shouldBe 30
        closedBatch.capacity shouldBe BatchSize(30)
        val next = result.composed().batch
        next.entries.size shouldBeLessThanOrEqualTo 10
        next.capacity shouldBe BatchSize(10)
    }

    // Scenario: Composición con una arrastrada sin progreso (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a carried id without progress when composing then throws IllegalArgumentException`() {
        // Given
        val catalog = listOf(aCatalogPhrase(id = "X"))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            compose(carry = CarryQueue(ids("X")), catalog = catalog)
        }
    }

    // Scenario: Progreso sin entrada en el catálogo (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given progress of a phrase missing from the catalog when composing then throws IllegalArgumentException`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 9))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            compose(progress = listOf(phrase), catalog = emptyList())
        }
    }

    // Scenario: Progreso con ids duplicados (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given two progress entries of the same phrase when composing then throws IllegalArgumentException`() {
        // Given
        val progress = listOf(aPhraseProgress(id = "P"), aPhraseProgress(id = "P"))
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            compose(progress = progress, catalog = catalog)
        }
    }

    // Scenario: Catálogo con ids duplicados (spec: Invariantes de la tanda y de la cola)
    @Test
    fun `given a catalog listing the same phrase twice when composing then throws IllegalArgumentException`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = 1, nextReview = LocalDate.of(2026, 3, 9))
        val catalog = listOf(aCatalogPhrase(id = "P"), aCatalogPhrase(id = "P"))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            compose(progress = listOf(phrase), catalog = catalog)
        }
    }

    private fun twelvePendingIds(): List<String> = (1..12).map { "P%02d".format(it) }

    private fun twelvePendingPhrases(): List<PhraseProgress> = twelvePendingIds().map {
        aPhraseProgress(id = it, level = 1, nextReview = LocalDate.of(2026, 3, 9))
    }

    private fun ids(vararg values: String): List<PhraseId> = values.map { PhraseId(it) }

    private fun compose(
        carry: CarryQueue = CarryQueue(emptyList()),
        progress: List<PhraseProgress> = emptyList(),
        catalog: List<CatalogPhrase> = emptyList(),
        cefrLevel: CefrLevel = CefrLevel.A1,
        size: BatchSize = BatchSize.DEFAULT,
    ): BatchComposition = ComposeNextBatch(FixedClock(today))(carry, progress, catalog, cefrLevel, size)

    private fun BatchComposition.composed(): Composed = shouldBeInstanceOf<Composed>()
}
