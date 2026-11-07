package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Cálculo del enfriamiento tras certificar una frase (ADR-0012, ADR-0017). Los intervalos se cuentan
 * en sesiones generadas y dependen solo del nivel alcanzado: es una función pura, sin fecha ni reloj.
 */
object ReviewSchedule {

    // Sesiones de enfriamiento por nivel alcanzado. El nivel 0 nunca se alcanza al certificar.
    private val COOLDOWN_SESSIONS: Map<Int, Int> = mapOf(1 to 1, 2 to 2, 3 to 4, 4 to 8, 5 to 16)

    /**
     * Devuelve el número de sesiones de enfriamiento para el [level] alcanzado al certificar.
     * Solo está definido para los niveles 1 a 5: certificar siempre sube a uno de ellos.
     */
    fun cooldownSessionsFor(level: Level): Int = requireNotNull(COOLDOWN_SESSIONS[level.value]) {
        "No cooldown is defined for level ${level.value}"
    }
}
