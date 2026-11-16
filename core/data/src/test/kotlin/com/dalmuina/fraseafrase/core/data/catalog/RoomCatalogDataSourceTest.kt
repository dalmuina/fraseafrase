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

    private fun document(schemaVersion: Int = 1, targetLocale: String = "pt-PT", id: String = "a1-saludos-001") =
        """
        {
          "schemaVersion": $schemaVersion,
          "targetLocale": "$targetLocale",
          "sourceLocale": "es-ES",
          "phrases": [
            { "id": "$id", "level": "A1", "topic": "saludos", "target": "Bom dia", "source": "Buenos días" }
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
        val dataSource = source(database) { listOf(document()) }

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
            listOf(document())
        }

        // When
        dataSource.phrases(ptPt, esEs)
        database.catalogDao().deletePhrasesForTargetLocale("pt-PT")
        val secondInstanceSameSeed = source(database) {
            reads++
            listOf(document())
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
        source(database) { listOf(document(schemaVersion = 1, id = "old")) }.phrases(ptPt, esEs)
        database.catalogDao().upsertSeedVersion(
            CatalogSeedVersionEntity(targetLocale = "pt-PT", schemaVersion = 0),
        )

        // When
        val result =
            source(database) { listOf(document(schemaVersion = 1, id = "new")) }.phrases(ptPt, esEs)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.map { it.id.value } shouldBe listOf("new")
    }

    // Scenario: Dos idiomas destino conviven de forma independiente (spec: phrase-catalog)
    @Test
    fun `given two target locale documents when seeding then both are available independently`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dataSource = source(database) {
            listOf(
                document(targetLocale = "pt-PT", id = "pt-pt-id"),
                document(targetLocale = "pt-BR", id = "pt-br-id"),
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
        val invalidDocument = document(targetLocale = "pt-BR").replace("\"level\": \"A1\"", "\"level\": \"Z9\"")
        val dataSource = source(database) { listOf(document(), invalidDocument) }

        // When
        val availableTargetLocales = dataSource.availableTargetLocales()
        val ptPtPhrases = dataSource.phrases(ptPt, esEs)

        // Then
        availableTargetLocales shouldBe listOf(ptPt)
        ptPtPhrases.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        ptPtPhrases.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001")
    }
}
