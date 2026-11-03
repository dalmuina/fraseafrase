package com.dalmuina.fraseafrase.core.domain.audio

import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Puerto de síntesis de voz (ADR-0010, ADR-0012). Reproduce un texto en el locale dado. No informa de
 * fallos: un motor no disponible o un locale sin voz instalada no reproducen nada (spec `phrase-audio`,
 * "Fallo silencioso del motor de TTS"). La implementación de sistema vive en `:core:platform`.
 */
interface PhrasePlayer {
    fun play(text: String, locale: LanguageTag)
}
