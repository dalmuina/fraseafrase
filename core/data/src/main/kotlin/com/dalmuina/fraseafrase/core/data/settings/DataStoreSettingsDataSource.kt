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
            UserSettings(cefrLevelOf(preferences, targetLocale), batchSizeOf(preferences, targetLocale))
        } catch (ignored: IOException) {
            UserSettings.DEFAULT
        }
    }

    override suspend fun save(targetLocale: LanguageTag, settings: UserSettings) = withContext(dispatcher) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(cefrLevelKey(targetLocale))] = settings.cefrLevel.name
            preferences[intPreferencesKey(batchSizeKey(targetLocale))] = settings.batchSize.value
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

    private fun cefrLevelOf(preferences: Preferences, targetLocale: LanguageTag): CefrLevel {
        val key = cefrLevelKey(targetLocale)
        val stored = preferences.asMap().entries.firstOrNull { it.key.name == key }?.value as? String
        return stored
            ?.let { name -> CefrLevel.entries.firstOrNull { it.name == name } }
            ?: CefrLevel.A1
    }

    private fun batchSizeOf(preferences: Preferences, targetLocale: LanguageTag): BatchSize {
        val key = batchSizeKey(targetLocale)
        val stored = preferences.asMap().entries.firstOrNull { it.key.name == key }?.value as? Int
        return stored
            ?.takeIf { it in BatchSize.MIN..BatchSize.MAX }
            ?.let(::BatchSize)
            ?: BatchSize.DEFAULT
    }

    private fun cefrLevelKey(targetLocale: LanguageTag): String = "${CEFR_LEVEL}_${targetLocale.value}"

    private fun batchSizeKey(targetLocale: LanguageTag): String = "${BATCH_SIZE}_${targetLocale.value}"

    private companion object {
        const val CEFR_LEVEL = "cefr_level"
        const val BATCH_SIZE = "batch_size"
        const val ACTIVE_TARGET_LOCALE = "active_target_locale"
    }
}
