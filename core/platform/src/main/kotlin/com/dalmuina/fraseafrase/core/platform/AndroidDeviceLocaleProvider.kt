package com.dalmuina.fraseafrase.core.platform

import android.os.LocaleList
import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import java.util.Locale
import javax.inject.Inject

/**
 * Android implementation of the [DeviceLocaleProvider] port (ADR-0032): reads the language list
 * configured in the system, in preference order. Same seam `TextToSpeechPlayer` uses to convert a
 * [LanguageTag] into a `Locale`, in the opposite direction. No state or coroutines, so it needs no
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
