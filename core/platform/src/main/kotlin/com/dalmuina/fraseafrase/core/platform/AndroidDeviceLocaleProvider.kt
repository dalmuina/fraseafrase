package com.dalmuina.fraseafrase.core.platform

import android.os.LocaleList
import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import java.util.Locale
import javax.inject.Inject

/**
 * Implementación Android del puerto [DeviceLocaleProvider] (ADR-0019): lee la lista de idiomas
 * configurada en el sistema, en su orden de preferencia. Mismo seam que `TextToSpeechPlayer` usa para
 * convertir [LanguageTag] a `Locale`, pero en sentido inverso. Sin estado ni corrutinas: no necesita
 * dispatcher.
 */
class AndroidDeviceLocaleProvider internal constructor(
    private val locales: () -> List<Locale>,
) : DeviceLocaleProvider {

    @Inject
    constructor() : this(
        locales = {
            val system = LocaleList.getDefault()
            List(system.size()) { index -> system[index] }
        },
    )

    // Malformed tags are dropped so that no system language can crash the app when building LanguageTag.
    override fun preferred(): List<LanguageTag> = locales()
        .map { it.toLanguageTag() }
        .filter(LanguageTag::isWellFormed)
        .map(::LanguageTag)
}
