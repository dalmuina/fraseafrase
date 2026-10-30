package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Racha de aciertos consecutivos de una frase dentro de la tanda (ADR-0012).
 */
@JvmInline
value class Streak(val value: Int) {

    init {
        require(value >= 0) { "La racha no puede ser negativa, recibido $value" }
    }

    /** Un acierto suma 1 a la racha. */
    fun afterHit(): Streak = Streak(value + 1)

    /** Un fallo reinicia la racha a 0. */
    fun afterMiss(): Streak = Streak(0)
}
