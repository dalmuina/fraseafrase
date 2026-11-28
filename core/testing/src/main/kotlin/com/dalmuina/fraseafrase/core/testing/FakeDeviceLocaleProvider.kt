package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/** Fake de [DeviceLocaleProvider] que devuelve las [locales] configuradas, modificables entre llamadas. */
class FakeDeviceLocaleProvider(var locales: List<LanguageTag>) : DeviceLocaleProvider {

    constructor(locale: LanguageTag) : this(listOf(locale))

    /** Primer idioma de la lista; al asignarlo la lista queda con ese único idioma. */
    var locale: LanguageTag
        get() = locales.first()
        set(value) {
            locales = listOf(value)
        }

    override fun preferred(): List<LanguageTag> = locales
}
