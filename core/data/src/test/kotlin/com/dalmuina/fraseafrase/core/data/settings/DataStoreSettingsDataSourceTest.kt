package com.dalmuina.fraseafrase.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.testing.FakeCatalogRepository
import io.kotest.assertions.throwables.shouldThrow
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

    private val ptPt = LanguageTag("pt-PT")
    private val ptBr = LanguageTag("pt-BR")

    private val cefrKeyPtPt = stringPreferencesKey("cefr_level_pt-PT")
    private val batchSizeTextPtPt = stringPreferencesKey("batch_size_pt-PT")
    private val batchSizeIntPtPt = intPreferencesKey("batch_size_pt-PT")

    private fun newStore(file: java.io.File, scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })

    // DataStore renombra un temporal sobre el fichero final; en Windows el rename falla si el destino ya existe,
    // así que cada test usa una ruta nueva dentro de un directorio temporal.
    private fun freshFile(): java.io.File =
        Files.createTempDirectory("settings").resolve("settings.preferences_pb").toFile()

    private fun availableLocales(locales: List<LanguageTag> = listOf(ptPt, ptBr)): FakeCatalogRepository =
        FakeCatalogRepository.withCatalogs(locales.associateWith { CatalogLoadResult.Loaded(emptyList()) })

    private fun newSource(
        store: DataStore<Preferences>,
        catalogRepository: FakeCatalogRepository = availableLocales(),
    ) = DataStoreSettingsDataSource(
        dataStore = store,
        dispatcher = UnconfinedTestDispatcher(),
        catalogRepository = catalogRepository,
    )

    // Scenario: Primera ejecución (spec: user-settings)
    @Test
    fun `given no saved settings when reading pt-PT then returns A1 and 20`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)

        // When
        val settings = newSource(store).settings(ptPt)

        // Then
        settings shouldBe UserSettings.DEFAULT
    }

    // Scenario: Un idioma destino nuevo no hereda los ajustes de otro (spec: user-settings)
    @Test
    fun `given pt-PT with saved settings when reading pt-BR then returns the defaults`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        val source = newSource(store)
        source.save(ptPt, UserSettings(cefrLevel = CefrLevel.B1, batchSize = BatchSize(25)))

        // When
        val settings = source.settings(ptBr)

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
            it[cefrKeyPtPt] = "B1"
            it[batchSizeIntPtPt] = 25
        }
        writerScope.cancel()

        // When
        val settings = newSource(newStore(file, backgroundScope)).settings(ptPt)

        // Then
        settings shouldBe UserSettings(cefrLevel = CefrLevel.B1, batchSize = BatchSize(25))
    }

    // DataStore real apoyado en fichero falla al renombrar en Windows si se le pide una segunda escritura
    // seguida en el mismo test (lock todavía no liberado); los escenarios que necesitan dos escrituras
    // seguidas usan en su lugar un DataStore en memoria.
    private fun inMemoryStore(initial: Preferences = emptyPreferences()): DataStore<Preferences> =
        object : DataStore<Preferences> {
            private var current = initial

            override val data: Flow<Preferences> = flow { emit(current) }

            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                current = transform(current)
                return current
            }
        }

    // Scenario: Los ajustes de dos idiomas destino no se mezclan (spec: user-settings)
    @Test
    fun `given different settings saved for pt-PT and pt-BR when reading each then they do not mix`() = runTest {
        // Given
        val source = newSource(inMemoryStore())
        source.save(ptPt, UserSettings(cefrLevel = CefrLevel.B1, batchSize = BatchSize(25)))
        source.save(ptBr, UserSettings(cefrLevel = CefrLevel.A2, batchSize = BatchSize(10)))

        // When / Then
        source.settings(ptPt) shouldBe UserSettings(cefrLevel = CefrLevel.B1, batchSize = BatchSize(25))
        source.settings(ptBr) shouldBe UserSettings(cefrLevel = CefrLevel.A2, batchSize = BatchSize(10))
    }

    // Scenario: N fuera de rango usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N of 31 when reading then uses 20`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeTextPtPt] = "31" }

        // When / Then
        newSource(store).settings(ptPt).batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: N guardado como texto no entero usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N saved as text veinte when reading then uses 20 without error`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeTextPtPt] = "veinte" }

        // When / Then
        newSource(store).settings(ptPt).batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: N guardado como texto con forma de entero usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N saved as text with integer form when reading then uses 20 because it is not an integer`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeTextPtPt] = "25" }

        // When / Then
        newSource(store).settings(ptPt).batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: N guardado con otro tipo usa el valor por defecto (spec: user-settings)
    @Test
    fun `given N saved as a boolean when reading then uses 20 without error`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[booleanPreferencesKey("batch_size_pt-PT")] = true }

        // When / Then
        newSource(store).settings(ptPt).batchSize shouldBe BatchSize.DEFAULT
    }

    // Scenario: Nivel CEFR guardado con otro tipo usa A1 sin error (spec: user-settings, "Nivel CEFR no válido")
    @Test
    fun `given cefr level saved as an integer when reading then uses A1 without error`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[intPreferencesKey("cefr_level_pt-PT")] = 2 }

        // When / Then
        newSource(store).settings(ptPt).cefrLevel shouldBe CefrLevel.A1
    }

    // Scenario: N guardado como entero se lee correctamente (spec: user-settings)
    @Test
    fun `given N saved as an integer when reading then returns it`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[batchSizeIntPtPt] = 25 }

        // When / Then
        newSource(store).settings(ptPt).batchSize shouldBe BatchSize(25)
    }

    // Scenario: Nivel CEFR no válido usa A1 (spec: user-settings)
    @Test
    fun `given a CEFR level D1 when reading then uses A1`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        store.edit { it[cefrKeyPtPt] = "D1" }

        // When / Then
        newSource(store).settings(ptPt).cefrLevel shouldBe CefrLevel.A1
    }

    // Scenario: Guardar sustituye los ajustes anteriores (spec: user-settings)
    @Test
    fun `given cefr level and batch size saved when settings are read then they match`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        val source = newSource(store)

        // When
        source.save(ptPt, UserSettings(cefrLevel = CefrLevel.B2, batchSize = BatchSize(15)))

        // Then
        source.settings(ptPt) shouldBe UserSettings(cefrLevel = CefrLevel.B2, batchSize = BatchSize(15))
    }

    // Scenario: Guardar los ajustes de un idioma destino no modifica los de otro (spec: user-settings)
    @Test
    fun `given pt-BR with saved settings when saving pt-PT then pt-BR is unchanged`() = runTest {
        // Given
        val source = newSource(inMemoryStore())
        source.save(ptBr, UserSettings(cefrLevel = CefrLevel.A1, batchSize = BatchSize(20)))

        // When
        source.save(ptPt, UserSettings(cefrLevel = CefrLevel.B2, batchSize = BatchSize(15)))

        // Then
        source.settings(ptBr) shouldBe UserSettings(cefrLevel = CefrLevel.A1, batchSize = BatchSize(20))
    }

    // Scenario: Fallo de escritura no mezcla ajustes antiguos y nuevos (spec: user-settings)
    @Test
    fun `given a write failure when save is called then it throws and a read still returns the previous settings`() =
        runTest {
            // Given
            val stored = emptyPreferences().toMutablePreferences().apply {
                this[cefrKeyPtPt] = "B1"
                this[batchSizeIntPtPt] = 25
            }.toPreferences()
            val failingOnWriteStore = object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow { emit(stored) }

                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                    throw IOException("write failed")
                }
            }
            val source = newSource(failingOnWriteStore)

            // When / Then
            shouldThrow<IOException> {
                source.save(ptPt, UserSettings(cefrLevel = CefrLevel.B2, batchSize = BatchSize(15)))
            }
            source.settings(ptPt) shouldBe UserSettings(cefrLevel = CefrLevel.B1, batchSize = BatchSize(25))
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
        newSource(unreadable).settings(ptPt) shouldBe UserSettings.DEFAULT
    }

    // Scenario: Primera ejecución usa el primer idioma destino disponible (spec: user-settings)
    @Test
    fun `given no active target locale saved when reading it then returns the first available`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        val source = newSource(store, availableLocales(listOf(ptPt, ptBr)))

        // When / Then
        source.activeTargetLocale() shouldBe ptPt
    }

    // Scenario: El idioma destino activo guardado se recupera (spec: user-settings)
    @Test
    fun `given pt-BR saved as active when reading it then returns pt-BR`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        val source = newSource(store, availableLocales(listOf(ptPt, ptBr)))
        source.setActiveTargetLocale(ptBr)

        // When / Then
        source.activeTargetLocale() shouldBe ptBr
    }

    // Scenario: Un idioma destino activo guardado que ya no está disponible usa el primero disponible
    // (spec: user-settings)
    @Test
    fun `given pt-BR saved as active when pt-BR is no longer available then returns the first available`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        val source = newSource(store, availableLocales(listOf(ptPt, ptBr)))
        source.setActiveTargetLocale(ptBr)
        val sourceWithoutPtBr = newSource(store, availableLocales(listOf(ptPt)))

        // When / Then
        sourceWithoutPtBr.activeTargetLocale() shouldBe ptPt
    }

    // Scenario: Cambiar el idioma destino activo (spec: user-settings)
    @Test
    fun `given pt-PT active when selecting pt-BR then a later read returns pt-BR`() = runTest {
        // Given
        val store = newStore(freshFile(), backgroundScope)
        val source = newSource(store, availableLocales(listOf(ptPt, ptBr)))

        // When
        source.setActiveTargetLocale(ptBr)

        // Then
        source.activeTargetLocale() shouldBe ptBr
    }
}
