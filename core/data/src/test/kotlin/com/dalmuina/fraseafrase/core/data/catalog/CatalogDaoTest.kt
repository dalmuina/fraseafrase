package com.dalmuina.fraseafrase.core.data.catalog

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Requirement: Persistencia y resembrado del catálogo en Room (spec: phrase-catalog) — self-JOIN entre idiomas
class CatalogDaoTest {

    private fun phrase(
        id: String,
        targetLocale: String,
        target: String,
        level: String = "A1",
        position: Int = 0,
    ) = CatalogPhraseEntity(
        id = id,
        targetLocale = targetLocale,
        position = position,
        level = level,
        topic = "saludos",
        target = target,
    )

    // Scenario: El origen sale del texto destino del otro idioma (spec: phrase-catalog)
    @Test
    fun `given phrases of pt-PT and es-ES when querying pt-PT from es-ES then the source is the es-ES target`() =
        runTest {
            // Given
            val dao = inMemoryCatalogDatabase().catalogDao()
            dao.insertPhrases(
                listOf(
                    phrase(id = "0000-a1-saludos-001", targetLocale = "pt-PT", target = "Bom dia", level = "A1"),
                    phrase(id = "0000-a1-saludos-001", targetLocale = "es-ES", target = "Buenos días", level = "A2"),
                ),
            )

            // When
            val found = dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "es-ES")

            // Then
            val row = found.single()
            row.phrase.id shouldBe "0000-a1-saludos-001"
            row.phrase.target shouldBe "Bom dia"
            row.phrase.level shouldBe "A1"
            row.phrase.targetLocale shouldBe "pt-PT"
            row.source shouldBe "Buenos días"
            row.sourceLocale shouldBe "es-ES"
        }

    // Scenario: Un par sin fichero propio está disponible (spec: phrase-catalog)
    @Test
    fun `given phrases of fr-FR and de-DE when querying de-DE from fr-FR then every phrase is returned`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()
        dao.insertPhrases(
            listOf(
                phrase(id = "a", targetLocale = "de-DE", target = "Guten Morgen", position = 0),
                phrase(id = "b", targetLocale = "de-DE", target = "Danke", position = 1),
                phrase(id = "a", targetLocale = "fr-FR", target = "Bonjour", position = 0),
                phrase(id = "b", targetLocale = "fr-FR", target = "Merci", position = 1),
            ),
        )

        // When
        val found = dao.phrasesFor(targetLocale = "de-DE", sourceLocale = "fr-FR")

        // Then
        found.map { it.phrase.id to it.source } shouldBe listOf("a" to "Bonjour", "b" to "Merci")
    }

    // Scenario: Un idioma de origen con documento inválido no devuelve frases (spec: phrase-catalog)
    @Test
    fun `given a source locale without seeded phrases when querying then the list is empty`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a", targetLocale = "pt-PT", target = "Bom dia")))

        // When
        val found = dao.phrasesFor(targetLocale = "pt-PT", sourceLocale = "it-IT")

        // Then
        found.shouldBeEmpty()
    }

    // Requirement: Persistencia y resembrado del catálogo en Room - el borrado de un idioma no afecta a otro
    @Test
    fun `given phrases of two target locales when deleting one locale then the other is kept`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()
        dao.insertPhrases(listOf(phrase(id = "a", targetLocale = "pt-PT", target = "Bom dia")))
        dao.insertPhrases(listOf(phrase(id = "b", targetLocale = "en-GB", target = "Bom dia")))

        // When
        dao.deletePhrasesForTargetLocale("pt-PT")

        // Then
        dao.targetLocales() shouldBe listOf("en-GB")
    }

    // Requirement: Persistencia y resembrado del catálogo en Room - huella de contenido por idioma
    @Test
    fun `given content hashes of two locales when querying seedVersionFor then each is independent`() = runTest {
        // Given
        val dao = inMemoryCatalogDatabase().catalogDao()
        dao.upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "pt-PT", contentHash = "pt-hash"))
        dao.upsertSeedVersion(CatalogSeedVersionEntity(targetLocale = "es-ES", contentHash = "es-hash"))

        // When / Then
        dao.seedVersionFor("pt-PT")?.contentHash shouldBe "pt-hash"
        dao.seedVersionFor("es-ES")?.contentHash shouldBe "es-hash"
    }
}
