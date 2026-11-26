package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.data.practice.PhraseProgressEntity
import com.dalmuina.fraseafrase.core.data.practice.inMemoryPracticeDatabase
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
        targetLocale: String = "pt-PT",
        id: String = "0000-a1-saludos-001",
        target: String = "Bom dia",
    ) = """
        {
          "schemaVersion": 1,
          "targetLocale": "$targetLocale",
          "phrases": [
            { "id": "$id", "level": "A1", "topic": "saludos", "target": "$target" }
          ]
        }
    """.trimIndent()

    private fun twoPhraseDocument(targetLocale: String, first: String, second: String) = """
        {
          "schemaVersion": 1,
          "targetLocale": "$targetLocale",
          "phrases": [
            { "id": "0000-a1-saludos-001", "level": "A1", "topic": "saludos", "target": "$first" },
            { "id": "0000-a1-saludos-002", "level": "A1", "topic": "saludos", "target": "$second" }
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

    // Scenario: Una frase añadida llega a una instalación ya sembrada (spec: phrase-catalog)
    @Test
    fun `given a seeded catalog when documents add a phrase then both phrases are returned in order`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        source(database) {
            listOf(
                phraseDocument(targetLocale = "pt-PT", target = "Bom dia"),
                phraseDocument(targetLocale = "es-ES", target = "Buenos días"),
            )
        }.phrases(ptPt, esEs)

        // When
        val result = source(database) {
            listOf(
                twoPhraseDocument("pt-PT", "Bom dia", "Boa noite"),
                twoPhraseDocument("es-ES", "Buenos días", "Buenas noches"),
            )
        }.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.map { it.target to it.source } shouldBe
            listOf("Bom dia" to "Buenos días", "Boa noite" to "Buenas noches")
    }

    // Scenario: Un texto corregido sustituye al anterior sin afectar a otros idiomas (spec: phrase-catalog)
    @Test
    fun `given a corrected text when seeding again then only that language is re-seeded`() = runTest {
        // Given: it-IT con filas borradas a mano para detectar si se vuelve a sembrar
        val database = inMemoryCatalogDatabase()
        source(database) { threeLanguages() }.phrases(ptPt, esEs)
        database.catalogDao().deletePhrasesForTargetLocale("it-IT")

        // When
        val result = source(database) {
            listOf(
                phraseDocument(targetLocale = "pt-PT", target = "Bom dia (corrigido)"),
                phraseDocument(targetLocale = "it-IT", target = "Buongiorno"),
                phraseDocument(targetLocale = "es-ES", target = "Buenos días"),
            )
        }.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.single().target shouldBe "Bom dia (corrigido)"
        database.catalogDao().phrasesFor("it-IT", "es-ES").shouldBeEmpty()
    }

    // Scenario: Sin cambio de contenido no se vuelve a sembrar (spec: phrase-catalog)
    @Test
    fun `given identical documents when seeding twice then the catalog is not re-seeded`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        source(database) { threeLanguages() }.phrases(ptPt, esEs)
        database.catalogDao().deletePhrasesForTargetLocale("pt-PT")

        // When
        val result = source(database) { threeLanguages() }.phrases(ptPt, esEs)

        // Then: la segunda instancia ve la misma huella y no vuelve a insertar las filas borradas
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.shouldBeEmpty()
    }

    // Scenario: Un documento nuevo inválido conserva lo sembrado (spec: phrase-catalog)
    @Test
    fun `given a seeded language when its new document is invalid then the seeded phrases are kept`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        source(database) { threeLanguages() }.phrases(ptPt, esEs)
        val invalid = phraseDocument(targetLocale = "pt-PT", target = "Boa tarde")
            .replace("\"level\": \"A1\"", "\"level\": \"Z9\"")

        // When
        val result = source(database) { listOf(invalid) }.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.map { it.target } shouldBe listOf("Bom dia")
    }

    // Scenario: Resembrar un idioma no cambia el progreso guardado (spec: phrase-catalog)
    @Test
    fun `given saved progress when re-seeding its language then the progress is unchanged`() = runTest {
        // Given
        val catalog = inMemoryCatalogDatabase()
        val practice = inMemoryPracticeDatabase()
        source(catalog) { listOf(phraseDocument(), phraseDocument(targetLocale = "es-ES", target = "Buenos días")) }
            .phrases(ptPt, esEs)
        val progress = PhraseProgressEntity(
            phraseId = "0000-a1-saludos-001",
            targetLocale = "pt-PT",
            level = 2,
            streak = 1,
            answeredInBatch = false,
            dueAtSession = 0,
            batchPosition = null,
        )
        practice.phraseProgressDao().insert(progress)

        // When
        val result = source(catalog) {
            listOf(
                twoPhraseDocument("pt-PT", "Bom dia", "Boa noite"),
                twoPhraseDocument("es-ES", "Buenos días", "Buenas noches"),
            )
        }.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.size shouldBe 2
        practice.phraseProgressDao().all("pt-PT") shouldBe listOf(progress)
        practice.close()
    }

    // Scenario: Primer arranque tras restaurar (spec: data-backup)
    @Test
    fun `given saved progress and no catalog when reading phrases then the catalog is seeded and progress is kept`() =
        runTest {
            // Given: base de catálogo vacía y progreso restaurado en la base de práctica
            val catalog = inMemoryCatalogDatabase()
            val practice = inMemoryPracticeDatabase()
            val progress = PhraseProgressEntity(
                phraseId = "0000-a1-saludos-001",
                targetLocale = "pt-PT",
                level = 2,
                streak = 1,
                answeredInBatch = false,
                dueAtSession = 0,
                batchPosition = null,
            )
            practice.phraseProgressDao().insert(progress)

            // When
            val result = source(catalog) {
                listOf(phraseDocument(), phraseDocument(targetLocale = "es-ES", target = "Buenos días"))
            }.phrases(ptPt, esEs)

            // Then
            result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
            result.phrases.map { it.target } shouldBe listOf("Bom dia")
            practice.phraseProgressDao().all("pt-PT") shouldBe listOf(progress)
            practice.close()
        }

    // Scenario: Dos idiomas conviven de forma independiente (spec: phrase-catalog)
    @Test
    fun `given two language documents when seeding then both are available independently`() = runTest {
        // Given
        val dataSource = source(inMemoryCatalogDatabase()) {
            listOf(
                phraseDocument(targetLocale = "pt-PT", id = "id-1"),
                phraseDocument(targetLocale = "en-GB", id = "id-1"),
            )
        }

        // When
        val available = dataSource.availableTargetLocales()

        // Then
        available shouldContainExactlyInAnyOrder listOf(ptPt, LanguageTag("en-GB"))
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
