package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomCatalogDataSourceTest {

    private val ptPt = LanguageTag("pt-PT")
    private val esEs = LanguageTag("es-ES")
    private val itIt = LanguageTag("it-IT")

    private fun phraseDocument(
        schemaVersion: Int = 1,
        targetLocale: String = "pt-PT",
        id: String = "a1-saludos-001",
        target: String = "Bom dia",
    ) = """
        {
          "schemaVersion": $schemaVersion,
          "targetLocale": "$targetLocale",
          "phrases": [
            { "id": "$id", "level": "A1", "topic": "saludos", "target": "$target" }
          ]
        }
    """.trimIndent()

    private fun source(database: CatalogDatabase, documents: () -> List<String>) = RoomCatalogDataSource(
        database = database,
        catalogDocuments = documents,
        dispatcher = UnconfinedTestDispatcher(),
    )

    private fun threeLanguages() = listOf(
        phraseDocument(targetLocale = "pt-PT", target = "Bom dia"),
        phraseDocument(targetLocale = "it-IT", target = "Buongiorno"),
        phraseDocument(targetLocale = "es-ES", target = "Buenos días"),
    )

    // Scenario: Primer arranque siembra el catálogo (spec: phrase-catalog)
    @Test
    fun `given no previous seed when reading phrases then every language is queryable from any other`() = runTest {
        // Given
        val dataSource = source(inMemoryCatalogDatabase()) { threeLanguages() }

        // When
        val ptFromEs = dataSource.phrases(ptPt, esEs)
        val ptFromIt = dataSource.phrases(ptPt, itIt)
        val esFromPt = dataSource.phrases(esEs, ptPt)
        val itFromEs = dataSource.phrases(itIt, esEs)

        // Then
        (ptFromEs as CatalogLoadResult.Loaded).phrases.single().source shouldBe "Buenos días"
        (ptFromIt as CatalogLoadResult.Loaded).phrases.single().source shouldBe "Buongiorno"
        (esFromPt as CatalogLoadResult.Loaded).phrases.single().source shouldBe "Bom dia"
        (itFromEs as CatalogLoadResult.Loaded).phrases.single().target shouldBe "Buongiorno"
    }

    // Scenario: Sin cambio de schemaVersion no se resiembra (spec: phrase-catalog)
    @Test
    fun `given an unchanged schemaVersion when seeding twice then the catalog is not re-seeded`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        source(database) { threeLanguages() }.phrases(ptPt, esEs)
        database.catalogDao().deletePhrasesForTargetLocale("pt-PT")

        // When
        val result = source(database) { threeLanguages() }.phrases(ptPt, esEs)

        // Then: la segunda instancia ve que el schemaVersion no cambió y no vuelve a insertar las filas borradas
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.shouldBeEmpty()
    }

    // Scenario: Un cambio de schemaVersion vuelve a sembrar solo el idioma afectado (spec: phrase-catalog)
    @Test
    fun `given a changed schemaVersion when seeding again then only that language is re-seeded`() = runTest {
        // Given: marca antigua de pt-PT; es-ES con filas borradas pero marca vigente
        val database = inMemoryCatalogDatabase()
        source(database) { threeLanguages() }.phrases(ptPt, esEs)
        database.catalogDao().upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "pt-PT", schemaVersion = 0))
        database.catalogDao().deletePhrasesForTargetLocale("es-ES")

        // When
        val dataSource = source(database) {
            listOf(
                phraseDocument(targetLocale = "pt-PT", target = "Bom dia (actualizado)"),
                phraseDocument(targetLocale = "es-ES", target = "Buenos días"),
            )
        }
        val result = dataSource.phrases(ptPt, itIt)

        // Then: pt-PT se resiembra; es-ES no (sigue vacío); it-IT conserva sus filas
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.single().target shouldBe "Bom dia (actualizado)"
        database.catalogDao().phrasesFor("pt-PT", "es-ES").shouldBeEmpty()
    }

    // Scenario: Dos idiomas conviven de forma independiente (spec: phrase-catalog)
    @Test
    fun `given two language documents when seeding then both are available independently`() = runTest {
        // Given
        val dataSource = source(inMemoryCatalogDatabase()) {
            listOf(
                phraseDocument(targetLocale = "pt-PT", id = "id-1"),
                phraseDocument(targetLocale = "pt-BR", id = "id-1"),
            )
        }

        // When
        val available = dataSource.availableTargetLocales()

        // Then
        available shouldContainExactlyInAnyOrder listOf(ptPt, LanguageTag("pt-BR"))
    }

    // Scenario: Un idioma con catálogo inválido no está disponible (spec: phrase-catalog)
    @Test
    fun `given one invalid language document when seeding then the other language is not affected`() = runTest {
        // Given
        val invalid = phraseDocument(targetLocale = "it-IT").replace("\"level\": \"A1\"", "\"level\": \"Z9\"")
        val dataSource = source(inMemoryCatalogDatabase()) { listOf(phraseDocument(), invalid) }

        // When
        val available = dataSource.availableTargetLocales()
        val fromInvalidOrigin = dataSource.phrases(ptPt, itIt)

        // Then
        available shouldBe listOf(ptPt)
        fromInvalidOrigin.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        fromInvalidOrigin.phrases.shouldBeEmpty()
    }
}
