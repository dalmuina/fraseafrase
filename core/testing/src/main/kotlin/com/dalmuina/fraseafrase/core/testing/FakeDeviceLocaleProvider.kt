package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/** Fake de [DeviceLocaleProvider] que devuelve el [locale] configurado, modificable entre llamadas. */
class FakeDeviceLocaleProvider(var locale: LanguageTag) : DeviceLocaleProvider {

    override fun current(): LanguageTag = locale
}
