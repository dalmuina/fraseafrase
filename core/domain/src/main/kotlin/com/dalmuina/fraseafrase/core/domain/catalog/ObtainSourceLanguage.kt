package com.dalmuina.fraseafrase.core.domain.catalog

import javax.inject.Inject

/** Devuelve el idioma de origen vigente: el del dispositivo si hay catálogo en él, o el por defecto. */
class ObtainSourceLanguage @Inject constructor(
    private val deviceLocaleProvider: DeviceLocaleProvider,
    private val catalogRepository: CatalogRepository,
    private val defaultSourceLocale: DefaultSourceLocale,
) {

    suspend operator fun invoke(): LanguageTag = ResolveSourceLocale(
        deviceLocaleProvider.current(),
        defaultSourceLocale.value,
        catalogRepository.availableTargetLocales(),
    )
}
