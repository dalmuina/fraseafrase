package com.dalmuina.fraseafrase.core.data.catalog

import android.content.Context
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Catálogo leído del asset `catalog/phrases.json` (ADR-0013). Es la única fuente del catálogo, así que implementa
 * el puerto directamente. La lectura del asset va detrás de [readAsset]: el constructor de Hilt la conecta al
 * contexto de la aplicación y los tests inyectan una función propia.
 */
class AssetsCatalogDataSource internal constructor(
    private val readAsset: () -> String,
    private val dispatcher: CoroutineDispatcher,
) : CatalogRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ) : this(
        readAsset = { context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() } },
        dispatcher = dispatcher,
    )

    override suspend fun phrases(): CatalogLoadResult = withContext(dispatcher) {
        runCatching { readAsset() }
            .fold(
                onSuccess = { text -> CatalogJson.parse(text) },
                onFailure = { CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable) },
            )
    }

    private companion object {
        const val ASSET_PATH = "catalog/phrases.json"
    }
}
