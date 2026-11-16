package com.dalmuina.fraseafrase.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Ajustes del usuario en DataStore Preferences, independientes por idioma destino, más el idioma destino
 * activo (ADR-0013, ADR-0019). Nunca falla al leer: una clave ausente, con otro tipo, fuera de rango o
 * ilegible cae al valor por defecto de la spec `user-settings`.
 *
 * DataStore compara las claves por nombre: leer `batch_size_<locale>` con una clave tipada lanza
 * `ClassCastException` si se guardó con otro tipo. Por eso el valor se lee del mapa completo (`asMap`), sin
 * castear, y solo se acepta un `Int` dentro de rango: un texto, incluido uno con forma de entero, no es un
 * entero (spec `user-settings`).
 */
class DataStoreSettingsDataSource internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher,
    private val catalogRepository: CatalogRepository,
) : SettingsRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        catalogRepository: CatalogRepository,
    ) : this(dataStore = context.settingsDataStore, dispatcher = dispatcher, catalogRepository = catalogRepository)

    override suspend fun settings(targetLocale: LanguageTag): UserSettings = withContext(dispatcher) {
        try {
            val preferences = dataStore.data.first()
            UserSettings(
                cefrLevelOf(preferences, keyFor(CEFR_LEVEL, targetLocale)) ?: CefrLevel.A1,
                batchSizeOf(preferences, keyFor(BATCH_SIZE, targetLocale)) ?: BatchSize.DEFAULT,
            )
        } catch (ignored: IOException) {
            UserSettings.DEFAULT
        }
    }

    override suspend fun save(targetLocale: LanguageTag, settings: UserSettings) = withContext(dispatcher) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(keyFor(CEFR_LEVEL, targetLocale))] = settings.cefrLevel.name
            preferences[intPreferencesKey(keyFor(BATCH_SIZE, targetLocale))] = settings.batchSize.value
        }
        Unit
    }

    override suspend fun activeTargetLocale(): LanguageTag = withContext(dispatcher) {
        val stored = dataStore.data.first().asMap().entries
            .firstOrNull { it.key.name == ACTIVE_TARGET_LOCALE }?.value as? String
        val available = catalogRepository.availableTargetLocales()
        val storedLocale = stored?.let(::LanguageTag)
        storedLocale?.takeIf { it in available }
            ?: available.firstOrNull()
            ?: storedLocale
            ?: error("No target locale available")
    }

    override suspend fun setActiveTargetLocale(targetLocale: LanguageTag) = withContext(dispatcher) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(ACTIVE_TARGET_LOCALE)] = targetLocale.value
        }
        Unit
    }

    override suspend fun appliedSettings(targetLocale: LanguageTag): UserSettings? = withContext(dispatcher) {
        try {
            val preferences = dataStore.data.first()
            val cefrLevel = cefrLevelOf(preferences, keyFor(APPLIED_CEFR_LEVEL, targetLocale))
            val batchSize = batchSizeOf(preferences, keyFor(APPLIED_BATCH_SIZE, targetLocale))
            if (cefrLevel != null && batchSize != null) UserSettings(cefrLevel, batchSize) else null
        } catch (ignored: IOException) {
            null
        }
    }

    override suspend fun recordAppliedSettings(targetLocale: LanguageTag, settings: UserSettings) =
        withContext(dispatcher) {
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey(keyFor(APPLIED_CEFR_LEVEL, targetLocale))] = settings.cefrLevel.name
                preferences[intPreferencesKey(keyFor(APPLIED_BATCH_SIZE, targetLocale))] = settings.batchSize.value
            }
            Unit
        }

    /** Nivel CEFR guardado bajo [key], o `null` si está ausente, es de otro tipo o no es un nivel válido. */
    private fun cefrLevelOf(preferences: Preferences, key: String): CefrLevel? {
        val stored = preferences.asMap().entries.firstOrNull { it.key.name == key }?.value as? String
        return stored?.let { name -> CefrLevel.entries.firstOrNull { it.name == name } }
    }

    /** N guardada bajo [key], o `null` si está ausente, es de otro tipo o está fuera de rango. */
    private fun batchSizeOf(preferences: Preferences, key: String): BatchSize? {
        val stored = preferences.asMap().entries.firstOrNull { it.key.name == key }?.value as? Int
        return stored?.takeIf { it in BatchSize.MIN..BatchSize.MAX }?.let(::BatchSize)
    }

    private fun keyFor(prefix: String, targetLocale: LanguageTag): String = "${prefix}_${targetLocale.value}"

    private companion object {
        const val CEFR_LEVEL = "cefr_level"
        const val BATCH_SIZE = "batch_size"
        const val ACTIVE_TARGET_LOCALE = "active_target_locale"
        const val APPLIED_CEFR_LEVEL = "applied_cefr_level"
        const val APPLIED_BATCH_SIZE = "applied_batch_size"
    }
}
