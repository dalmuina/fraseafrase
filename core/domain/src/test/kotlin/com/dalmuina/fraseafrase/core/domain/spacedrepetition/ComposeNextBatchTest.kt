package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import com.dalmuina.fraseafrase.core.testing.anAnswerHistory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ComposeNextBatchTest {

    // Scenario: Una frase pendiente vuelve a estar disponible en su sesión (spec: Candidatas a la tanda nueva)
    @Test
    fun `given a phrase due at the current session when composing then it enters the batch`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = Level(1), dueAtSession = 12)
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When
        val result = compose(progress = listOf(phrase), catalog = catalog, currentSession = 12)

        // Then
        result.ids() shouldBe listOf("P")
    }

    // Scenario: Una frase con sesión futura no entra antes de su vencimiento (spec: Candidatas a la tanda nueva)
    @Test
    fun `given ten pending and a phrase due at a future session when composing size 10 then it does not enter`() {
        // Given
        val pending = (1..10).map { aPhraseProgress(id = "P%02d".format(it), level = Level(1), dueAtSession = 12) }
        val progress = pending + aPhraseProgress(id = "F", level = Level(1), dueAtSession = 13)

        // When
        val result = compose(
            progress = progress,
            catalog = progress.map { aCatalogPhrase(id = it.id.value) },
            size = BatchSize(10),
            currentSession = 12,
        )

        // Then
        result.ids() shouldBe pending.map { it.id.value }
    }

    // Scenario: Una frase con sesión futura entra como repaso adelantado si falta sitio (spec: Candidatas a la
    // tanda nueva)
    @Test
    fun `given two pending and a future phrase when composing size 10 then the future one enters as early review`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "P1", level = Level(1), dueAtSession = 12),
            aPhraseProgress(id = "P2", level = Level(1), dueAtSession = 12),
            aPhraseProgress(id = "F", level = Level(1), dueAtSession = 13),
        )
        val catalog = listOf("F", "P1", "P2").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10), currentSession = 12)

        // Then
        result.ids() shouldBe listOf("P1", "P2", "F")
    }

    // Scenario: Una frase de nivel alto sin vencer no entra (spec: Candidatas a la tanda nueva)
    @Test
    fun `given eight pending and many new when composing size 10 then high level phrases not yet due do not enter`() {
        // Given
        val pending = pendingPhrases(count = 8, level = 1)
        val progress = pending +
            aPhraseProgress(id = "H5", level = Level(5), dueAtSession = 30) +
            aPhraseProgress(id = "H4", level = Level(4), dueAtSession = 25)

        // When
        val result = composeAtSession20(progress, size = 10)

        // Then
        result.ids().toSet() shouldBe (pending.map { it.id.value } + listOf("N01", "N02")).toSet()
    }

    // Scenario: Una pendiente de nivel CEFR inferior o superior no entra (spec: Candidatas a la tanda nueva)
    @Test
    fun `given pending and future phrases of other CEFR levels when composing then none of them enters`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "R", level = Level(1), dueAtSession = 0),
            aPhraseProgress(id = "S", level = Level(1), dueAtSession = 0),
            aPhraseProgress(id = "T", level = Level(1), dueAtSession = 30),
        )
        val catalog = listOf(
            aCatalogPhrase(id = "R", level = CefrLevel.A1),
            aCatalogPhrase(id = "S", level = CefrLevel.C1),
            aCatalogPhrase(id = "T", level = CefrLevel.C1),
            aCatalogPhrase(id = "N1", level = CefrLevel.B1),
        )

        // When
        val result = compose(progress = progress, catalog = catalog, cefrLevel = CefrLevel.B1, currentSession = 20)

        // Then
        result.ids() shouldBe listOf("N1")
    }

    // Scenario: Una pendiente del nivel CEFR elegido entra (spec: Candidatas a la tanda nueva)
    @Test
    fun `given two pending phrases of the chosen CEFR level when composing then both enter`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "R", level = Level(1), dueAtSession = 0),
            aPhraseProgress(id = "S", level = Level(1), dueAtSession = 0),
        )
        val catalog = listOf("R", "S").map { aCatalogPhrase(id = it, level = CefrLevel.A1) }

        // When
        val result = compose(progress = progress, catalog = catalog, cefrLevel = CefrLevel.A1, currentSession = 20)

        // Then
        result.ids().toSet() shouldBe setOf("R", "S")
    }

    // Scenario: Sin material del nivel elegido la tanda sale más pequeña (spec: Candidatas a la tanda nueva)
    @Test
    fun `given only two pending phrases of the chosen level and no new ones when composing then the batch has two`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "O1", level = Level(1), dueAtSession = 0),
            aPhraseProgress(id = "O2", level = Level(1), dueAtSession = 0),
            aPhraseProgress(id = "B1", level = Level(1), dueAtSession = 0),
            aPhraseProgress(id = "B2", level = Level(1), dueAtSession = 0),
        )
        val catalog = listOf(
            aCatalogPhrase(id = "O1", level = CefrLevel.A1),
            aCatalogPhrase(id = "O2", level = CefrLevel.C1),
            aCatalogPhrase(id = "B1", level = CefrLevel.B1),
            aCatalogPhrase(id = "B2", level = CefrLevel.B1),
        )

        // When
        val result = compose(
            progress = progress,
            catalog = catalog,
            cefrLevel = CefrLevel.B1,
            size = BatchSize(20),
            currentSession = 20,
        )

        // Then
        result.ids() shouldBe listOf("B1", "B2")
    }

    // Scenario: Menor nivel va antes a igual retraso (spec: Prioridad de las pendientes)
    @Test
    fun `given pending phrases with equal delay when composing then the lower level goes first`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "A", level = Level(2), dueAtSession = 20),
            aPhraseProgress(id = "B", level = Level(0), dueAtSession = 20),
        )
        val catalog = listOf("A", "B").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, currentSession = 20)

        // Then
        result.ids() shouldBe listOf("B", "A")
    }

    // Scenario: El retraso adelanta a una frase de nivel alto (spec: Prioridad de las pendientes)
    @Test
    fun `given a delayed high level phrase when composing then it goes before a fresh level zero one`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "A", level = Level(3), dueAtSession = 15),
            aPhraseProgress(id = "B", level = Level(0), dueAtSession = 20),
        )
        val catalog = listOf("B", "A").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, currentSession = 20)

        // Then
        result.ids() shouldBe listOf("A", "B")
    }

    // Scenario: Empate de prioridad se resuelve por catálogo (spec: Prioridad de las pendientes)
    @Test
    fun `given pending phrases with the same priority when composing then catalog order breaks the tie`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "A", level = Level(4), dueAtSession = 16),
            aPhraseProgress(id = "B", level = Level(0), dueAtSession = 20),
        )
        val catalog = listOf("B", "A").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, currentSession = 20)

        // Then
        result.ids() shouldBe listOf("B", "A")
    }

    // Scenario: Las pendientes que no caben esperan (spec: Prioridad de las pendientes)
    @Test
    fun `given seventeen pending phrases and size 10 when composing then the lowest priority ones wait`() {
        // Given
        val zeroIds = (1..5).map { "Z$it" }
        val oneIds = (1..12).map { "U%02d".format(it) }
        val progress = zeroIds.map { aPhraseProgress(id = it, level = Level(0), dueAtSession = 20) } +
            oneIds.map { aPhraseProgress(id = it, level = Level(1), dueAtSession = 20) }
        val catalog = (oneIds + zeroIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10), currentSession = 20)

        // Then
        result.ids() shouldBe zeroIds + oneIds.take(5)
    }

    // Scenario: Tras la primera tanda entran como mucho N/5 nuevas (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given twelve consolidated pending when composing size 10 then there are 2 new and 8 pending`() {
        // When
        val ids = composeAtSession20(pendingPhrases(count = 12, level = 1), size = 10).ids()

        // Then
        ids.count { it.startsWith("N") } shouldBe 2
        ids.count { it.startsWith("P") } shouldBe 8
    }

    // Scenario: Con N igual a 17 el límite es 3 nuevas (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given twenty pending and size 17 when composing then there are 3 new and 14 pending`() {
        // When
        val ids = composeAtSession20(pendingPhrases(count = 20, level = 1), size = 17).ids()

        // Then
        ids.count { it.startsWith("N") } shouldBe 3
        ids.count { it.startsWith("P") } shouldBe 14
    }

    // Scenario: Aprendizaje saturado no admite nuevas (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given twelve level zero pending when composing size 10 then there are no new`() {
        // When
        val ids = composeAtSession20(pendingPhrases(count = 12, level = 0), size = 10).ids()

        // Then
        ids.count { it.startsWith("P") } shouldBe 10
        ids.count { it.startsWith("N") } shouldBe 0
    }

    // Scenario: Con once frases de nivel 0 aún entran nuevas (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given eleven level zero pending when composing size 10 then there are 2 new and 8 pending`() {
        // When
        val ids = composeAtSession20(pendingPhrases(count = 11, level = 0), size = 10).ids()

        // Then
        ids.count { it.startsWith("N") } shouldBe 2
        ids.count { it.startsWith("P") } shouldBe 8
    }

    // Scenario: Las frases de nivel 0 de otro nivel CEFR no cuentan (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given level zero phrases of another CEFR level when composing then they do not block the new ones`() {
        // Given
        val other = group("B", count = 12, level = 0, dueAtSession = 20)
        val pending = pendingPhrases(count = 8, level = 1)
        val newIds = (1..20).map { "N%02d".format(it) }
        val catalog = other.map { aCatalogPhrase(id = it.id.value, level = CefrLevel.B1) } +
            (pending.map { it.id.value } + newIds).map { aCatalogPhrase(id = it, level = CefrLevel.A1) }

        // When
        val result = compose(progress = other + pending, catalog = catalog, size = BatchSize(10), currentSession = 20)

        // Then
        result.ids().toSet() shouldBe (pending.map { it.id.value } + newIds.take(2)).toSet()
    }

    // Scenario: Primera tanda de un nivel CEFR con progreso en otro nivel (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given progress only in another CEFR level when composing the first batch then it has ten new phrases`() {
        // Given
        val other = aPhraseProgress(id = "A1-1", level = Level(2), dueAtSession = 20)
        val newPhrases = (1..15).map { aCatalogPhrase(id = "B1-%02d".format(it), level = CefrLevel.B1) }
        val catalog = listOf(aCatalogPhrase(id = "A1-1", level = CefrLevel.A1)) + newPhrases

        // When
        val result = compose(
            progress = listOf(other),
            catalog = catalog,
            cefrLevel = CefrLevel.B1,
            size = BatchSize(10),
            currentSession = 20,
        )

        // Then
        result.ids() shouldBe newPhrases.take(10).map { it.id.value }
        result.composed().batch.entries.forEach { entry ->
            entry.level shouldBe Level(0)
            entry.streak shouldBe Streak(0)
        }
    }

    // Scenario: El repaso adelantado entra antes que más nuevas (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given three pending, five early and many new when composing size 10 then there are 3, 5 early and 2 new`() {
        // Given
        val progress = pendingPhrases(count = 3, level = 1) + earlyPhrases(count = 5, level = 2)

        // When
        val ids = composeAtSession20(progress, size = 10).ids()

        // Then
        ids.count { it.startsWith("P") } shouldBe 3
        ids.count { it.startsWith("R") } shouldBe 5
        ids.count { it.startsWith("N") } shouldBe 2
    }

    // Scenario: Las nuevas completan la tanda si no hay otras frases (spec: Límite de frases nuevas por tanda)
    @Test
    fun `given three pending and no other phrase when composing size 10 then seven new complete the batch`() {
        // When
        val ids = composeAtSession20(pendingPhrases(count = 3, level = 1), size = 10).ids()

        // Then
        ids.size shouldBe 10
        ids.count { it.startsWith("P") } shouldBe 3
        ids.filter { it.startsWith("N") } shouldBe (1..7).map { "N%02d".format(it) }
    }

    // Scenario: Dos nuevas en una tanda de diez (spec: Reparto de las nuevas en la tanda)
    @Test
    fun `given eight level zero pending and many new when composing with size 10 then two new are spread`() {
        // Given
        val pendingIds = (1..8).map { "P$it" }
        val newIds = (1..20).map { "N%02d".format(it) }
        val progress = pendingIds.map { aPhraseProgress(id = it, level = Level(0), dueAtSession = 20) }
        val catalog = (pendingIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10), currentSession = 20)

        // Then
        result.ids() shouldBe listOf("P1", "P2", "P3", "N01", "P4", "P5", "N02", "P6", "P7", "P8")
    }

    // Scenario: Cuatro nuevas en una tanda de diez (spec: Reparto de las nuevas en la tanda)
    @Test
    fun `given six level one pending and many new when composing with size 10 then four new are spread`() {
        // Given
        val pendingIds = (1..6).map { "P$it" }
        val newIds = (1..20).map { "N%02d".format(it) }
        val progress = pendingIds.map { aPhraseProgress(id = it, level = Level(1), dueAtSession = 20) }
        val catalog = (pendingIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10), currentSession = 20)

        // Then
        result.ids() shouldBe listOf("P1", "P2", "N01", "P3", "N02", "P4", "N03", "P5", "N04", "P6")
    }

    // Scenario: Las nuevas se reparten también entre el repaso adelantado (spec: Reparto de las nuevas en la
    // tanda)
    @Test
    fun `given three pending, three early and many new when composing size 10 then new spread among both`() {
        // Given
        val progress = pendingPhrases(count = 3, level = 1) + earlyPhrases(count = 3, level = 2)

        // When
        val result = composeAtSession20(progress, size = 10)

        // Then
        result.ids() shouldBe listOf("P1", "P2", "N01", "P3", "N02", "R1", "N03", "R2", "N04", "R3")
    }

    // Scenario: Con dos o más pendientes las nuevas no quedan todas al final (spec: Reparto de las nuevas en la
    // tanda)
    @Test
    fun `given at least two non new and one new when composing then the last position is not a new phrase`() {
        // Given
        val progress = pendingPhrases(count = 3, level = 1)

        // When
        val result = composeAtSession20(progress, size = 10)

        // Then
        result.ids().size shouldBe 10
        result.ids().last() shouldBe "P3"
    }

    // Scenario: Solo entra el repaso adelantado que cabe (spec: Orden del repaso adelantado)
    @Test
    fun `given room for one early review when composing then the lowest level plus distance enters`() {
        // Given: A is worth 2 + 1 = 3 and B is worth 0 + 2 = 2
        val pending = pendingPhrases(count = 9, level = 1)
        val progress = pending +
            aPhraseProgress(id = "A", level = Level(2), dueAtSession = 21) +
            aPhraseProgress(id = "B", level = Level(0), dueAtSession = 22)
        val catalog = progress.map { aCatalogPhrase(id = it.id.value) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10), currentSession = 20)

        // Then
        result.ids() shouldBe pending.map { it.id.value } + "B"
    }

    // Scenario: Empate en el repaso adelantado se resuelve por catálogo (spec: Orden del repaso adelantado)
    @Test
    fun `given early reviews with the same value when composing then catalog order breaks the tie`() {
        // Given
        val progress = listOf(
            aPhraseProgress(id = "A", level = Level(1), dueAtSession = 22),
            aPhraseProgress(id = "B", level = Level(2), dueAtSession = 21),
        )
        val catalog = listOf("B", "A").map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10), currentSession = 20)

        // Then
        result.ids() shouldBe listOf("B", "A")
    }

    // Scenario: Primera tanda con catálogo disponible (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given no progress and fifteen A1 phrases with size 20 when composing then the batch has all of them`() {
        // Given
        val catalog = (1..15).map { aCatalogPhrase(id = "A1-%02d".format(it)) }

        // When
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.A1, size = BatchSize(20))

        // Then
        val entries = result.composed().batch.entries
        entries.map { it.id.value } shouldBe catalog.map { it.id.value }
        entries.forEach { entry ->
            entry.level shouldBe Level(0)
            entry.streak shouldBe Streak(0)
            entry.answeredInBatch shouldBe false
        }
    }

    // Scenario: Primera tanda con más frases que N (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given no progress and fifteen A1 phrases with size 10 when composing then the batch has the first ten`() {
        // Given
        val catalog = (1..15).map { aCatalogPhrase(id = "A1-%02d".format(it)) }

        // When
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.A1, size = BatchSize(10))

        // Then
        val entries = result.composed().batch.entries
        entries.map { it.id.value } shouldBe catalog.take(10).map { it.id.value }
        entries.forEach { entry ->
            entry.level shouldBe Level(0)
            entry.streak shouldBe Streak(0)
        }
    }

    // Scenario: Tanda menor que N si faltan frases (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given three pending candidates and size 10 with nothing else available then the batch has three`() {
        // Given
        val ids = listOf("X", "Y", "Z")
        val progress = ids.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val catalog = ids.map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10))

        // Then
        result.ids() shouldBe ids
    }

    // Scenario: Catálogo vacío sin progreso no crea tanda (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given an empty catalog and no progress when composing then there is no batch`() {
        // When
        val result = compose(catalog = emptyList())

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Catálogo sin frases del nivel elegido no crea tanda (spec: Composición de la tanda o
    // ausencia de tanda)
    @Test
    fun `given a catalog without phrases of the chosen level when composing then there is no batch`() {
        // Given
        val catalog = listOf(aCatalogPhrase(id = "A1-1", level = CefrLevel.A1))

        // When
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.B1)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Catálogo agotado sin pendientes no crea tanda (spec: Composición de la tanda o ausencia de
    // tanda)
    @Test
    fun `given only B1 progress and no A1 phrase when composing A1 then there is no batch`() {
        // Given
        val progress = listOf("B1-1", "B1-2").map { aPhraseProgress(id = it, level = Level(2), dueAtSession = 30) }
        val catalog = listOf("B1-1", "B1-2").map { aCatalogPhrase(id = it, level = CefrLevel.B1) }

        // When
        val result = compose(progress = progress, catalog = catalog, cefrLevel = CefrLevel.A1, currentSession = 20)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Solo repasos futuros crean una tanda de repaso adelantado (spec: Composición de la tanda o
    // ausencia de tanda)
    @Test
    fun `given only future reviews when composing then an early review batch keeps their progress`() {
        // Given
        val ids = listOf("B1-1", "B1-2", "B1-3")
        val progress = ids.map {
            aPhraseProgress(id = it, level = Level(2), streak = Streak(1), answeredInBatch = true, dueAtSession = 30)
        }
        val catalog = ids.map { aCatalogPhrase(id = it, level = CefrLevel.B1) }

        // When
        val result = compose(
            progress = progress,
            catalog = catalog,
            cefrLevel = CefrLevel.B1,
            size = BatchSize(10),
            currentSession = 20,
        )

        // Then
        result.composed().batch.entries shouldBe progress.map { it.copy(answeredInBatch = false) }
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
        val result = compose(catalog = catalog, cefrLevel = CefrLevel.A1, size = BatchSize(20))

        // Then
        result.ids() shouldBe listOf("A1-1", "A1-2")
    }

    // Scenario: Progreso sin entrada en el catálogo (spec: Invariantes de la tanda)
    @Test
    fun `given progress of a phrase missing from the catalog when composing then throws IllegalArgumentException`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = Level(1), dueAtSession = 0)

        // When / Then
        shouldThrow<IllegalArgumentException> {
            compose(progress = listOf(phrase), catalog = emptyList())
        }
    }

    // Scenario: Progreso con ids duplicados (spec: Invariantes de la tanda)
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

    // Scenario: Catálogo con ids duplicados (spec: Invariantes de la tanda)
    @Test
    fun `given a catalog listing the same phrase twice when composing then throws IllegalArgumentException`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = Level(1), dueAtSession = 0)
        val catalog = listOf(aCatalogPhrase(id = "P"), aCatalogPhrase(id = "P"))

        // When / Then
        shouldThrow<IllegalArgumentException> {
            compose(progress = listOf(phrase), catalog = catalog)
        }
    }

    // Scenario: Una fallada en la sesión anterior entra la primera (spec: Composición de la tanda por tramos)
    @Test
    fun `given a phrase missed in the previous session when composing then it goes first despite its priority`() {
        // Given
        val missed = aPhraseProgress(id = "F", level = Level(2), dueAtSession = 20).answeredAt(19, missed = true)
        val pending = group("P", count = 12, level = 0, dueAtSession = 20).map { it.answeredAt(17) }

        // When
        val result = composeAtSession20(pending + missed, size = 10)

        // Then
        result.ids() shouldBe listOf("F") + ids("P", 1..9)
    }

    // Scenario: Una fallada que no cupo vuelve en la sesión siguiente (spec: Composición de la tanda por tramos)
    @Test
    fun `given a phrase missed two sessions ago when composing then it takes the first position`() {
        // Given
        val missed = aPhraseProgress(id = "F", level = Level(2), dueAtSession = 20).answeredAt(19, missed = true)
        val pending = group("P", count = 10, level = 0, dueAtSession = 21).map { it.answeredAt(17) }

        // When
        val result = composeWithoutNew(pending + missed, currentSession = 21)

        // Then
        result.ids().first() shouldBe "F"
    }

    // Scenario: Las falladas repetidas no superan el máximo de repetidas (spec: Composición de la tanda por tramos)
    @Test
    fun `given six missed repeated phrases when composing size 10 then only four of them enter`() {
        // Given
        val missed = group("F", count = 6, level = 0, dueAtSession = 20).map { it.answeredAt(19, missed = true) }
        val pending = group("P", count = 6, level = 0, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(missed + pending)

        // Then
        result.ids() shouldBe ids("F", 1..4) + ids("P", 1..6)
    }

    // Scenario: Se completan las repetidas hasta el mínimo (spec: Composición de la tanda por tramos)
    @Test
    fun `given repeated phrases not yet due when composing size 10 then three of them enter before the pending`() {
        // Given
        val repeated = group("R", count = 5, level = 1, dueAtSession = 21).map { it.answeredAt(19) }
        val pending = group("P", count = 10, level = 1, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(repeated + pending)

        // Then
        result.ids() shouldBe ids("R", 1..3) + ids("P", 1..7)
    }

    // Scenario: Las pendientes repetidas no superan el máximo de repetidas (spec: Composición de la tanda por
    // tramos)
    @Test
    fun `given eight repeated pending when composing size 10 then four enter and early review fills the batch`() {
        // Given
        val repeated = group("R", count = 8, level = 0, dueAtSession = 20).map { it.answeredAt(19) }
        val pending = group("P", count = 2, level = 0, dueAtSession = 20).map { it.answeredAt(15) }
        val early = group("Q", count = 5, level = 1, dueAtSession = 22).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(repeated + pending + early)

        // Then
        result.ids() shouldBe ids("R", 1..4) + ids("P", 1..2) + ids("Q", 1..4)
    }

    // Scenario: El máximo de repetidas se supera solo para llenar la tanda (spec: Composición de la tanda por tramos)
    @Test
    fun `given almost only repeated phrases when composing size 10 then the maximum is exceeded to fill it`() {
        // Given
        val repeated = group("R", count = 9, level = 0, dueAtSession = 20).map { it.answeredAt(19) }
        val pending = group("P", count = 1, level = 0, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(repeated + pending)

        // Then
        result.ids() shouldBe ids("R", 1..4) + "P1" + ids("R", 5..9)
    }

    // Scenario: Una fallada en la sesión actual vuelve al recomponer (spec: Frases repetidas y falladas por volver)
    @Test
    fun `given a phrase missed in the current session when recomposing then it takes the first position`() {
        // Given
        val missed = aPhraseProgress(id = "F", level = Level(2), dueAtSession = 21).answeredAt(20, missed = true)
        val pending = group("P", count = 10, level = 0, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(pending + missed)

        // Then
        result.ids().first() shouldBe "F"
    }

    // Scenario: Una fallada hace tres sesiones no tiene preferencia (spec: Frases repetidas y falladas por volver)
    @Test
    fun `given a phrase missed three sessions ago when composing then it has no preference`() {
        // Given
        val missed = aPhraseProgress(id = "F", level = Level(3), dueAtSession = 18).answeredAt(17, missed = true)
        val pending = group("P", count = 10, level = 0, dueAtSession = 20).map { it.answeredAt(16) }

        // When
        val result = composeWithoutNew(pending + missed)

        // Then
        result.ids() shouldBe ids("P", 1..10)
    }

    // Scenario: Una fallada que ya se respondió después no tiene preferencia (spec: Frases repetidas y falladas por
    // volver)
    @Test
    fun `given a missed phrase answered correctly afterwards when composing then it has no preference`() {
        // Given
        val recovered = aPhraseProgress(id = "F", level = Level(3), dueAtSession = 21)
            .copy(answerHistory = anAnswerHistory(18, lastMissSession = 17))
        val pending = group("P", count = 10, level = 0, dueAtSession = 20).map { it.answeredAt(16) }

        // When
        val result = composeWithoutNew(pending + recovered)

        // Then
        result.ids() shouldBe ids("P", 1..10)
    }

    // Scenario: Una frase con seis sesiones seguidas descansa (spec: Frases en descanso)
    @Test
    fun `given a phrase answered six sessions in a row when composing then it does not enter`() {
        // Given
        val resting = aPhraseProgress(id = "D", level = Level(0), dueAtSession = 20)
            .answeredAt(19, missed = true, consecutive = 6)
        val pending = group("P", count = 10, level = 0, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(listOf(resting) + pending)

        // Then
        result.ids() shouldBe ids("P", 1..10)
    }

    // Scenario: Una frase con cinco sesiones seguidas puede entrar (spec: Frases en descanso)
    @Test
    fun `given a phrase answered five sessions in a row when composing then it enters`() {
        // Given
        val phrase = aPhraseProgress(id = "D", level = Level(0), dueAtSession = 20).answeredAt(19, consecutive = 5)
        val pending = group("P", count = 10, level = 0, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(pending + phrase)

        // Then
        result.ids().contains("D") shouldBe true
    }

    // Scenario: Una frase en descanso vuelve tras una sesión sin responderla (spec: Frases en descanso)
    @Test
    fun `given a resting phrase not answered in the previous session when composing then it enters`() {
        // Given
        val phrase = aPhraseProgress(id = "D", level = Level(0), dueAtSession = 20).answeredAt(19, consecutive = 6)

        // When
        val result = composeWithoutNew(listOf(phrase), currentSession = 21)

        // Then
        result.ids() shouldBe listOf("D")
    }

    // Scenario: Las nuevas entran antes que una frase en descanso (spec: Frases en descanso)
    @Test
    fun `given a resting phrase and many new when composing size 10 then the new ones enter instead`() {
        // Given
        val resting = aPhraseProgress(id = "D", level = Level(0), dueAtSession = 20).answeredAt(19, consecutive = 6)
        val pending = group("P", count = 3, level = 1, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeAtSession20(listOf(resting) + pending, size = 10)

        // Then
        result.ids().toSet() shouldBe (ids("P", 1..3) + (1..7).map { "N%02d".format(it) }).toSet()
    }

    // Scenario: Una frase en descanso entra si no hay otra para llenar la tanda (spec: Frases en descanso)
    @Test
    fun `given nothing else to fill the batch when composing then the resting phrase enters last`() {
        // Given
        val resting = aPhraseProgress(id = "D", level = Level(0), dueAtSession = 20).answeredAt(19, consecutive = 6)
        val pending = group("P", count = 3, level = 1, dueAtSession = 20).map { it.answeredAt(15) }

        // When
        val result = composeWithoutNew(listOf(resting) + pending)

        // Then
        result.ids() shouldBe ids("P", 1..3) + "D"
    }

    private fun ids(prefix: String, range: IntRange): List<String> = range.map { "$prefix$it" }

    private fun group(prefix: String, count: Int, level: Int, dueAtSession: Int): List<PhraseProgress> =
        ids(prefix, 1..count).map { aPhraseProgress(id = it, level = Level(level), dueAtSession = dueAtSession) }

    private fun PhraseProgress.answeredAt(session: Int, missed: Boolean = false, consecutive: Int = 1): PhraseProgress =
        copy(answerHistory = anAnswerHistory(session, consecutive, session.takeIf { missed }))

    /** Composes size 10 with a catalog holding only the progress phrases, in their order. */
    private fun composeWithoutNew(progress: List<PhraseProgress>, currentSession: Int = 20): BatchComposition =
        compose(
            progress = progress,
            catalog = progress.map { aCatalogPhrase(id = it.id.value) },
            size = BatchSize(10),
            currentSession = currentSession,
        )

    /** Pending phrases P1..Pn, all due at session 20. */
    private fun pendingPhrases(count: Int, level: Int): List<PhraseProgress> =
        (1..count).map { aPhraseProgress(id = "P$it", level = Level(level), dueAtSession = 20) }

    /** Early review phrases R1..Rn, all due after session 20. */
    private fun earlyPhrases(count: Int, level: Int): List<PhraseProgress> =
        (1..count).map { aPhraseProgress(id = "R$it", level = Level(level), dueAtSession = 25) }

    /** Composes at session 20 with a catalog holding the progress phrases followed by twenty new ones. */
    private fun composeAtSession20(progress: List<PhraseProgress>, size: Int): BatchComposition =
        compose(progress = progress, catalog = catalogOf(progress), size = BatchSize(size), currentSession = 20)

    private fun catalogOf(progress: List<PhraseProgress>): List<CatalogPhrase> =
        (progress.map { it.id.value } + (1..20).map { "N%02d".format(it) }).map { aCatalogPhrase(id = it) }

    private fun compose(
        progress: List<PhraseProgress> = emptyList(),
        catalog: List<CatalogPhrase> = emptyList(),
        cefrLevel: CefrLevel = CefrLevel.A1,
        size: BatchSize = BatchSize.DEFAULT,
        currentSession: Int = 0,
    ): BatchComposition = ComposeNextBatch()(progress, catalog, cefrLevel, size, currentSession)

    private fun BatchComposition.composed(): Composed = shouldBeInstanceOf<Composed>()

    private fun BatchComposition.ids(): List<String> = composed().batch.entries.map { it.id.value }
}
