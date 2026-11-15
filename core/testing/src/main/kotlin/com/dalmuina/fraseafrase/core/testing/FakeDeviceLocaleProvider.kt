package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/** Fake de [DeviceLocaleProvider] que devuelve siempre el locale configurado. */
class FakeDeviceLocaleProvider(private val locale: LanguageTag) : DeviceLocaleProvider {

    override fun current(): LanguageTag = locale
}
