package com.dalmuina.fraseafrase.core.data.catalog

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Scenario: Persistencia del catálogo en Room (spec: phrase-catalog) — insertar y consultar por (target, source)
class CatalogDatabaseTest {

    private fun phrase(id: String, targetLocale: String = "pt-PT", sourceLocale: String = "es-ES", position: Int = 0) =
        CatalogPhraseEntity(
            id = id,
            targetLocale = targetLocale,
            sourceLocale = sourceLocale,
            position = position,
            level = "A1",
            topic = "saludos",
            target = "Bom dia",
            source = "Buenos días",
        )

    @Test
    fun `given a phrase row when inserted then it is found by target and source locale`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()

        // When
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))
        val found = dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "es-ES")

        // Then
        found.map { it.id } shouldBe listOf("a1-saludos-001")
    }

    @Test
    fun `given rows of a different locale pair when querying then nothing is found`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", targetLocale = "pt-BR")))

        // When
        val found = dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "es-ES")

        // Then
        found.shouldBeEmpty()
    }

    @Test
    fun `given a seed version row when upserted twice then the latest schemaVersion wins`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()

        // When
        dao.upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "pt-PT", schemaVersion = 1))
        dao.upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "pt-PT", schemaVersion = 2))

        // Then
        dao.seedVersionFor("pt-PT")?.schemaVersion shouldBe 2
    }
}
