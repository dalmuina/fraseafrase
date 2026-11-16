package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

// Contrato sobre el catálogo real empaquetado (ADR-0013, ADR-0019), ejecutado por idioma destino: la permanencia
// de ids de uno es independiente de la de cualquier otro.
class RealCatalogContractTest {

    private fun catalogTextOf(targetLocale: String) =
        File("src/main/assets/catalog/phrases-$targetLocale.json").readText()

    private fun releasedIdsOf(targetLocale: String) =
        File("src/test/resources/catalog/released-ids-$targetLocale.txt").readLines().filter(String::isNotBlank)

    private fun currentIdsOf(targetLocale: String) =
        (CatalogJson.parse(catalogTextOf(targetLocale)) as CatalogLoadResult.Loaded).phrases.map { it.id.value }

    // Scenario: Versión del esquema (spec: phrase-catalog) — cada idioma destino empaquetado carga por separado
    @ParameterizedTest
    @ValueSource(strings = ["pt-PT", "pt-BR"])
    fun `given the packaged catalog of a target locale when parsing then it is loaded`(targetLocale: String) {
        // When
        val result = CatalogJson.parse(catalogTextOf(targetLocale))

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
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
}
