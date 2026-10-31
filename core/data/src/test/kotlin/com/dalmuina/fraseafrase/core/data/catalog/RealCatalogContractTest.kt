package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.io.File

// Contrato sobre el catálogo real empaquetado (tarea 6.4). Los tests de este fichero leen los ficheros del módulo.
class RealCatalogContractTest {

    private val catalogText = File("src/main/assets/catalog/phrases.json").readText()
    private val releasedIds = File("src/test/resources/catalog/released-ids.txt").readLines().filter(String::isNotBlank)

    @Test
    fun `given the packaged catalog when parsing then it is loaded`() {
        // When
        val result = CatalogJson.parse(catalogText)

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
    }

    // Scenario: Ids únicos y no vacíos (spec: phrase-catalog)
    @Test
    fun `given the packaged catalog when checking ids then no released id is retired and the prefix is kept`() {
        // Given
        val currentIds = (CatalogJson.parse(catalogText) as CatalogLoadResult.Loaded).phrases.map { it.id.value }

        // When / Then
        CatalogContract.retiredIds(releasedIds, currentIds).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(releasedIds, currentIds) shouldBe true
    }
}
