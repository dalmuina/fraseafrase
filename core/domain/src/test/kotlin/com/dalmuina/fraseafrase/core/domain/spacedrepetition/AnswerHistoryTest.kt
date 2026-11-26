package com.dalmuina.fraseafrase.core.domain.spacedrepetition

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

// Spec: spaced-repetition, "Invariantes del registro de respuestas"
class AnswerHistoryTest {

    // Scenario: Sesión respondida negativa se rechaza
    @Test
    fun `given a negative last answered session when building the history then it is rejected`() {
        shouldThrow<IllegalArgumentException> {
            AnswerHistory(lastAnsweredSession = -1, consecutiveSessions = 1, lastMissSession = null)
        }
    }

    // Scenario: Sesión respondida con cero sesiones seguidas se rechaza
    @Test
    fun `given zero consecutive sessions when building the history then it is rejected`() {
        shouldThrow<IllegalArgumentException> {
            AnswerHistory(lastAnsweredSession = 4, consecutiveSessions = 0, lastMissSession = null)
        }
    }

    // Scenario: Sesión fallada posterior a la respondida se rechaza
    @Test
    fun `given a last miss after the last answered session when building the history then it is rejected`() {
        shouldThrow<IllegalArgumentException> {
            AnswerHistory(lastAnsweredSession = 4, consecutiveSessions = 1, lastMissSession = 5)
        }
    }

    // "Alguna de sus sesiones es negativa" also covers the last missed session
    @Test
    fun `given a negative last miss session when building the history then it is rejected`() {
        shouldThrow<IllegalArgumentException> {
            AnswerHistory(lastAnsweredSession = 4, consecutiveSessions = 1, lastMissSession = -1)
        }
    }

    // Scenario: Registro coherente se acepta
    @Test
    fun `given a coherent history when building it then it is accepted`() {
        shouldNotThrowAny {
            AnswerHistory(lastAnsweredSession = 0, consecutiveSessions = 1, lastMissSession = 0)
        }
    }
}
