package com.dalmuina.fraseafrase.core.domain.settings

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import javax.inject.Inject

/** Devuelve los idiomas destino disponibles (spec `phrase-catalog`, "Idiomas destino disponibles"). */
class ObtainAvailableLanguages @Inject constructor(private val catalogRepository: CatalogRepository) {

    suspend operator fun invoke(): List<LanguageTag> = catalogRepository.availableTargetLocales()
}
