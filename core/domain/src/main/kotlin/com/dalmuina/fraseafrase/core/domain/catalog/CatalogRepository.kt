package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Puerto del catálogo de frases precargado, independiente por idioma destino (ADR-0013, ADR-0019). Una
 * fuente: Room, sembrada desde los assets JSON de `:core:data`. La carga devuelve un resultado tipado: un
 * catálogo inválido es un resultado esperable, no una excepción.
 */
interface CatalogRepository {

    /** Idiomas destino cuyo catálogo se cargó correctamente. */
    suspend fun availableTargetLocales(): List<LanguageTag>

    suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult
}
