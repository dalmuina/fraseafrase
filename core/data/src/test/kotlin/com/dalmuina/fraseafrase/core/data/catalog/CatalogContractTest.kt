package com.dalmuina.fraseafrase.core.data.catalog

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// Reglas de permanencia de ids (spec phrase-catalog, ADR-0013): ningún id publicado se retira ni cambia de orden.
class CatalogContractTest {

    // Scenario: Alta de frase nueva al final (spec: phrase-catalog)
    @Test
    fun `given released ids A and B when a new phrase C is appended then the released ids keep their order`() {
        // Given
        val released = listOf("A", "B")
        val current = listOf("A", "B", "C")

        // When / Then
        CatalogContract.retiredIds(released, current).shouldBeEmpty()
        CatalogContract.keepsReleasedPrefix(released, current) shouldBe true
    }

    // Scenario: Ids publicados siguen presentes y en el mismo orden (spec: phrase-catalog)
    @Test
    fun `given released ids A and B when the catalog reorders them then the prefix rule fails`() {
        // Given
        val released = listOf("A", "B")
        val reordered = listOf("B", "A", "C")

        // When / Then
        CatalogContract.keepsReleasedPrefix(released, reordered) shouldBe false
    }

    // Scenario: Catálogo que quita un id publicado devuelve el id retirado (spec: phrase-catalog)
    @Test
    fun `given released ids A and B when the catalog drops B then returns B as retired`() {
        // Given
        val released = listOf("A", "B")
        val current = listOf("A")

        // When
        val retired = CatalogContract.retiredIds(released, current)

        // Then
        retired shouldContainExactly listOf("B")
    }
}
