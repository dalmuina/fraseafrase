package com.dalmuina.fraseafrase.core.domain.catalog

/** Resultado de cargar el catálogo: las frases en orden de catálogo, o el error que impide cargarlo. */
sealed interface CatalogLoadResult {

    data class Loaded(val phrases: List<CatalogPhrase>) : CatalogLoadResult

    data class Failed(val error: CatalogLoadError) : CatalogLoadResult
}

/** Motivos por los que la carga del catálogo se rechaza (spec `phrase-catalog`). */
sealed interface CatalogLoadError {

    data class UnsupportedSchemaVersion(val found: Int) : CatalogLoadError

    data object MissingSchemaVersion : CatalogLoadError

    data class InvalidLocale(val path: String) : CatalogLoadError

    data class InvalidPhrase(val id: String, val field: String) : CatalogLoadError

    data class DuplicateId(val id: String) : CatalogLoadError

    data object AssetUnreadable : CatalogLoadError
}
