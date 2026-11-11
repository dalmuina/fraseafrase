package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import javax.inject.Inject

/**
 * Registra la autoevaluación de una frase de la tanda activa y la mueve al final de la lista (ADR-0012,
 * ADR-0017). Devuelve el [AnswerOutcome] con el nivel resultante de la frase respondida y el
 * [UndoableAnswer] necesario para revertirla con `RevertLastAnswer`. Responder sin tanda activa, o una
 * frase que no está en ella, es un error de programación: lanza [IllegalArgumentException] desde la
 * invariante de la tanda.
 */
class AnswerPhrase @Inject constructor(
    private val practiceRepository: PracticeRepository,
) {

    suspend operator fun invoke(id: PhraseId, assessment: SelfAssessment): AnswerOutcome {
        return practiceRepository.transact { snapshot ->
            val batch = requireNotNull(snapshot.batch()) { "There is no active batch to answer" }
            val answeredBatch = batch.answer(id, assessment, snapshot.sessionCount)
            val progressBefore = batch.entries.first { it.id == id }
            val resultingLevel = answeredBatch.entries.first { it.id == id }.level
            val outcome = AnswerOutcome(resultingLevel, UndoableAnswer(id, progressBefore))
            PracticeChange(snapshot.storing(answeredBatch), outcome)
        }
    }
}

/**
 * Lo necesario para revertir una autoevaluación con [RevertLastAnswer]: la frase afectada y su
 * [PhraseProgress] inmediatamente antes de autoevaluarla (design D1). Nunca se persiste: vive solo en
 * memoria mientras sigue siendo la autoevaluación más reciente de la tanda.
 */
data class UndoableAnswer(val phraseId: PhraseId, val progressBefore: PhraseProgress)

/** Resultado de [AnswerPhrase]: el [Level] resultante de la frase respondida y su [UndoableAnswer]. */
data class AnswerOutcome(val resultingLevel: Level, val undo: UndoableAnswer)
