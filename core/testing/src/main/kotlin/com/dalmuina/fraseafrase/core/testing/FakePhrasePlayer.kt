package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.audio.PhrasePlayer
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/** Registra cada llamada a [play] para que un test pueda comprobar el texto y el locale recibidos. */
class FakePhrasePlayer : PhrasePlayer {

    private val _calls = mutableListOf<Call>()
    val calls: List<Call> get() = _calls

    override fun play(text: String, locale: LanguageTag) {
        _calls += Call(text, locale)
    }

    data class Call(val text: String, val locale: LanguageTag)
}
