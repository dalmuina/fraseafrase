package com.dalmuina.fraseafrase.core.domain.catalog

/**
 * Puerto del catálogo de frases precargado (ADR-0013). Una fuente: el asset JSON de `:core:data`.
 * La carga devuelve un resultado tipado: un catálogo inválido es un resultado esperable, no una excepción.
 */
interface CatalogRepository {

    suspend fun phrases(): CatalogLoadResult
}
