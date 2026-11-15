package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Resuelve el idioma de origen por defecto (ADR-0003, ADR-0019): el locale del dispositivo si hay
 * contenido de catálogo con ese `sourceLocale` entre [available], o `es-ES` en caso contrario. Función
 * pura, sin dependencias de infraestructura.
 */
object ResolveSourceLocale {

    private val fallback = LanguageTag("es-ES")

    operator fun invoke(deviceLocale: LanguageTag, available: List<LanguageTag>): LanguageTag =
        available.firstOrNull { it == deviceLocale } ?: fallback
}
