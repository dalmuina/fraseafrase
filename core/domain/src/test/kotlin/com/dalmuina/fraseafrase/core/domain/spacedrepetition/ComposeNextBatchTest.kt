package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchComposition.Composed
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ComposeNextBatchTest {

    // Scenario: Cuotas por defecto con material suficiente (spec: Composición de la tanda nueva por cuotas
    // sobre el progreso). Nota: el GIVEN de la spec dice "10 frases a reforzar disponibles", pero la cuota de
    // a reforzar para N=20 es 11 (floor(20*0.55)); con solo 10 no hay "material suficiente" y el resultado no
    // puede dar 11 a reforzar. Se interpreta como "al menos 11", igual que las otras dos cuotas del escenario.
    @Test
    fun `given enough reinforce mastered and new phrases when composing then quotas fill 4 11 and 5`() {
        // Given
        val reinforceIds = (1..11).map { "R$it" }
        val reinforce = reinforceIds.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val masteredIds = (1..6).map { "M$it" }
        val mastered = masteredIds.map { aPhraseProgress(id = it, level = Level(4), dueAtSession = 30) }
        val newIds = (1..20).map { "N$it" }
        val catalog = (reinforceIds + masteredIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            progress = reinforce + mastered,
            catalog = catalog,
            size = BatchSize(20),
            currentSession = 20,
        )

        // Then
        val entries = result.composed().batch.entries.map { it.id.value }
        entries.count { it in reinforceIds } shouldBe 11
        entries.count { it.startsWith("N") } shouldBe 4
        entries.count { it in masteredIds } shouldBe 5
    }

    // Scenario: Reforzar corto cede su hueco a nuevas (spec: Composición de la tanda nueva por cuotas sobre
    // el progreso)
    @Test
    fun `given only five reinforce candidates with plenty of new and mastered when composing then new absorbs gap`() {
        // Given
        val reinforceIds = (1..5).map { "R$it" }
        val reinforce = reinforceIds.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val masteredIds = (1..20).map { "M$it" }
        val mastered = masteredIds.map { aPhraseProgress(id = it, level = Level(4), dueAtSession = 30) }
        val newIds = (1..20).map { "N$it" }
        val catalog = (reinforceIds + masteredIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            progress = reinforce + mastered,
            catalog = catalog,
            size = BatchSize(20),
            currentSession = 20,
        )

        // Then
        val entries = result.composed().batch.entries.map { it.id.value }
        entries.count { it in reinforceIds } shouldBe 5
        entries.count { it.startsWith("N") } shouldBe 10
        entries.count { it in masteredIds } shouldBe 5
    }

    // Scenario: Dominadas corto cede su hueco a reforzar (spec: Composición de la tanda nueva por cuotas
    // sobre el progreso)
    @Test
    fun `given one mastered candidate with plenty of reinforce and new when composing then reinforce absorbs gap`() {
        // Given
        val reinforceIds = (1..20).map { "R$it" }
        val reinforce = reinforceIds.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val masteredIds = listOf("M1")
        val mastered = masteredIds.map { aPhraseProgress(id = it, level = Level(4), dueAtSession = 30) }
        val newIds = (1..20).map { "N$it" }
        val catalog = (reinforceIds + masteredIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            progress = reinforce + mastered,
            catalog = catalog,
            size = BatchSize(20),
            currentSession = 20,
        )

        // Then
        val entries = result.composed().batch.entries.map { it.id.value }
        entries.count { it in reinforceIds } shouldBe 15
        entries.count { it.startsWith("N") } shouldBe 4
        entries.count { it in masteredIds } shouldBe 1
    }

    // Scenario: Cálculo de las cuotas con N igual a 20 (spec: Composición de la tanda nueva por cuotas sobre
    // el progreso)
    @Test
    fun `given size 20 when computing quotas then new is 4 reinforce is 11 and mastered is 5`() {
        // Given
        val reinforceIds = (1..20).map { "R$it" }
        val reinforce = reinforceIds.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val masteredIds = (1..20).map { "M$it" }
        val mastered = masteredIds.map { aPhraseProgress(id = it, level = Level(4), dueAtSession = 30) }
        val newIds = (1..20).map { "N$it" }
        val catalog = (reinforceIds + masteredIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            progress = reinforce + mastered,
            catalog = catalog,
            size = BatchSize(20),
            currentSession = 20,
        )

        // Then
        val entries = result.composed().batch.entries.map { it.id.value }
        entries.count { it in reinforceIds } shouldBe 11
        entries.count { it.startsWith("N") } shouldBe 4
        entries.count { it in masteredIds } shouldBe 5
    }

    // Scenario: Cálculo de las cuotas con un N no divisible exactamente (spec: Composición de la tanda nueva
    // por cuotas sobre el progreso)
    @Test
    fun `given size 17 when computing quotas then new is 3 reinforce is 9 and mastered is 5`() {
        // Given
        val reinforceIds = (1..17).map { "R$it" }
        val reinforce = reinforceIds.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val masteredIds = (1..17).map { "M$it" }
        val mastered = masteredIds.map { aPhraseProgress(id = it, level = Level(4), dueAtSession = 30) }
        val newIds = (1..17).map { "N$it" }
        val catalog = (reinforceIds + masteredIds + newIds).map { aCatalogPhrase(id = it) }

        // When
        val result = compose(
            progress = reinforce + mastered,
            catalog = catalog,
            size = BatchSize(17),
            currentSession = 20,
        )

        // Then
        val entries = result.composed().batch.entries.map { it.id.value }
        entries.count { it in reinforceIds } shouldBe 9
        entries.count { it.startsWith("N") } shouldBe 3
        entries.count { it in masteredIds } shouldBe 5
    }

    // Scenario: Una frase pendiente vuelve a estar disponible en su sesión (spec: Composición de la tanda
    // nueva por cuotas sobre el progreso)
    @Test
    fun `given a phrase due at the current session when composing then it is a reinforce candidate`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = Level(1), dueAtSession = 12)
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When
        val result = compose(progress = listOf(phrase), catalog = catalog, currentSession = 12)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("P")
    }

    // Scenario: Una frase con sesión futura no entra antes de su vencimiento (spec: Composición de la tanda
    // nueva por cuotas sobre el progreso)
    @Test
    fun `given a phrase due at a future session when composing then it is not a reinforce candidate`() {
        // Given
        val phrase = aPhraseProgress(id = "P", level = Level(1), dueAtSession = 13)
        val catalog = listOf(aCatalogPhrase(id = "P"))

        // When
        val result = compose(progress = listOf(phrase), catalog = catalog, currentSession = 12)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: El filtro CEFR no deja bajar de nivel a reforzar ni a dominadas (spec: Composición de la
    // tanda nueva por cuotas sobre el progreso)
    @Test
    fun `given a below level reinforce and mastered phrase when composing then neither enters`() {
        // Given
        val reinforce = aPhraseProgress(id = "R", level = Level(1), dueAtSession = 0)
        val mastered = aPhraseProgress(id = "M", level = Level(4), dueAtSession = 30)
        val catalog = listOf(
            aCatalogPhrase(id = "R", level = CefrLevel.A1),
            aCatalogPhrase(id = "M", level = CefrLevel.A2),
            aCatalogPhrase(id = "N1", level = CefrLevel.B1),
        )

        // When
        val result = compose(
            progress = listOf(reinforce, mastered),
            catalog = catalog,
            cefrLevel = CefrLevel.B1,
            currentSession = 20,
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("N1")
    }

    // Scenario: El filtro CEFR admite niveles iguales o superiores al elegido en reforzar y dominadas (spec:
    // Composición de la tanda nueva por cuotas sobre el progreso)
    @Test
    fun `given a reinforce and mastered phrase of a higher level when composing then both enter`() {
        // Given
        val reinforce = aPhraseProgress(id = "R", level = Level(1), dueAtSession = 0)
        val mastered = aPhraseProgress(id = "M", level = Level(4), dueAtSession = 30)
        val catalog = listOf(
            aCatalogPhrase(id = "R", level = CefrLevel.B1),
            aCatalogPhrase(id = "M", level = CefrLevel.C1),
        )

        // When
        val result = compose(
            progress = listOf(reinforce, mastered),
            catalog = catalog,
            cefrLevel = CefrLevel.A1,
            currentSession = 20,
        )

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("R", "M")
    }

    // Scenario: Sin material del nivel elegido o superior la tanda sale más pequeña (spec: Composición de la
    // tanda nueva por cuotas sobre el progreso)
    @Test
    fun `given only below level material when composing then there is no batch`() {
        // Given
        val reinforce = aPhraseProgress(id = "R", level = Level(1), dueAtSession = 0)
        val mastered = aPhraseProgress(id = "M", level = Level(4), dueAtSession = 30)
        val catalog = listOf(
            aCatalogPhrase(id = "R", level = CefrLevel.A1),
            aCatalogPhrase(id = "M", level = CefrLevel.A1),
        )

        // When
        val result = compose(
            progress = listOf(reinforce, mastered),
            catalog = catalog,
            cefrLevel = CefrLevel.B1,
            currentSession = 20,
        )

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Dominada sin vencer entra en el bloque de dominadas (spec: Frases dominadas disponibles para
    // repaso ligero antes de su vencimiento)
    @Test
    fun `given a mastered phrase not yet due when composing then it enters as mastered`() {
        // Given
        val mastered = aPhraseProgress(id = "M", level = Level(5), dueAtSession = 30)
        val catalog = listOf(aCatalogPhrase(id = "M"))

        // When
        val result = compose(progress = listOf(mastered), catalog = catalog, currentSession = 20)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("M")
    }

    // Scenario: Dominada vencida cuenta como a reforzar (spec: Frases dominadas disponibles para repaso
    // ligero antes de su vencimiento)
    @Test
    fun `given a mastered phrase that is due when composing then it enters only once as reinforce`() {
        // Given
        val masteredDue = aPhraseProgress(id = "M", level = Level(4), dueAtSession = 20)
        val catalog = listOf(aCatalogPhrase(id = "M"))

        // When
        val result = compose(progress = listOf(masteredDue), catalog = catalog, currentSession = 20)

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe listOf("M")
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
        }
    }

    // Scenario: Tanda menor que N si faltan frases (spec: Composición de la tanda o ausencia de tanda)
    @Test
    fun `given three reinforce candidates and size 10 with nothing else available then the batch has three`() {
        // Given
        val reinforceIds = listOf("X", "Y", "Z")
        val progress = reinforceIds.map { aPhraseProgress(id = it, dueAtSession = 0) }
        val catalog = reinforceIds.map { aCatalogPhrase(id = it) }

        // When
        val result = compose(progress = progress, catalog = catalog, size = BatchSize(10))

        // Then
        result.composed().batch.entries.map { it.id.value } shouldBe reinforceIds
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
    fun `given an exhausted B1 catalog with phrases due in the future when composing then there is no batch`() {
        // Given
        val progress = listOf("B1-1", "B1-2", "B1-3").map {
            aPhraseProgress(id = it, level = Level(2), dueAtSession = 30)
        }
        val catalog = listOf("B1-1", "B1-2", "B1-3").map { aCatalogPhrase(id = it, level = CefrLevel.B1) }

        // When
        val result = compose(progress = progress, catalog = catalog, cefrLevel = CefrLevel.B1, currentSession = 20)

        // Then
        result shouldBe BatchComposition.NothingAvailable
    }

    // Scenario: Solo nuevas del nivel elegido (spec: Frases nuevas por nivel CEFR, no afectado por este
    // delta)
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

    private fun compose(
        progress: List<PhraseProgress> = emptyList(),
        catalog: List<CatalogPhrase> = emptyList(),
        cefrLevel: CefrLevel = CefrLevel.A1,
        size: BatchSize = BatchSize.DEFAULT,
        currentSession: Int = 0,
    ): BatchComposition = ComposeNextBatch()(progress, catalog, cefrLevel, size, currentSession)

    private fun BatchComposition.composed(): Composed = shouldBeInstanceOf<Composed>()
}
