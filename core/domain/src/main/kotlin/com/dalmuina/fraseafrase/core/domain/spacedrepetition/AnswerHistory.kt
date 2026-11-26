package com.dalmuina.fraseafrase.core.domain.spacedrepetition

/**
 * Registro de respuestas de una frase (ADR-0027): última sesión respondida, sesiones generadas seguidas en
 * las que se respondió y última sesión fallada. Una frase nunca respondida no tiene registro.
 *
 * Un estado incoherente es un error de programación, por eso se lanza [IllegalArgumentException].
 */
data class AnswerHistory(
    val lastAnsweredSession: Int,
    val consecutiveSessions: Int,
    val lastMissSession: Int?,
) {

    init {
        require(lastAnsweredSession >= 0) {
            "lastAnsweredSession must not be negative, received $lastAnsweredSession"
        }
        require(consecutiveSessions >= 1) {
            "consecutiveSessions must be at least 1, received $consecutiveSessions"
        }
        require(lastMissSession == null || lastMissSession in 0..lastAnsweredSession) {
            "lastMissSession must be between 0 and $lastAnsweredSession, received $lastMissSession"
        }
    }
}

/**
 * Registro tras autoevaluar la frase en la sesión generada [session] (ADR-0027): las sesiones seguidas
 * suben en 1 si se respondió en la anterior, no cambian si ya se había respondido en esta y vuelven a 1 en
 * otro caso. Un fallo ([missed]) fija la última sesión fallada; un acierto la conserva.
 */
internal fun AnswerHistory?.afterAnswer(session: Int, missed: Boolean): AnswerHistory = AnswerHistory(
    lastAnsweredSession = session,
    consecutiveSessions = when (this?.lastAnsweredSession) {
        session -> consecutiveSessions
        session - 1 -> consecutiveSessions + 1
        else -> 1
    },
    lastMissSession = if (missed) session else this?.lastMissSession,
)
