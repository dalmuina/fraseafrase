package com.dalmuina.fraseafrase.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
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
 * Ajustes del usuario en DataStore Preferences (ADR-0013). Nunca falla: una clave ausente, con otro tipo, fuera de
 * rango o ilegible cae al valor por defecto de la spec `user-settings`.
 *
 * DataStore compara las claves por nombre: leer `batch_size` con una clave tipada lanza `ClassCastException` si se
 * guardó con otro tipo. Por eso el valor se lee del mapa completo (`asMap`), sin castear, y solo se acepta un `Int`
 * dentro de rango: un texto, incluido uno con forma de entero, no es un entero (spec `user-settings`).
 */
class DataStoreSettingsDataSource internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher,
) : SettingsRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ) : this(dataStore = context.settingsDataStore, dispatcher = dispatcher)

    override suspend fun settings(): UserSettings = withContext(dispatcher) {
        try {
            val preferences = dataStore.data.first()
            UserSettings(cefrLevelOf(preferences), batchSizeOf(preferences))
        } catch (ignored: IOException) {
            UserSettings.DEFAULT
        }
    }

    private fun cefrLevelOf(preferences: Preferences): CefrLevel {
        val stored = preferences.asMap().entries.firstOrNull { it.key.name == CEFR_LEVEL }?.value as? String
        return stored
            ?.let { name -> CefrLevel.entries.firstOrNull { it.name == name } }
            ?: CefrLevel.A1
    }

    private fun batchSizeOf(preferences: Preferences): BatchSize {
        val stored = preferences.asMap().entries.firstOrNull { it.key.name == BATCH_SIZE }?.value as? Int
        return stored
            ?.takeIf { it in BatchSize.MIN..BatchSize.MAX }
            ?.let(::BatchSize)
            ?: BatchSize.DEFAULT
    }

    private companion object {
        const val CEFR_LEVEL = "cefr_level"
        const val BATCH_SIZE = "batch_size"
    }
}
