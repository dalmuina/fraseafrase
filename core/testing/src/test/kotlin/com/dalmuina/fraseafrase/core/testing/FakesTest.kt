package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FakesTest {

    @Test
    fun `given an empty fake repository when transacting then the change is stored`() = runTest {
        // Given
        val fake = FakePracticeRepository()
        val carried = PhraseId("a")

        // When
        val result = fake.transact { snapshot ->
            PracticeChange(snapshot.copy(carry = CarryQueue(listOf(carried))), "done")
        }

        // Then
        result shouldBe "done"
        fake.read().carry.ids shouldBe listOf(carried)
    }

    @Test
    fun `given a fake catalog repository when loading then returns the configured phrases`() = runTest {
        // Given
        val phrases = listOf(aCatalogPhrase(id = "a1-1"))
        val fake = FakeCatalogRepository(CatalogLoadResult.Loaded(phrases))

        // When
        val loaded = fake.phrases()

        // Then
        loaded shouldBe CatalogLoadResult.Loaded(phrases)
    }
}
