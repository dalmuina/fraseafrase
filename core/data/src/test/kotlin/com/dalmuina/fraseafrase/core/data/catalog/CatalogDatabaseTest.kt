package com.dalmuina.fraseafrase.core.data.catalog

import androidx.sqlite.SQLiteException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Scenario: Persistencia del catálogo en Room (spec: phrase-catalog) — esquema de catalog_phrase/catalog_translation
class CatalogDatabaseTest {

    private fun phrase(id: String, targetLocale: String = "pt-PT", position: Int = 0, target: String = "Bom dia") =
        CatalogPhraseEntity(
            id = id,
            targetLocale = targetLocale,
            position = position,
            level = "A1",
            topic = "saludos",
            target = target,
        )

    private fun translation(
        id: String,
        targetLocale: String = "pt-PT",
        sourceLocale: String = "es-ES",
        source: String = "Buenos días",
    ) = CatalogTranslationEntity(
        id = id,
        targetLocale = targetLocale,
        sourceLocale = sourceLocale,
        source = source,
    )

    @Test
    fun `given a phrase row when inserted then it is found by target locale`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()

        // When
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))
        dao.insertTranslations(listOf(translation(id = "a1-saludos-001")))
        val found = dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "es-ES")

        // Then
        found.map { it.phrase.id } shouldBe listOf("a1-saludos-001")
    }

    @Test
    fun `given rows of a different locale pair when querying then nothing is found`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", targetLocale = "pt-BR")))
        dao.insertTranslations(listOf(translation(id = "a1-saludos-001", targetLocale = "pt-BR")))

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
        dao.seedVersionFor("pt-PT", CatalogSeedVersionEntity.NO_SOURCE_LOCALE)?.schemaVersion shouldBe 2
    }

    // Scenario: 3.1 - PK de catalog_phrase es (id, target_locale), sin source_locale
    @Test
    fun `given a phrase row when inserting another with the same id and target locale then it fails`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))

        // When / Then
        shouldThrow<SQLiteException> {
            dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", target = "otro texto")))
        }
    }

    // Scenario: 3.1 - PK de catalog_translation es (id, target_locale, source_locale)
    @Test
    fun `given a translation row when inserting another with a different source locale then both are kept`() =
        runTest {
            // Given
            val database = inMemoryCatalogDatabase()
            val dao = database.catalogDao()
            dao.insertTranslations(listOf(translation(id = "a1-saludos-001", sourceLocale = "es-ES")))

            // When
            dao.insertTranslations(listOf(translation(id = "a1-saludos-001", sourceLocale = "en-GB")))

            // Then
            dao.sourceLocalesFor("pt-PT") shouldContainExactlyInAnyOrder listOf("es-ES", "en-GB")
        }

    // Scenario: 3.1 - PK de catalog_translation es (id, target_locale, source_locale)
    @Test
    fun `given a translation row when inserting another with the same full key then it fails`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()
        dao.insertTranslations(listOf(translation(id = "a1-saludos-001")))

        // When / Then
        shouldThrow<SQLiteException> {
            dao.insertTranslations(listOf(translation(id = "a1-saludos-001", source = "otro texto")))
        }
    }
}
