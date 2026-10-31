package com.dalmuina.fraseafrase.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsDataSourceTest {

    private val cefrKey = stringPreferencesKey("cefr_level")
    private val batchSizeText = stringPreferencesKey("batch_size")
    private val batchSizeInt = intPreferencesKey("batch_size")

    private fun newStore(file: java.io.File, scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })

    // DataStore renombra un temporal sobre el fichero final; en Windows el rename falla si el destino ya existe,
    // así que cada test usa una ruta nueva dentro de un directorio temporal.
    private fun freshFile(): java.io.File =
        Files.createTempDirectory("settings").resolve("settings.preferences_pb").toFile()

    private fun newSource(store: DataStore<Preferences>) =
        DataStoreSettingsDataSource(dataStore = store, dispatcher = UnconfinedTestDispatcher())

    // Scenario: Valores por defecto (spec: user-settings)
    @Test
    fun `given no saved settings when reading then returns A1 and 20`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)

        // When
        val settings = newSource(store).settings()

        // Then
        settings shouldBe UserSettings.DEFAULT
    }

    // Scenario: Ajustes sobreviven al reinicio (spec: user-settings)
    @Test
    fun `given saved settings when a new store reads the same file then returns the same values`() = runTest {
        // Given
        val file = freshFile()
        // Un solo DataStore activo por fichero: el de escritura se cancela antes de abrir el de lectura.
        val writerScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        newStore(file, writerScope).edit {
            it[cefrKey] = "B1"
            it[batchSizeInt] = 25
        }
        writerScope.cancel()

        // When
        val settings = newSource(newStore(file, backgroundScope)).settings()

        // Then
        settings shouldBe UserSettings(cefrLevel = CefrLevel.B1, batchSize = BatchSize(25))
    }

    // Scenario: N fuera de rango usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N of 31 when reading then uses 20`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeText] = "31" }

        // When / Then
        newSource(store).settings().batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: N guardado como texto no entero usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N saved as text veinte when reading then uses 20 without error`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeText] = "veinte" }

        // When / Then
        newSource(store).settings().batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: N guardado como texto con forma de entero usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N saved as text with integer form when reading then uses 20 because it is not an integer`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeText] = "25" }

        // When / Then
        newSource(store).settings().batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: N guardado con otro tipo usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N saved as a boolean when reading then uses 20 without error`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[booleanPreferencesKey("batch_size")] = true }

        // When / Then
        newSource(store).settings().batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: Nivel CEFR guardado con otro tipo usa A1 sin error (spec: user-settings, "Nivel CEFR no válido")
    @Test
    fun `given cefr level saved as an integer when reading then uses A1 without error`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[intPreferencesKey("cefr_level")] = 2 }

        // When / Then
        newSource(store).settings().cefrLevel shouldBe CefrLevel.A1
    }

    // Scenario: N guardado como entero se lee correctamente (spec: user-settings)
    @Test
    fun `given N saved as an integer when reading then returns it`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeInt] = 25 }

        // When / Then
        newSource(store).settings().batchSize shouldBe BatchSize(25)
    }

    // Scenario: Nivel CEFR no válido usa A1 (spec: user-settings)
    @Test
    fun `given a CEFR level D1 when reading then uses A1`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[cefrKey] = "D1" }

        // When / Then
        newSource(store).settings().cefrLevel shouldBe CefrLevel.A1
    }

    // Scenario: Almacén ilegible devuelve los valores por defecto (spec: user-settings)
    @Test
    fun `given an unreadable store when reading then returns the defaults without error`() = runTest {
        // Given
        val unreadable = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("unreadable") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                error("not used in this test")
        }

        // When / Then
        newSource(unreadable).settings() shouldBe UserSettings.DEFAULT
    }
}
