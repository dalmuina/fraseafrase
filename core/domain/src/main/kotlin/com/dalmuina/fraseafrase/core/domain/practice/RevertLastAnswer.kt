package com.dalmuina.fraseafrase.core.domain.practice

import javax.inject.Inject

/**
 * Revierte la autoevaluación más reciente de toda la tanda activa (design D2), descrita por [UndoableAnswer]
 * (devuelto por [AnswerPhrase]): restaura su nivel, racha y `dueAtSession` y la devuelve a la posición que
 * ocupaba antes de autoevaluarla. Revertir una autoevaluación que ya no es la más reciente de la tanda (porque
 * se autoevaluó otra frase después, o porque ya se revirtió) es un error de programación: lanza
 * [IllegalArgumentException] desde la invariante de [com.dalmuina.fraseafrase.core.domain.spacedrepetition
 * .PracticeBatch.revertLastAnswer].
 */
class RevertLastAnswer @Inject constructor(
    private val practiceRepository: PracticeRepository,
) {

    suspend operator fun invoke(undo: UndoableAnswer) {
        practiceRepository.transact { snapshot ->
            val batch = requireNotNull(snapshot.batch()) { "There is no active batch to revert" }
            val revertedBatch = batch.revertLastAnswer(undo.phraseId, undo.progressBefore)
            PracticeChange(snapshot.storing(revertedBatch), Unit)
        }
    }
}
