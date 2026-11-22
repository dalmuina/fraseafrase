package com.dalmuina.fraseafrase.core.domain.catalog

import javax.inject.Inject

/**
 * Resuelve las frases del catálogo para un idioma destino usando el idioma de origen vigente para ese
 * idioma destino (ADR-0003, ADR-0019, ADR-0020): el locale del dispositivo si hay contenido con ese
 * `sourceLocale`, o un respaldo que nunca coincide con el propio idioma destino en caso contrario
 * ([ResolveSourceLocale]). Sustituye a llamar directamente a [CatalogRepository.phrases] con un locale
 * de origen fijo.
 */
class ObtainCatalogPhrasesForActiveLanguage @Inject constructor(
    private val deviceLocaleProvider: DeviceLocaleProvider,
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(targetLocale: LanguageTag): CatalogLoadResult {
        val available = catalogRepository.availableSourceLocales(targetLocale)
        // Temporal hasta 2.4: conserva el comportamiento previo (excluye el destino de los candidatos).
        val candidates = available.filterNot { it == targetLocale }
        val device = deviceLocaleProvider.current()
        val sourceLocale = candidates.firstOrNull { it == device }
            ?: candidates.firstOrNull { it == LanguageTag("es-ES") }
            ?: candidates.firstOrNull()
            ?: device
        return catalogRepository.phrases(targetLocale, sourceLocale)
    }
}
