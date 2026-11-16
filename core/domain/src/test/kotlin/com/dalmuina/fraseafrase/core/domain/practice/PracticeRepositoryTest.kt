package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.testing.FakePracticeRepository
import com.dalmuina.fraseafrase.core.testing.aPhraseProgress
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

// El progreso de dos idiomas destino para la misma frase no se mezcla (spec: practice-persistence)
class PracticeRepositoryTest {

    private val ptPt = LanguageTag("pt-PT")
    private val ptBr = LanguageTag("pt-BR")

    // Scenario: El progreso de dos idiomas destino para la misma frase no se mezcla
    @Test
    fun `given progress saved for two target locales when reading each then they do not share state`() = runTest {
        // Given
        val practice = FakePracticeRepository()
        practice.transact(ptPt) { snapshot ->
            PracticeChange(snapshot.copy(progress = listOf(aPhraseProgress(id = "a1-saludos-001"))), Unit)
        }
        practice.transact(ptBr) { snapshot ->
            PracticeChange(snapshot.copy(sessionCount = 7), Unit)
        }

        // When
        val ptPtSnapshot = practice.read(ptPt)
        val ptBrSnapshot = practice.read(ptBr)

        // Then
        ptPtSnapshot.progress.map { it.id } shouldBe listOf(PhraseId("a1-saludos-001"))
        ptPtSnapshot.sessionCount shouldBe 0
        ptBrSnapshot.progress shouldBe emptyList()
        ptBrSnapshot.sessionCount shouldBe 7
    }

    // Scenario: La misma posición es válida en idiomas destino distintos
    @Test
    fun `given a batch position used in one target locale when used in another then it does not clash`() = runTest {
        // Given
        val practice = FakePracticeRepository()

        // When
        practice.transact(ptPt) { snapshot ->
            PracticeChange(
                snapshot.copy(
                    progress = listOf(aPhraseProgress(id = "A")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10)),
                ),
                Unit,
            )
        }
        practice.transact(ptBr) { snapshot ->
            PracticeChange(
                snapshot.copy(
                    progress = listOf(aPhraseProgress(id = "A")),
                    activeBatch = ActiveBatch(order = listOf(PhraseId("A")), capacity = BatchSize(10)),
                ),
                Unit,
            )
        }

        // Then
        practice.read(ptPt).activeBatch?.order shouldBe listOf(PhraseId("A"))
        practice.read(ptBr).activeBatch?.order shouldBe listOf(PhraseId("A"))
    }
}
