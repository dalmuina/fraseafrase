package com.dalmuina.fraseafrase.core.platform

import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import java.util.Locale
import javax.inject.Inject

/**
 * Implementación Android del puerto [DeviceLocaleProvider] (ADR-0019): lee el locale configurado en
 * el sistema. Mismo seam que `TextToSpeechPlayer` usa para convertir [LanguageTag] a `Locale`, pero en
 * sentido inverso. Sin estado ni corrutinas: no necesita dispatcher.
 */
class AndroidDeviceLocaleProvider internal constructor(
    private val locale: () -> Locale,
) : DeviceLocaleProvider {

    @Inject
    constructor() : this(locale = { Locale.getDefault() })

    override fun current(): LanguageTag = LanguageTag(locale().toLanguageTag())
}
