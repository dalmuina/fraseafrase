package com.dalmuina.fraseafrase.core.data.catalog

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

// Contrato sobre el catálogo real empaquetado (ADR-0013, ADR-0019), ejecutado por idioma destino: la permanencia
// de ids de uno es independiente de la de cualquier otro.
class RealCatalogContractTest {

    private companion object {
        val PACKAGED_LOCALES = listOf("pt-PT", "pt-BR", "es-ES", "it-IT", "en-GB", "de-DE", "fr-FR", "ru-RU")
    }

    private fun catalogTextOf(targetLocale: String) =
        File("src/main/assets/catalog/phrases-$targetLocale.json").readText()

    private fun releasedIdsOf(targetLocale: String) =
        File("src/test/resources/catalog/released-ids-$targetLocale.txt").readLines().filter(String::isNotBlank)

    private fun currentIdsOf(targetLocale: String) =
        (CatalogJson.parsePhrases(catalogTextOf(targetLocale)) as CatalogParseResult.PhrasesParsed)
            .phrases.map { it.id.value }

    // Scenario: Versión del esquema (spec: phrase-catalog) — cada idioma destino empaquetado carga por separado
    @ParameterizedTest
    @ValueSource(strings = ["pt-PT", "pt-BR", "es-ES", "it-IT", "en-GB", "de-DE", "fr-FR", "ru-RU"])
    fun `given the packaged catalog of a target locale when parsing then it is loaded`(targetLocale: String) {
        // When
        val result = CatalogJson.parsePhrases(catalogTextOf(targetLocale))

        // Then
        result.shouldBeInstanceOf<CatalogParseResult.PhrasesParsed>()
    }

    // Scenario: Ids paralelos entre idiomas (spec: phrase-catalog) - todos los documentos empaquetados
    @Test
    fun `given all packaged phrase documents when checking parallel ids then every language has the same ids`() {
        // Given
        val docs = PACKAGED_LOCALES.associateWith(::currentIdsOf)

        // When / Then
        CatalogContract.parallelIdMismatches(docs).shouldBeEmpty()
    }

    // Scenario: El catálogo empaquetado no contiene documentos de traducción (spec: phrase-catalog)
    @Test
    fun `given the packaged catalog assets when checking their roots then none has a source locale`() {
        // Given
        val assets = File("src/main/assets/catalog").listFiles { f -> f.extension == "json" }.orEmpty()

        // When
        val withSourceLocale = assets.filter { "\"sourceLocale\"" in it.readText() }.map(File::getName)

        // Then
        assets.isNotEmpty() shouldBe true
        withSourceLocale.shouldBeEmpty()
    }

    // Scenario: Ids publicados siguen presentes y en el mismo orden (spec: phrase-catalog)
    @Test
    fun `given pt-PT released ids when checking the packaged catalog then the prefix is kept`() {
        // Given
        val currentIds = currentIdsOf("pt-PT")
        val releasedIds = releasedIdsOf("pt-PT")

        // When / Then
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given pt-BR with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("pt-BR")
        val releasedIds = releasedIdsOf("pt-BR")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given es-ES with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("es-ES")
        val releasedIds = releasedIdsOf("es-ES")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given it-IT with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("it-IT")
        val releasedIds = releasedIdsOf("it-IT")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given en-GB with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("en-GB")
        val releasedIds = releasedIdsOf("en-GB")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given de-DE with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("de-DE")
        val releasedIds = releasedIdsOf("de-DE")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given fr-FR with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("fr-FR")
        val releasedIds = releasedIdsOf("fr-FR")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }

    // Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga (spec: phrase-catalog)
    @Test
    fun `given ru-RU with no released ids yet when checking the contract then it passes without requiring any id`() {
        // Given
        val currentIds = currentIdsOf("ru-RU")
        val releasedIds = releasedIdsOf("ru-RU")

        // When / Then
        releasedIds.shouldBeEmpty()
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }
}
