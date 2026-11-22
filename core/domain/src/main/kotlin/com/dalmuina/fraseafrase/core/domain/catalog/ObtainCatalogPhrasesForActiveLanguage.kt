package com.dalmuina.fraseafrase.core.domain.catalog

import javax.inject.Inject

/**
 * Resuelve las frases del catálogo para un idioma destino usando el idioma de origen vigente
 * ([ObtainSourceLanguage], ADR-0003, ADR-0019): el repositorio devuelve cada frase con el texto del
 * idioma de origen como `source`.
 */
class ObtainCatalogPhrasesForActiveLanguage @Inject constructor(
    private val obtainSourceLanguage: ObtainSourceLanguage,
    private val catalogRepository: CatalogRepository,
) {

    suspend operator fun invoke(targetLocale: LanguageTag): CatalogLoadResult =
        catalogRepository.phrases(targetLocale, obtainSourceLanguage())
}
