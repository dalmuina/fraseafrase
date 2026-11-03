package com.dalmuina.fraseafrase.core.domain.audio

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.testing.FakePhrasePlayer
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PlayPhraseAudioTest {

    // Scenario: given a text and a locale when invoked then the player receives them (design.md - Decisiones)
    @Test
    fun `given a text and a locale when invoked then the player receives them`() {
        // Given
        val player = FakePhrasePlayer()
        val useCase = PlayPhraseAudio(player)

        // When
        useCase("Bom dia", LanguageTag("pt-PT"))

        // Then
        player.calls shouldBe listOf(FakePhrasePlayer.Call("Bom dia", LanguageTag("pt-PT")))
    }
}
