package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Port for the device languages (ADR-0019). Single source: the locales configured in the Android system.
 * Stateless and without coroutines: it needs no dispatcher.
 */
interface DeviceLocaleProvider {

    /** The device languages as BCP-47 tags, in the system preference order. It may be empty. */
    fun preferred(): List<LanguageTag>
}
