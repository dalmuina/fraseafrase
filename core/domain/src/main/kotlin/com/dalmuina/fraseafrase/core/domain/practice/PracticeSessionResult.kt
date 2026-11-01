package com.dalmuina.fraseafrase.core.domain.practice

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak

/**
 * Resultado de leer la sesión de práctica: la tanda con su texto, o el motivo por el que no se puede practicar.
 * Todos los casos son esperables: ninguno lanza excepción.
 */
sealed interface PracticeSessionResult {

    /**
     * Tanda activa con su texto, en el orden de la tanda. [uncertifiedCount] es el número de frases no certificadas
     * que pasarían a la siguiente tanda si se cerrara ahora (aviso previo).
     */
    data class Ready(val items: List<PracticeItem>, val uncertifiedCount: Int) : PracticeSessionResult

    /** No hay arrastradas, pendientes ni frases nuevas: no hay nada que practicar (ADR-0012). */
    data object NothingToPractice : PracticeSessionResult

    /**
     * El catálogo no se puede cargar, así que no se puede mostrar ninguna frase. Se llama distinto de
     * `ActiveBatchResult.CatalogUnavailable` a propósito, para no confundirlos en los `when`.
     */
    data class CatalogFailed(val error: CatalogLoadError) : PracticeSessionResult
}

/**
 * Frase de la tanda lista para mostrar: textos en el idioma de origen y de destino, racha dentro de la tanda y si
 * está marcada como sabida. [isKnown] es `streak >= 3` dentro de la tanda; no se llama `isCertified` para no
 * confundirlo con la certificación al cerrar (ADR-0012), que es la de `PhraseProgress`.
 */
data class PracticeItem(
    val id: PhraseId,
    val source: String,
    val target: String,
    val streak: Streak,
    val isKnown: Boolean,
) {

    companion object {
        /** Racha a partir de la cual una frase de la tanda se marca como sabida (spec `practice-session`). */
        const val KNOWN_STREAK = 3
    }
}
