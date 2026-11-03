package com.dalmuina.fraseafrase.core.domain.audio

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import javax.inject.Inject

/**
 * Reproduce el audio de una frase en el locale dado (ADR-0006). Envoltorio directo sobre [PhrasePlayer]
 * para que la presentación dependa de un caso de uso, no del puerto.
 */
class PlayPhraseAudio @Inject constructor(
    private val player: PhrasePlayer,
) {

    operator fun invoke(text: String, locale: LanguageTag) = player.play(text, locale)
}
