package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/**
 * Resultado de leer la sesión de práctica: la tanda con su texto, o el motivo por el que no se puede practicar.
 * Todos los casos son esperables: ninguno lanza excepción.
 */
sealed interface PracticeSessionResult {

    /**
     * Tanda activa con su texto, en el orden de la tanda. [isComplete] es si el usuario ya ha dado una vuelta
     * completa a la lista circular, es decir, si todas sus frases están respondidas (spec `practice-session`,
     * "Mensaje de tanda completada y generación de sesión"). Calcularlo no modifica la tanda, sus rachas ni el
     * progreso.
     */
    data class Ready(
        val items: List<PracticeItem>,
        val isComplete: Boolean,
    ) : PracticeSessionResult

    /** No hay arrastradas, pendientes ni frases nuevas: no hay nada que practicar (ADR-0012). */
    data object NothingToPractice : PracticeSessionResult

    /**
     * El catálogo no se puede cargar, así que no se puede mostrar ninguna frase. Se llama distinto de
     * `ActiveBatchResult.CatalogUnavailable` a propósito, para no confundirlos en los `when`.
     */
    data class CatalogFailed(val error: CatalogLoadError) : PracticeSessionResult
}

/**
 * Frase de la tanda lista para mostrar: textos en el idioma de origen y de destino, racha dentro de la tanda y
 * nivel de consolidación.
 */
data class PracticeItem(
    val id: PhraseId,
    val source: String,
    val target: String,
    val targetLocale: LanguageTag,
    val cefrLevel: CefrLevel,
    val streak: Streak,
    val level: Level,
)
