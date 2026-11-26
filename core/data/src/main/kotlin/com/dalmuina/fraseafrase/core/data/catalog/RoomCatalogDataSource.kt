package com.dalmuina.fraseafrase.core.data.catalog

import android.content.Context
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catálogo sembrado en [CatalogDatabase] desde los documentos JSON empaquetados (ADR-0023, ADR-0028). Es la única
 * fuente del catálogo, así que implementa el puerto directamente. El sembrado se comprueba una vez por instancia,
 * antes de servir cualquier consulta, y solo reescribe los idiomas cuyo documento cambió desde la última siembra; un
 * documento cuyo contenido no se pudo validar no siembra nada, conserva lo ya sembrado y no bloquea a los demás. El
 * texto de origen sale de un self-JOIN entre idiomas.
 *
 * Es `@Singleton` por el mismo motivo que la antigua fuente de assets: una única siembra por proceso.
 */
@Singleton
class RoomCatalogDataSource internal constructor(
    private val database: CatalogDatabase,
    private val catalogDocuments: () -> List<String>,
    private val dispatcher: CoroutineDispatcher,
) : CatalogRepository {

    @Inject
    constructor(
        database: CatalogDatabase,
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ) : this(
        database = database,
        catalogDocuments = { readPackagedCatalogDocuments(context) },
        dispatcher = dispatcher,
    )

    private val seedingMutex = Mutex()
    private var seeded = false

    override suspend fun availableTargetLocales(): List<LanguageTag> = withContext(dispatcher) {
        ensureSeeded()
        database.catalogDao().targetLocales().map(::LanguageTag)
    }

    override suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult =
        withContext(dispatcher) {
            ensureSeeded()
            val rows = database.catalogDao().phrasesFor(targetLocale.value, sourceLocale.value)
            CatalogLoadResult.Loaded(rows.map { it.toDomain() })
        }

    /** Siembra cada documento de frases una sola vez por instancia, antes de servir cualquier consulta. */
    private suspend fun ensureSeeded() {
        if (seeded) return
        seedingMutex.withLock {
            if (seeded) return
            catalogDocuments().forEach { text -> seedDocument(text) }
            seeded = true
        }
    }

    /**
     * Siembra el documento si su huella difiere de la guardada. Un documento sin cabecera válida o que no valida por
     * completo no siembra nada: no bloquea a los demás y se vuelve a intentar en el siguiente arranque.
     */
    private suspend fun seedDocument(text: String) {
        val header = CatalogJson.headerOf(text) ?: return
        val contentHash = sha256Of(text)
        if (database.catalogDao().seedVersionFor(header.targetLocale)?.contentHash == contentHash) return
        val parsed = CatalogJson.parsePhrases(text)
        if (parsed is CatalogParseResult.PhrasesParsed) replaceLanguage(header.targetLocale, contentHash, parsed)
    }

    private suspend fun replaceLanguage(
        targetLocale: String,
        contentHash: String,
        parsed: CatalogParseResult.PhrasesParsed,
    ) {
        val entities = parsed.phrases.mapIndexed { position, phrase -> phrase.toEntity(position) }
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.catalogDao().deletePhrasesForTargetLocale(targetLocale)
                database.catalogDao().insertPhrases(entities)
                database.catalogDao().upsertSeedVersion(
                    CatalogSeedVersionEntity(targetLocale = targetLocale, contentHash = contentHash),
                )
            }
        }
    }

    private companion object {
        const val ASSETS_DIR = "catalog"
        const val HASH_ALGORITHM = "SHA-256"
        const val HEX_RADIX = 16

        /** Huella en hexadecimal del texto de un documento. */
        fun sha256Of(text: String): String =
            MessageDigest.getInstance(HASH_ALGORITHM)
                .digest(text.toByteArray(Charsets.UTF_8))
                .joinToString("") { it.toUByte().toString(HEX_RADIX).padStart(2, '0') }

        fun readPackagedCatalogDocuments(context: Context): List<String> =
            context.assets.list(ASSETS_DIR).orEmpty()
                .filter { it.endsWith(".json") }
                .map { name -> context.assets.open("$ASSETS_DIR/$name").bufferedReader().use { it.readText() } }
    }
}
