package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/** Fake [DeviceLocaleProvider] that returns the configured [locales], which can change between calls. */
class FakeDeviceLocaleProvider(var locales: List<LanguageTag>) : DeviceLocaleProvider {

    constructor(locale: LanguageTag) : this(listOf(locale))

    /** First language of the list; assigning it leaves that language as the only one. */
    var locale: LanguageTag
        get() = locales.first()
        set(value) {
            locales = listOf(value)
        }

    override fun preferred(): List<LanguageTag> = locales
}
