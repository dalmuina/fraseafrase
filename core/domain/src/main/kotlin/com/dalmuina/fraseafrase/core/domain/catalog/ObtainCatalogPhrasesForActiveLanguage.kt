package com.dalmuina.fraseafrase.core.domain.catalog

import javax.inject.Inject

/**
 * Resuelve las frases del catálogo para un idioma destino usando el idioma de origen vigente para ese
 * idioma destino (ADR-0003, ADR-0019): el locale del dispositivo si hay contenido con ese `sourceLocale`,
 * o `es-ES` en caso contrario ([ResolveSourceLocale]). Sustituye a llamar directamente a
 * [CatalogRepository.phrases] con un locale de origen fijo.
 */
class ObtainCatalogPhrasesForActiveLanguage @Inject constructor(
    private val deviceLocaleProvider: DeviceLocaleProvider,
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(targetLocale: LanguageTag): CatalogLoadResult {
        val available = catalogRepository.availableSourceLocales(targetLocale)
        val sourceLocale = ResolveSourceLocale(deviceLocaleProvider.current(), targetLocale, available)
        return catalogRepository.phrases(targetLocale, sourceLocale)
    }
}
