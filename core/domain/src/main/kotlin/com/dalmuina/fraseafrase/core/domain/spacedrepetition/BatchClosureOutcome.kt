package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Resultado de cerrar la tanda para una frase (ADR-0012). Toda variante deja la frase con racha 0 y
 * sin respuesta en la tanda, así que el cierre siempre devuelve el progreso ya resuelto.
 */
sealed interface BatchClosureOutcome {

    val progress: PhraseProgress

    /** Frase certificada: sube de nivel con repaso programado y sale de la tanda. */
    data class Certified(override val progress: PhraseProgress) : BatchClosureOutcome

    /** Frase arrastrada a la siguiente tanda. */
    data class Carried(override val progress: PhraseProgress) : BatchClosureOutcome
}
