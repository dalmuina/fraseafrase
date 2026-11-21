package com.dalmuina.fraseafrase.core.data.catalog

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Scenario: Persistencia del catálogo en Room (spec: phrase-catalog) — JOIN y borrado independiente por clave
class CatalogDaoTest {

    private fun phrase(id: String, targetLocale: String = "pt-PT", position: Int = 0) = CatalogPhraseEntity(
        id = id,
        targetLocale = targetLocale,
        position = position,
        level = "A1",
        topic = "saludos",
        target = "Bom dia",
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

    // Scenario: 3.3 - phrasesFor hace el JOIN y devuelve el source correcto
    @Test
    fun `given a phrase and its translation when querying phrasesFor then the source is joined`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))
        dao.insertTranslations(listOf(translation(id = "a1-saludos-001", source = "Buenos días")))

        // When
        val found = dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "es-ES")

        // Then
        found.map { it.phrase.id to it.source } shouldBe listOf("a1-saludos-001" to "Buenos días")
    }

    // Scenario: 3.3 - deletePhrasesForTargetLocale y deleteTranslationsFor borran solo su propia clave
    @Test
    fun `given translations of two origins when deleting one origin then only that key is removed`() =
        runTest {
            // Given
            val database = inMemoryCatalogDatabase()
            val dao = database.catalogDao()
            dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))
            dao.insertTranslations(
                listOf(
                    translation(id = "a1-saludos-001", sourceLocale = "es-ES"),
                    translation(id = "a1-saludos-001", sourceLocale = "en-GB"),
                ),
            )

            // When
            dao.deleteTranslationsFor(targetLocale = "pt-PT", sourceLocale = "es-ES")

            // Then
            dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "es-ES").shouldBeEmpty()
            dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "en-GB").map { it.phrase.id } shouldBe
                listOf("a1-saludos-001")
        }

    // Scenario: 3.3 - deletePhrasesForTargetLocale solo borra su propio target_locale
    @Test
    fun `given phrases of two target locales when deleting one locale then the other is kept`() = runTest {
        // Given
        val database = inMemoryCatalogDatabase()
        val dao = database.catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-001", targetLocale = "pt-PT")))
        dao.insertPhrases(listOf(phrase(id = "a1-saludos-002", targetLocale = "pt-BR")))

        // When
        dao.deletePhrasesForTargetLocale("pt-PT")

        // Then
        dao.targetLocales() shouldBe listOf("pt-BR")
    }

    // Scenario: 3.3 - sourceLocalesFor lee de catalog_translation, no de catalog_phrase
    @Test
    fun `given translations of two source locales when querying sourceLocalesFor then both are returned`() =
        runTest {
            // Given
            val database = inMemoryCatalogDatabase()
            val dao = database.catalogDao()
            dao.insertPhrases(listOf(phrase(id = "a1-saludos-001")))
            dao.insertTranslations(
                listOf(
                    translation(id = "a1-saludos-001", sourceLocale = "es-ES"),
                    translation(id = "a1-saludos-001", sourceLocale = "en-GB"),
                ),
            )

            // When
            val sourceLocales = dao.sourceLocalesFor("pt-PT")

            // Then
            sourceLocales shouldContainExactlyInAnyOrder listOf("es-ES", "en-GB")
        }

    // Scenario: 3.3 - seedVersionFor versiona por (target_locale, source_locale)
    @Test
    fun `given seed versions of two source locales when querying seedVersionFor then each is independent`() =
        runTest {
            // Given
            val database = inMemoryCatalogDatabase()
            val dao = database.catalogDao()
            dao.upsertSeedVersion(
                CatalogSeedVersionEntity(targetLocale = "pt-PT", sourceLocale = "es-ES", schemaVersion = 1),
            )
            dao.upsertSeedVersion(
                CatalogSeedVersionEntity(targetLocale = "pt-PT", sourceLocale = "en-GB", schemaVersion = 2),
            )

            // When / Then
            dao.seedVersionFor("pt-PT", "es-ES")?.schemaVersion shouldBe 1
            dao.seedVersionFor("pt-PT", "en-GB")?.schemaVersion shouldBe 2
        }
}
