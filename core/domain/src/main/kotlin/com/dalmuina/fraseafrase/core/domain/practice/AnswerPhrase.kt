package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import javax.inject.Inject

/**
 * Registra la autoevaluación de una frase de la tanda activa y la mueve al final de la lista (ADR-0012).
 * Responder sin tanda activa, o una frase que no está en ella, es un error de programación: lanza
 * [IllegalArgumentException] desde la invariante de la tanda.
 */
class AnswerPhrase @Inject constructor(
    private val practiceRepository: PracticeRepository,
) {

    suspend operator fun invoke(id: PhraseId, assessment: SelfAssessment) {
        practiceRepository.transact { snapshot ->
            val batch = requireNotNull(snapshot.batch()) { "There is no active batch to answer" }
            PracticeChange(snapshot.storing(batch.answer(id, assessment)), Unit)
        }
    }
}
