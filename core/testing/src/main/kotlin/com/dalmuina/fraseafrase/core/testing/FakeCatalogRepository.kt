package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag

/**
 * Fake de [CatalogRepository] con un catálogo por idioma destino. El constructor de un solo idioma destino
 * es un atajo para los tests que solo ejercitan uno (por defecto `pt-PT`); [withCatalogs] permite configurar
 * varios idiomas destino a la vez. [lastPhrasesCall] registra el último `(targetLocale, sourceLocale)` con el
 * que se llamó a [phrases], para comprobar qué idioma de origen resolvió un caso de uso.
 */
class FakeCatalogRepository private constructor(
    private val catalogs: Map<LanguageTag, CatalogLoadResult>,
) : CatalogRepository {

    constructor(
        result: CatalogLoadResult,
        targetLocale: LanguageTag = LanguageTag("pt-PT"),
    ) : this(mapOf(targetLocale to result))

    var lastPhrasesCall: Pair<LanguageTag, LanguageTag>? = null
        private set

    override suspend fun availableTargetLocales(): List<LanguageTag> =
        catalogs.filterValues { it is CatalogLoadResult.Loaded }.keys.toList()

    override suspend fun availableSourceLocales(targetLocale: LanguageTag): List<LanguageTag> =
        (catalogs[targetLocale] as? CatalogLoadResult.Loaded)
            ?.phrases
            ?.map { it.sourceLocale }
            ?.distinct()
            .orEmpty()

    override suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult {
        lastPhrasesCall = targetLocale to sourceLocale
        return catalogs[targetLocale] ?: CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)
    }

    companion object {
        fun withCatalogs(catalogs: Map<LanguageTag, CatalogLoadResult>): FakeCatalogRepository =
            FakeCatalogRepository(catalogs)
    }
}
