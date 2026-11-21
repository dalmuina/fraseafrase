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
    private val enGb = LanguageTag("en-GB")

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

    private fun translationDocument(
        schemaVersion: Int = 1,
        targetLocale: String = "pt-PT",
        sourceLocale: String = "es-ES",
        id: String = "a1-saludos-001",
        source: String = "Buenos días",
    ) = """
        {
          "schemaVersion": $schemaVersion,
          "targetLocale": "$targetLocale",
          "sourceLocale": "$sourceLocale",
          "translations": [
            { "id": "$id", "source": "$source" }
          ]
        }
    """.trimIndent()

    private fun source(database: CatalogDatabase, documents: () -> List<String>) = RoomCatalogDataSource(
        database = database,
        catalogDocuments = documents,
        dispatcher = UnconfinedTestDispatcher(),
    )

    // Scenario: Primer arranque siembra el catálogo (spec: phrase-catalog)
    @Test
    fun `given no previous seed when reading phrases then the catalog is seeded and returned`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dataSource = source(database) { listOf(phraseDocument(), translationDocument()) }

        // When
        val result = dataSource.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001")
    }

    // Scenario: Sin cambio de schemaVersion no se resiembra (spec: phrase-catalog)
    @Test
    fun `given an unchanged schemaVersion when seeding twice then the catalog is not re-seeded`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        var reads = 0
        val dataSource = source(database) {
            reads++
            listOf(phraseDocument(), translationDocument())
        }

        // When
        dataSource.phrases(ptPt, esEs)
        database.catalogDao().deletePhrasesForTargetLocale("pt-PT")
        val secondInstanceSameSeed = source(database) {
            reads++
            listOf(phraseDocument(), translationDocument())
        }
        val result = secondInstanceSameSeed.phrases(ptPt, esEs)

        // Then: la segunda instancia ve que el schemaVersion no cambió y no vuelve a insertar las filas borradas
        reads shouldBe 2
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.shouldBeEmpty()
    }

    // Scenario: Un cambio de schemaVersion vuelve a sembrar sin tocar el progreso (spec: phrase-catalog)
    @Test
    fun `given a changed schemaVersion when seeding again then the catalog is re-seeded`() = runTest {
        // Given: un idioma destino ya sembrado cuya marca de versión guardada queda desactualizada respecto
        // al schemaVersion soportado (solo 1 está soportado hoy: no se puede empaquetar un documento con otro
        // valor y que siga siendo válido, así que se simula la marca antigua directamente sobre la tabla de
        // seguimiento del sembrado, igual que ocurriría tras una actualización real de la app)
        val database = inMemoryCatalogDatabase()
        source(database) { listOf(phraseDocument(), translationDocument()) }.phrases(ptPt, esEs)
        database.catalogDao().upsertSeedVersion(
            CatalogSeedVersionEntity(targetLocale = "pt-PT", schemaVersion = 0),
        )

        // When
        val result = source(database) {
            listOf(phraseDocument(target = "Bom dia (actualizado)"), translationDocument())
        }.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.single().target shouldBe "Bom dia (actualizado)"
    }

    // Scenario: Dos idiomas destino conviven de forma independiente (spec: phrase-catalog)
    @Test
    fun `given two target locale documents when seeding then both are available independently`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dataSource = source(database) {
            listOf(
                phraseDocument(targetLocale = "pt-PT", id = "pt-pt-id"),
                translationDocument(targetLocale = "pt-PT", id = "pt-pt-id"),
                phraseDocument(targetLocale = "pt-BR", id = "pt-br-id"),
                translationDocument(targetLocale = "pt-BR", id = "pt-br-id"),
            )
        }

        // When
        val availableTargetLocales = dataSource.availableTargetLocales()
        val ptPtPhrases = dataSource.phrases(ptPt, esEs)
        val ptBrPhrases = dataSource.phrases(LanguageTag("pt-BR"), esEs)

        // Then
        availableTargetLocales shouldContainExactlyInAnyOrder listOf(ptPt, LanguageTag("pt-BR"))
        ptPtPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        ptPtPhrases.phrases.map { it.id.value } shouldBe listOf("pt-pt-id")
        ptBrPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        ptBrPhrases.phrases.map { it.id.value } shouldBe listOf("pt-br-id")
    }

    // Scenario: Un idioma destino con catálogo inválido no está disponible (spec: phrase-catalog)
    @Test
    fun `given one invalid target locale document when seeding then the other locale is not affected`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val invalidDocument = phraseDocument(targetLocale = "pt-BR").replace("\"level\": \"A1\"", "\"level\": \"Z9\"")
        val dataSource = source(database) {
            listOf(phraseDocument(), translationDocument(), invalidDocument)
        }

        // When
        val availableTargetLocales = dataSource.availableTargetLocales()
        val ptPtPhrases = dataSource.phrases(ptPt, esEs)

        // Then
        availableTargetLocales shouldBe listOf(ptPt)
        ptPtPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        ptPtPhrases.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001")
    }

    // Scenario: Un cambio de schemaVersion en un documento de traducción vuelve a sembrarlo sin tocar las
    // frases destino ni otros orígenes (spec: phrase-catalog)
    @Test
    fun `given a reseeded translation document when seeding then its phrases and other origins are kept`() =
        runTest {
            // Given: pt-PT ya sembrado con dos orígenes (es-ES y en-GB)
            val database = inMemoryCatalogDatabase()
            source(database) {
                listOf(
                    phraseDocument(),
                    translationDocument(sourceLocale = "es-ES"),
                    translationDocument(sourceLocale = "en-GB"),
                )
            }.availableTargetLocales()

            // When: se resiembra solo el documento de traducción es-ES, con otro schemaVersion
            database.catalogDao().upsertSeedVersion(
                CatalogSeedVersionEntity(targetLocale = "pt-PT", sourceLocale = "es-ES", schemaVersion = 0),
            )
            val dataSource = source(database) {
                listOf(
                    phraseDocument(),
                    translationDocument(sourceLocale = "es-ES", source = "Buenos días (actualizado)"),
                    translationDocument(sourceLocale = "en-GB"),
                )
            }
            val esEsPhrases = dataSource.phrases(ptPt, esEs)
            val enGbPhrases = dataSource.phrases(ptPt, enGb)

            // Then
            esEsPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
            esEsPhrases.phrases.single().source shouldBe "Buenos días (actualizado)"
            esEsPhrases.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001")
            enGbPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
            enGbPhrases.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001")
        }

    // Scenario: Traducción sin frase destino correspondiente rechaza la carga (spec: phrase-catalog)
    @Test
    fun `given a translation id without a matching target phrase when seeding then nothing of it is seeded`() =
        runTest {
            // Given: pt-PT sin el id a1-saludos-999, y una traducción que sí lo incluye
            val database = inMemoryCatalogDatabase()
            val dataSource = source(database) {
                listOf(
                    phraseDocument(id = "a1-saludos-001"),
                    translationDocument(id = "a1-saludos-999"),
                )
            }

            // When
            val ptPtPhrases = dataSource.phrases(ptPt, esEs)
            val availableSourceLocales = dataSource.availableSourceLocales(ptPt)

            // Then
            ptPtPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
            ptPtPhrases.phrases.shouldBeEmpty()
            availableSourceLocales.shouldBeEmpty()
        }

    // Scenario: Persistencia del catálogo en Room (spec: phrase-catalog) - orden inverso de documentos nuevos
    @Test
    fun `given a translation document delivered before its target phrases when seeding then both are seeded`() =
        runTest {
            // Given: el documento de traducción llega antes que el de frases destino de su mismo idioma destino
            val database = inMemoryCatalogDatabase()
            val dataSource = source(database) {
                listOf(translationDocument(), phraseDocument())
            }

            // When
            val result = dataSource.phrases(ptPt, esEs)

            // Then
            result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
            result.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001")
        }
}
