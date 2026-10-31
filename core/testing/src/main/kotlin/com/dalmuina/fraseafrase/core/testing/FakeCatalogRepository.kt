package com.dalmuina.fraseafrase.core.testing

import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository

/** Fake de [CatalogRepository] que devuelve siempre el resultado configurado. */
class FakeCatalogRepository(private val result: CatalogLoadResult) : CatalogRepository {

    override suspend fun phrases(): CatalogLoadResult = result
}
