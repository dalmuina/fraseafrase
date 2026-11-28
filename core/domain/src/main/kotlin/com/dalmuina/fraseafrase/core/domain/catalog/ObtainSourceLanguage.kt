package com.dalmuina.fraseafrase.core.domain.catalog

import javax.inject.Inject

/** Returns the current source language: the first device language matching the catalog, or the default one. */
class ObtainSourceLanguage @Inject constructor(
    private val deviceLocaleProvider: DeviceLocaleProvider,
    private val catalogRepository: CatalogRepository,
    private val defaultSourceLocale: DefaultSourceLocale,
) {

    suspend operator fun invoke(): LanguageTag = ResolveSourceLocale(
        deviceLocaleProvider.preferred(),
        defaultSourceLocale.value,
        catalogRepository.availableTargetLocales(),
    )
}
