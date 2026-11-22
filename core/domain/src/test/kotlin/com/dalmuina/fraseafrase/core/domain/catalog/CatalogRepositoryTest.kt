package com.dalmuina.fraseafrase.core.domain.catalog

import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import com.dalmuina.fraseafrase.core.testing.aCatalogPhrase
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// Idiomas destino disponibles (spec: phrase-catalog)
class CatalogRepositoryTest {

    private val ptPt = LanguageTag("pt-PT")
    private val ptBr = LanguageTag("pt-BR")

    // Scenario: Dos idiomas destino cargados correctamente están disponibles
    @Test
    fun `given two valid target locale catalogs when listing available target locales then both are listed`() =
        runTest {
            // Given
            val repository = FakeCatalogRepository.withCatalogs(
                mapOf(
                    ptPt to CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "a"))),
                    ptBr to CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "b"))),
                ),
            )

            // When
            val result = repository.availableTargetLocales()

            // Then
            result shouldContainExactlyInAnyOrder listOf(ptPt, ptBr)
        }

    // Scenario: Un idioma destino con catálogo inválido no está disponible
    @Test
    fun `given one invalid target locale catalog when listing available target locales then only valid is listed`() =
        runTest {
            // Given
            val repository = FakeCatalogRepository.withCatalogs(
                mapOf(
                    ptPt to CatalogLoadResult.Loaded(listOf(aCatalogPhrase(id = "a"))),
                    ptBr to CatalogLoadResult.Failed(CatalogLoadError.MalformedDocument),
                ),
            )

            // When
            val result = repository.availableTargetLocales()

            // Then
            result shouldBe listOf(ptPt)
        }
}
