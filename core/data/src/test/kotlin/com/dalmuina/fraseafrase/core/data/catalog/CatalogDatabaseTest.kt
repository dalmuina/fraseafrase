package com.dalmuina.fraseafrase.core.data.catalog

import androidx.sqlite.SQLiteException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Requirement: Persistencia y resembrado del catálogo en Room (spec: phrase-catalog) — esquema de catalog_phrase y de
// la huella de siembra por idioma
class CatalogDatabaseTest {

    private fun phrase(id: String, targetLocale: String = "pt-PT", target: String = "Bom dia") =
        CatalogPhraseEntity(
            id = id,
            targetLocale = targetLocale,
            position = 0,
            level = "A1",
            topic = "saludos",
            target = target,
        )

    @Test
    fun `given a seed version row when upserted twice then the latest content hash wins`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()

        // When
        dao.upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "pt-PT", contentHash = "first"))
        dao.upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "pt-PT", contentHash = "second"))

        // Then
        dao.seedVersionFor("pt-PT")?.contentHash shouldBe "second"
    }

    // Scenario: PK de catalog_phrase es (id, target_locale)
    @Test
    fun `given a phrase row when inserting another with the same id and target locale then it fails`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))

        // When / Then
        shouldThrow<SQLiteException> {
            dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", target = "otro texto")))
        }
    }

    // Scenario: PK de catalog_phrase es (id, target_locale): el mismo id convive en dos idiomas
    @Test
    fun `given a phrase row when inserting the same id for another locale then both are kept`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", targetLocale = "pt-PT")))

        // When
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", targetLocale = "es-ES")))

        // Then
        dao.targetLocales().toSet() shouldBe setOf("pt-PT", "es-ES")
    }
}
