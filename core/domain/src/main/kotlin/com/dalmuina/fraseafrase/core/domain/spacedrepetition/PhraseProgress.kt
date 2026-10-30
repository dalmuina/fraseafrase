package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import java.time.LocalDate

/**
 * Frase dentro de la tanda activa (ADR-0012). Inmutable y con invariantes validadas al construirla:
 * - un nivel 0 no tiene próximo repaso, y un nivel de 1 a 5 sí lo tiene;
 * - una racha mayor que 0 exige que la frase haya sido respondida en la tanda.
 *
 * Un estado incoherente es un error de programación, por eso se lanza [IllegalArgumentException].
 */
data class PhraseProgress(
    val id: PhraseId,
    val level: Level,
    val streak: Streak,
    val answeredInBatch: Boolean,
    val nextReview: LocalDate?,
) {

    init {
        require(level.value > 0 || nextReview == null) {
            "Una frase de nivel 0 no puede tener próximo repaso"
        }
        require(level.value == 0 || nextReview != null) {
            "Una frase de nivel ${level.value} debe tener próximo repaso"
        }
        require(streak.value == 0 || answeredInBatch) {
            "Una racha mayor que 0 exige que la frase haya sido respondida en la tanda"
        }
    }

    /** Certificada si su racha alcanza [CERTIFICATION_STREAK] al cerrar la tanda. */
    val isCertified: Boolean
        get() = streak.value >= CERTIFICATION_STREAK

    companion object {
        /** Racha mínima dentro de la tanda para certificar una frase (ADR-0012). */
        const val CERTIFICATION_STREAK = 3
    }
}
