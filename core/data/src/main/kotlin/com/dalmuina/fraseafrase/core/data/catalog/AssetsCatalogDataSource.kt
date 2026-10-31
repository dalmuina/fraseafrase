package com.dalmuina.fraseafrase.core.data.catalog

import android.content.Context
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadError
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
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

    // Memoización (design D9, ADR-0013 "cargado en memoria"): la primera llamada carga y guarda el resultado, sea
    // Loaded o Failed. El Mutex evita que dos llamadas concurrentes lean y parseen el asset por duplicado.
    private val mutex = Mutex()
    private var cached: CatalogLoadResult? = null

    override suspend fun phrases(): CatalogLoadResult = mutex.withLock {
        cached ?: load().also { cached = it }
    }

    // Solo un IOException es un asset ilegible. Una cancelación u otro error de programación se propaga sin
    // memoizarse (design D9): la cancelación no es un fallo del asset.
    private suspend fun load(): CatalogLoadResult = withContext(dispatcher) {
        val text = runCatching { readAsset() }.getOrElse { error ->
            if (error is IOException) return@withContext CatalogLoadResult.Failed(CatalogLoadError.AssetUnreadable)
            throw error
        }
        CatalogJson.parse(text)
    }

    private companion object {
        const val ASSET_PATH = "catalog/phrases.json"
    }
}
