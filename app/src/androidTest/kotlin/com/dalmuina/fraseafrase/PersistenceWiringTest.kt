package com.dalmuina.fraseafrase

import com.dalmuina.fraseafrase.core.data.catalog.CatalogDatabase
import com.dalmuina.fraseafrase.core.data.practice.PracticeDatabase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.DefaultSourceLocale
import com.dalmuina.fraseafrase.core.domain.catalog.DeviceLocaleProvider
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainCatalogPhrasesForActiveLanguage
import com.dalmuina.fraseafrase.core.domain.catalog.ObtainSourceLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainAvailableLanguages
import com.dalmuina.fraseafrase.core.domain.settings.SelectActiveLanguage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Tests de cableado (design.md D1 a D5): comprueban que el grafo de producción de `PersistenceModule` comparte una
 * única instancia de cada recurso con ámbito singleton. Usan el grafo real, sin módulos de prueba, y no ejecutan
 * consultas: solo inyectan y comparan referencias.
 */
@HiltAndroidTest
class PersistenceWiringTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var firstCatalog: CatalogRepository

    @Inject
    lateinit var secondCatalog: CatalogRepository

    @Inject
    lateinit var firstDatabase: PracticeDatabase

    @Inject
    lateinit var secondDatabase: PracticeDatabase

    @Inject
    lateinit var firstCatalogDatabase: CatalogDatabase

    @Inject
    lateinit var secondCatalogDatabase: CatalogDatabase

    @Inject
    lateinit var deviceLocaleProvider: DeviceLocaleProvider

    @Inject
    lateinit var obtainCatalogPhrasesForActiveLanguage: ObtainCatalogPhrasesForActiveLanguage

    @Inject
    lateinit var obtainAvailableLanguages: ObtainAvailableLanguages

    @Inject
    lateinit var obtainActiveLanguage: ObtainActiveLanguage

    @Inject
    lateinit var selectActiveLanguage: SelectActiveLanguage

    @Inject
    lateinit var obtainSourceLanguage: ObtainSourceLanguage

    @Inject
    lateinit var defaultSourceLocale: DefaultSourceLocale

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // Cada test crea un grafo nuevo que abre su propia base de datos sobre el mismo fichero. Se
    // cierran ambas referencias: si la mutación rompe el singleton hay dos instancias abiertas,
    // y cerrar dos veces es inocuo en Room.
    @After
    fun tearDown() {
        firstDatabase.close()
        secondDatabase.close()
        firstCatalogDatabase.close()
        secondCatalogDatabase.close()
    }

    // Escenario: Dos obtenciones del catálogo desde el grafo de producción devuelven la misma instancia
    // (requisito "Existe una única instancia de `CatalogRepository` en el grafo de la aplicación", phrase-catalog).
    @Test
    fun givenProductionGraph_whenInjectingCatalogTwice_thenBothReferencesAreTheSameInstance() {
        assertSame(firstCatalog, secondCatalog)
    }

    // Escenario: Dos obtenciones de la base de datos desde el grafo de producción devuelven la
    // misma instancia (requisito "Existe una única instancia de `PracticeDatabase` en el grafo
    // de la aplicación", practice-persistence).
    @Test
    fun givenProductionGraph_whenObtainingDatabaseTwice_thenBothReferencesAreTheSameInstance() {
        assertSame(firstDatabase, secondDatabase)
    }

    // Escenario: Dos obtenciones de `CatalogDatabase` desde el grafo de producción devuelven la
    // misma instancia (requisito "Existe una única instancia de `CatalogRepository`" extendido a
    // `RoomCatalogDataSource`, phrase-catalog).
    @Test
    fun givenProductionGraph_whenObtainingCatalogDatabaseTwice_thenBothReferencesAreTheSameInstance() {
        assertSame(firstCatalogDatabase, secondCatalogDatabase)
    }

    // Escenario: el grafo de producción resuelve `DeviceLocaleProvider` y los casos de uso nuevos
    // sin ciclos (phrase-catalog, design "Nuevo puerto DeviceLocaleProvider...").
    @Test
    fun givenProductionGraph_whenInjectingNewLanguagePorts_thenAllAreResolved() {
        assertNotNull(deviceLocaleProvider)
        assertNotNull(obtainCatalogPhrasesForActiveLanguage)
        assertNotNull(obtainAvailableLanguages)
        assertNotNull(obtainActiveLanguage)
        assertNotNull(selectActiveLanguage)
    }

    // Escenario: El idioma del dispositivo no está disponible (phrase-catalog): el grafo de producción
    // resuelve ObtainSourceLanguage y el idioma de origen por defecto es es-ES.
    @Test
    fun givenProductionGraph_whenInjectingSourceLanguage_thenDefaultSourceLocaleIsEsEs() {
        assertNotNull(obtainSourceLanguage)
        assertEquals(LanguageTag("es-ES"), defaultSourceLocale.value)
    }
}
