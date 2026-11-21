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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Catálogo sembrado en [CatalogDatabase] desde los documentos JSON empaquetados (ADR-0019, ADR-0022). Es la única
 * fuente del catálogo, así que implementa el puerto directamente. El sembrado ocurre una vez por instancia, antes
 * de servir cualquier consulta; un documento, de cualquiera de los dos tipos, cuyo contenido no se pudo validar
 * simplemente no siembra nada y no bloquea el sembrado de los demás (design.md - "Dos tipos de documento JSON").
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

    override suspend fun availableSourceLocales(targetLocale: LanguageTag): List<LanguageTag> =
        withContext(dispatcher) {
            ensureSeeded()
            database.catalogDao().sourceLocalesFor(targetLocale.value).map(::LanguageTag)
        }

    override suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult =
        withContext(dispatcher) {
            ensureSeeded()
            val rows = database.catalogDao().phrasesFor(targetLocale.value, sourceLocale.value)
            CatalogLoadResult.Loaded(rows.map { it.toDomain() })
        }

    /**
     * Siembra en dos pasadas: primero todos los documentos de frases destino, después todos los de traducción
     * (design.md - "Un id de traducción sin frase destino correspondiente..."), sin depender del orden de
     * [catalogDocuments] (no garantizado, ver `AssetManager.list()`). Así, una traducción nunca se valida contra
     * las frases destino de su propio idioma antes de que estas ya estén sembradas.
     */
    private suspend fun ensureSeeded() {
        if (seeded) return
        seedingMutex.withLock {
            if (seeded) return
            val documents = catalogDocuments()
            val (translationDocuments, phraseDocuments) =
                documents.partition { CatalogJson.headerOf(it)?.isTranslation == true }
            phraseDocuments.forEach { text -> seedDocument(text) }
            translationDocuments.forEach { text -> seedDocument(text) }
            seeded = true
        }
    }

    /** Un documento sin cabecera válida o que no valida por completo no siembra nada: no bloquea a los demás. */
    private suspend fun seedDocument(text: String) {
        val header = CatalogJson.headerOf(text) ?: return
        val sourceLocale = header.sourceLocale
        if (sourceLocale != null) {
            seedTranslationDocument(header, sourceLocale, text)
        } else {
            seedPhraseDocument(header, text)
        }
    }

    private suspend fun seedPhraseDocument(header: CatalogHeader, text: String) {
        val existing = database.catalogDao().seedVersionFor(
            header.targetLocale,
            CatalogSeedVersionEntity.NO_SOURCE_LOCALE,
        )
        if (existing?.schemaVersion == header.schemaVersion) return
        val result = CatalogJson.parsePhrases(text)
        if (result !is CatalogParseResult.PhrasesParsed) return
        val entities = result.phrases.mapIndexed { position, phrase -> phrase.toEntity(position) }
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.catalogDao().deletePhrasesForTargetLocale(header.targetLocale)
                database.catalogDao().insertPhrases(entities)
                database.catalogDao().upsertSeedVersion(
                    CatalogSeedVersionEntity(
                        targetLocale = header.targetLocale,
                        sourceLocale = CatalogSeedVersionEntity.NO_SOURCE_LOCALE,
                        schemaVersion = header.schemaVersion,
                    ),
                )
            }
        }
    }

    /** Una traducción cuyo id no tiene frase destino correspondiente no siembra nada de su documento. */
    private suspend fun seedTranslationDocument(header: CatalogHeader, sourceLocale: String, text: String) {
        val existing = database.catalogDao().seedVersionFor(header.targetLocale, sourceLocale)
        if (existing?.schemaVersion == header.schemaVersion) return
        val entities = validTranslationEntitiesOf(header, text) ?: return
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                database.catalogDao().deleteTranslationsFor(header.targetLocale, sourceLocale)
                database.catalogDao().insertTranslations(entities)
                database.catalogDao().upsertSeedVersion(
                    CatalogSeedVersionEntity(
                        targetLocale = header.targetLocale,
                        sourceLocale = sourceLocale,
                        schemaVersion = header.schemaVersion,
                    ),
                )
            }
        }
    }

    /** Null si el documento no valida, o si alguna traducción no tiene frase destino correspondiente ya sembrada. */
    private suspend fun validTranslationEntitiesOf(
        header: CatalogHeader,
        text: String,
    ): List<CatalogTranslationEntity>? {
        val result = CatalogJson.parseTranslations(text) as? CatalogParseResult.TranslationsParsed ?: return null
        val phraseIds = database.catalogDao().phraseIdsFor(header.targetLocale).toSet()
        val allHaveAMatchingPhrase = result.translations.all { it.id.value in phraseIds }
        return result.translations.takeIf { allHaveAMatchingPhrase }?.map { it.toEntity() }
    }

    private companion object {
        const val ASSETS_DIR = "catalog"

        fun readPackagedCatalogDocuments(context: Context): List<String> =
            context.assets.list(ASSETS_DIR).orEmpty()
                .filter { it.endsWith(".json") }
                .map { name -> context.assets.open("$ASSETS_DIR/$name").bufferedReader().use { it.readText() } }
    }
}
