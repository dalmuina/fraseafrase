package com.dalmuina.fraseafrase.core.data.catalog

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AssetsCatalogDataSourceTest {

    private val document = """
        {
          "schemaVersion": 1,
          "targetLocale": "pt-PT",
          "sourceLocale": "es-ES",
          "phrases": [
            { "id": "a1-saludos-001", "level": "A1", "topic": "saludos", "target": "Bom dia", "source": "Buenos días" },
            { "id": "a1-comida-001", "level": "A1", "topic": "comida", "target": "Tenho fome", "source": "Tengo hambre" }
          ]
        }
    """.trimIndent()

    // Scenario: Formato del catálogo (spec: phrase-catalog) — las frases conservan el orden de catálogo
    @Test
    fun `given a readable asset when loading then returns the phrases in catalog order`() = runTest {
        // Given
        val source =
            AssetsCatalogDataSource(readAsset = { document }, dispatcher = UnconfinedTestDispatcher(testScheduler))

        // When
        val result = source.phrases()

        // Then
        result.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
        result.phrases.map { it.id.value } shouldBe listOf("a1-saludos-001", "a1-comida-001")
    }

    // Scenario: Asset no legible falla la carga (spec: phrase-catalog)
    @Test
    fun `given an asset that cannot be read when loading then fails with AssetUnreadable and no phrases`() = runTest {
        // Given
        val source = AssetsCatalogDataSource(
            readAsset = { throw IOException("asset missing") },
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        // When
        val result = source.phrases()

        // Then
        result shouldBe CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)
    }

    // Memoización (design D9): la segunda llamada reutiliza la carga y no vuelve a leer el asset
    @Test
    fun `given a loaded catalog when loading twice then the asset is read only once`() = runTest {
        // Given
        var reads = 0
        val source = AssetsCatalogDataSource(
            readAsset = {
                reads++
                document
            },
            dispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        // When
        val first = source.phrases()
        val second = source.phrases()

        // Then
        reads shouldBe 1
        second shouldBe first
        second.shouldBeInstanceOf<CatalogLoadResult.Loaded>()
    }

    // Memoización (design D9): un fallo de lectura también se guarda y no se reintenta
    @Test
    fun `given an unreadable asset when loading twice then the failure is memoized and the asset is read only once`() =
        runTest {
            // Given
            var reads = 0
            val source = AssetsCatalogDataSource(
                readAsset = {
                    reads++
                    throw IOException("asset missing")
                },
                dispatcher = UnconfinedTestDispatcher(testScheduler),
            )

            // When
            val first = source.phrases()
            val second = source.phrases()

            // Then
            reads shouldBe 1
            first shouldBe CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)
            second shouldBe first
        }
}
