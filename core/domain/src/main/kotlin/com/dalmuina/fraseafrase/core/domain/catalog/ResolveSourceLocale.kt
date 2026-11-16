package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Resuelve el idioma de origen por defecto (ADR-0003, ADR-0019, ADR-0020): el locale del dispositivo
 * si hay contenido de catálogo con ese `sourceLocale` entre [available] para [targetLocale], o `es-ES`
 * entre los candidatos restantes en caso contrario. El respaldo nunca coincide con [targetLocale]: se
 * excluye de los candidatos antes de elegir. Si no queda ningún candidato (catálogo del destino vacío
 * o fallido), devuelve [deviceLocale] sin lanzar: el fallo lo reporta el repositorio de catálogo al
 * resolver las frases. Función pura, sin dependencias de infraestructura.
 */
object ResolveSourceLocale {

    private val preferredFallback = LanguageTag("es-ES")

    operator fun invoke(
        deviceLocale: LanguageTag,
        targetLocale: LanguageTag,
        available: List<LanguageTag>,
    ): LanguageTag {
        val candidates = available.filterNot { it == targetLocale }
        return candidates.firstOrNull { it == deviceLocale }
            ?: candidates.firstOrNull { it == preferredFallback }
            ?: candidates.firstOrNull()
            ?: deviceLocale
    }
}
