package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Puerto del locale del dispositivo (ADR-0019). Una fuente: el locale configurado en el sistema Android.
 * Sin estado ni corrutinas: no necesita dispatcher.
 */
interface DeviceLocaleProvider {

    /** El locale actual del dispositivo, como etiqueta BCP-47. */
    fun current(): LanguageTag
}
