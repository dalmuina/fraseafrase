package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
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

    // Scenario: Documentos con los mismos ids cumplen el contrato (spec: phrase-catalog, Ids paralelos)
    @Test
    fun `given two documents with the same ids when checking parallel ids then there are no mismatches`() {
        // Given
        val docs = mapOf("pt-PT" to listOf("A", "B", "C"), "es-ES" to listOf("A", "B", "C"))

        // When / Then
        CatalogContract.parallelIdMismatches(docs).shouldBeEmpty()
    }

    // Scenario: Un documento al que le falta un id incumple el contrato (spec: phrase-catalog, Ids paralelos)
    @Test
    fun `given a document missing an id when checking parallel ids then it reports the missing id`() {
        // Given
        val docs = mapOf("pt-PT" to listOf("A", "B", "C"), "es-ES" to listOf("A", "B"))

        // When
        val mismatches = CatalogContract.parallelIdMismatches(docs)

        // Then
        mismatches["es-ES"]?.missing shouldBe listOf("C")
        mismatches["es-ES"]?.extra shouldBe emptyList()
    }

    // Scenario: Un documento con un id de más incumple el contrato (spec: phrase-catalog, Ids paralelos)
    @Test
    fun `given a document with an extra id when checking parallel ids then it reports the extra id`() {
        // Given
        val docs = mapOf("pt-PT" to listOf("A", "B"), "es-ES" to listOf("A", "B", "D"))

        // When
        val mismatches = CatalogContract.parallelIdMismatches(docs)

        // Then
        mismatches["es-ES"]?.extra shouldBe listOf("D")
        mismatches["es-ES"]?.missing shouldBe emptyList()
    }

    // Scenario: Un id con formato válido cumple el contrato (spec: phrase-catalog, Formato de los ids)
    @Test
    fun `given a well formed id when checking the id format then there are no violations`() {
        formatViolationsOf("0000-a1-saludos-001").shouldBeEmpty()
    }

    // Scenario: Un id sin bloque incumple el contrato (spec: phrase-catalog, Formato de los ids)
    @Test
    fun `given an id without block when checking the id format then it is reported as malformed`() {
        formatViolationsOf("a1-saludos-001") shouldContainExactly
            listOf(IdViolation("a1-saludos-001", IdViolationReason.MALFORMED))
    }

    // Scenario: Un id con número 000 incumple el contrato (spec: phrase-catalog, Formato de los ids)
    @Test
    fun `given an id numbered 000 when checking the id format then it is reported as malformed`() {
        formatViolationsOf("0000-a1-saludos-000") shouldContainExactly
            listOf(IdViolation("0000-a1-saludos-000", IdViolationReason.MALFORMED))
    }

    // Scenario: Un bloque fuera del registro incumple el contrato (spec: phrase-catalog, Formato de los ids)
    @Test
    fun `given an unregistered block when checking the id format then it is reported as unknown block`() {
        formatViolationsOf("1000-a1-saludos-001") shouldContainExactly
            listOf(IdViolation("1000-a1-saludos-001", IdViolationReason.UNKNOWN_BLOCK))
    }

    // Scenario: Un tema fuera de la lista incumple el contrato (spec: phrase-catalog, Formato de los ids)
    @Test
    fun `given an unregistered topic when checking the id format then it is reported as unknown topic`() {
        formatViolationsOf("0000-a1-viajes-001") shouldContainExactly
            listOf(IdViolation("0000-a1-viajes-001", IdViolationReason.UNKNOWN_TOPIC))
    }

    // Scenario: Una frase nueva cuyo nivel no coincide con su id incumple el contrato (spec: phrase-catalog)
    @Test
    fun `given an unreleased phrase with another level than its id when checking birth labels then it is reported`() {
        // Given
        val phrase = targetPhrase("0000-a1-saludos-003", CefrLevel.A2, "saludos")

        // When / Then
        CatalogContract.birthLabelMismatches(listOf(phrase), released = emptySet()) shouldContainExactly
            listOf("0000-a1-saludos-003")
    }

    // Scenario: Una frase nueva cuyo tema no coincide con su id incumple el contrato (spec: phrase-catalog)
    @Test
    fun `given an unreleased phrase with another topic than its id when checking birth labels then it is reported`() {
        // Given
        val phrase = targetPhrase("0000-a1-saludos-003", CefrLevel.A1, "comida")

        // When / Then
        CatalogContract.birthLabelMismatches(listOf(phrase), released = emptySet()) shouldContainExactly
            listOf("0000-a1-saludos-003")
    }

    // Scenario: Una frase publicada reclasificada conserva su id (spec: phrase-catalog)
    @Test
    fun `given a released phrase reclassified when checking birth labels then it is not reported`() {
        // Given
        val phrase = targetPhrase("0000-a1-saludos-001", CefrLevel.A2, "comida")

        // When / Then
        CatalogContract.birthLabelMismatches(listOf(phrase), released = setOf("0000-a1-saludos-001")).shouldBeEmpty()
    }

    private fun formatViolationsOf(id: String) =
        CatalogContract.idFormatViolations(listOf(id), blocks = setOf("0000"), topics = setOf("saludos"))

    private fun targetPhrase(id: String, level: CefrLevel, topic: String) =
        TargetPhrase(PhraseId(id), level, Topic(topic), LanguageTag("pt-PT"), target = "Bom dia")
}
