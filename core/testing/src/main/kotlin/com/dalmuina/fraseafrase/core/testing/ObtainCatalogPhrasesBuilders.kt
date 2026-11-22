package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.DefaultSourceLocale
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainCatalogPhrasesForActiveLanguage
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage

/** Construye [ObtainCatalogPhrasesForActiveLanguage] con el dispositivo en [deviceLocale] y `es-ES` por defecto. */
fun anObtainCatalogPhrases(
    catalogRepository: CatalogRepository,
    deviceLocale: LanguageTag = LanguageTag("es-ES"),
): ObtainCatalogPhrasesForActiveLanguage = ObtainCatalogPhrasesForActiveLanguage(
    obtainSourceLanguage = ObtainSourceLanguage(
        FakeDeviceLocaleProvider(deviceLocale),
        catalogRepository,
        DefaultSourceLocale(LanguageTag("es-ES")),
    ),
    catalogRepository = catalogRepository,
)
