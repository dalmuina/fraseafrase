package com.dalmuina.fraseafrase.core.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import com.dalmuina.fraseafrase.core.domain.audio.PhrasePlayer
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

/**
 * Reproductor de audio del dispositivo (ADR-0010, ADR-0012) sobre `android.speech.tts.TextToSpeech`.
 * No informa de fallos (spec `phrase-audio`, "Fallo silencioso del motor de TTS"): si el motor no
 * está listo o el locale no tiene voz instalada, [play] no hace nada. El motor real vive mientras vive
 * el proceso, igual que `PracticeDatabase`; no se llama a `shutdown()`. El constructor interno permite
 * sustituir `isReady`, `setLanguage` y `speak` en los tests (mismo seam que `DeviceClock`); Hilt usa el
 * constructor `@Inject` sobre un `TextToSpeech` real, cuya inicialización asíncrona y bandera `ready`
 * vive en [Engine].
 */
class TextToSpeechPlayer internal constructor(
    private val isReady: () -> Boolean,
    private val setLanguage: (Locale) -> Int,
    private val speak: (String) -> Unit,
) : PhrasePlayer {

    private constructor(engine: Engine) : this(
        isReady = { engine.ready },
        setLanguage = { locale -> engine.tts.setLanguage(locale) },
        speak = { text -> engine.tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) },
    )

    @Inject
    constructor(@ApplicationContext context: Context) : this(Engine(context))

    override fun play(text: String, locale: LanguageTag) {
        if (!isReady()) return
        val result = setLanguage(Locale.forLanguageTag(locale.value))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
        speak(text)
    }

    private class Engine(context: Context) {
        var ready = false
        val tts: TextToSpeech = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }
    }
}
