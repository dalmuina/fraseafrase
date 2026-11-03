package com.dalmuina.fraseafrase.core.platform

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val LANG_MISSING_DATA = -1
private const val LANG_NOT_SUPPORTED = -2

class TextToSpeechPlayerTest {

    // Scenario: Motor de TTS no disponible
    @Test
    fun `given isReady returns false when play is called then speak is never invoked`() {
        // Given
        val spoken = mutableListOf<String>()
        val player = TextToSpeechPlayer(
            isReady = { false },
            setLanguage = { 0 },
            speak = { text -> spoken.add(text) },
        )

        // When
        player.play("Bom dia", LanguageTag("pt-PT"))

        // Then
        spoken shouldBe emptyList()
    }

    // Scenario: Locale de la frase sin voz instalada
    @Test
    fun `given setLanguage returns an unsupported locale result when play is called then speak is never invoked`() {
        // Given
        val spoken = mutableListOf<String>()
        val missingDataPlayer = TextToSpeechPlayer(
            isReady = { true },
            setLanguage = { LANG_MISSING_DATA },
            speak = { text -> spoken.add(text) },
        )
        val notSupportedPlayer = TextToSpeechPlayer(
            isReady = { true },
            setLanguage = { LANG_NOT_SUPPORTED },
            speak = { text -> spoken.add(text) },
        )

        // When
        missingDataPlayer.play("Bom dia", LanguageTag("pt-PT"))
        notSupportedPlayer.play("Bom dia", LanguageTag("pt-PT"))

        // Then
        spoken shouldBe emptyList()
    }

    // Scenario: Escuchar dos veces reinicia la reproducción
    // El modo de cola real (QUEUE_FLUSH) vive en el lambda de producción de TextToSpeechPlayer.Engine y
    // solo se puede verificar con el motor real (design.md - Risks / Trade-offs); este test solo cubre
    // que play() vuelve a invocar speak() en cada llamada, no que la reproducción se reinicia.
    @Test
    fun `given play is called twice when the second call runs then speak is invoked again`() {
        // Given
        val spoken = mutableListOf<String>()
        val player = TextToSpeechPlayer(
            isReady = { true },
            setLanguage = { 0 },
            speak = { text -> spoken.add(text) },
        )

        // When
        player.play("Bom dia", LanguageTag("pt-PT"))
        player.play("Bom dia", LanguageTag("pt-PT"))

        // Then
        spoken shouldBe listOf("Bom dia", "Bom dia")
    }
}
