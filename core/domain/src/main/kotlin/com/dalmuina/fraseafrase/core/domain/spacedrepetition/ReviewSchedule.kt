package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import java.time.LocalDate

/**
 * Cálculo del próximo repaso según el nivel (ADR-0012). Los intervalos se cuentan en días naturales
 * y dependen solo de la fecha recibida, nunca del reloj del sistema.
 */
object ReviewSchedule {

    // Días de intervalo por nivel. El nivel 0 no tiene intervalo: no aparece en el mapa.
    private val INTERVALS_DAYS: Map<Int, Long> = mapOf(1 to 1L, 2 to 3L, 3 to 7L, 4 to 14L, 5 to 30L)

    /**
     * Devuelve la fecha [from] más el intervalo del [level], o `null` si el nivel es 0.
     */
    fun nextReviewAfter(level: Level, from: LocalDate): LocalDate? {
        val days = INTERVALS_DAYS[level.value] ?: return null
        return from.plusDays(days)
    }
}
