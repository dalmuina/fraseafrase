package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Resuelve el idioma de origen (ADR-0003, ADR-0019): el locale del dispositivo si está entre [available];
 * si no, [default]. Función pura, sin literales de idioma ni dependencias de infraestructura.
 */
object ResolveSourceLocale {

    operator fun invoke(device: LanguageTag, default: LanguageTag, available: List<LanguageTag>): LanguageTag =
        if (device in available) device else default
}
