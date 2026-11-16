package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.settings.ObtainActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.ObtainAvailableLanguages
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SelectActiveLanguage
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests instrumentados de [SettingsRoute] (ADR-0007). Construye un [SettingsViewModel] real sobre fakes locales
 * de los puertos del dominio, sin Hilt: el `Route` admite sustituir su `viewModel` por defecto, así que no hace
 * falta el grafo de producción. No usa `:core:testing` porque es un módulo JVM puro (JUnit 5) que no se declara
 * como dependencia de `androidTest` (JUnit 4 instrumentado, ADR-0007/ADR-0014). Desde `remove-settings-save-button`
 * (design D3), no hay ninguna señal de un solo uso que observar: el autoguardado se comprueba leyendo el propio
 * repositorio de ajustes tras una acción, sin ninguna acción de salida adicional.
 */
class SettingsRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: Guardar persiste el nuevo nivel CEFR y N (spec: settings-screen, "Guardado de los ajustes editados")
    @Test
    fun givenCefrLevelChosen_whenDispatched_thenALaterReadReflectsItWithoutAnyExitAction() {
        // Given: ajustes con nivel CEFR A1 persistido, y la pantalla montada
        val repository = TestSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository)
        composeTestRule.setContent {
            SettingsRoute(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // When: el usuario elige el nivel B1, sin ninguna acción adicional
        viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))

        // Then: una lectura posterior de los ajustes devuelve el nivel CEFR B1 de inmediato
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runBlocking { repository.settings(LOCALE) }.cefrLevel == CefrLevel.B1
        }
        assertEquals(CefrLevel.B1, runBlocking { repository.settings(LOCALE) }.cefrLevel)
    }

    // Scenario: Elegir un idioma destino lo activa sin salir de ajustes (spec: settings-screen, "Elegir un
    // idioma destino lo activa de inmediato")
    @Test
    fun givenAvailableLanguages_whenSelectingAnotherOne_thenItIsActivatedImmediately() {
        // Given: pt-PT activo con pt-BR también disponible, y la pantalla montada
        val repository = TestSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository, availableTargetLocales = listOf(LOCALE, OTHER_LOCALE))
        composeTestRule.setContent {
            SettingsRoute(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        // When: el usuario elige pt-BR en el desplegable, sin salir de la pantalla de ajustes
        viewModel.onAction(SettingsAction.SelectLanguage(OTHER_LOCALE))

        // Then: una lectura posterior del idioma destino activo devuelve pt-BR
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runBlocking { repository.activeTargetLocale() } == OTHER_LOCALE
        }
        assertEquals(OTHER_LOCALE, runBlocking { repository.activeTargetLocale() })
    }

    private fun settingsViewModel(
        repository: SettingsRepository,
        availableTargetLocales: List<LanguageTag> = listOf(LOCALE),
    ): SettingsViewModel {
        val catalogRepository = TestCatalogRepository(availableTargetLocales)
        val obtainAvailableLanguages = ObtainAvailableLanguages(catalogRepository)
        return SettingsViewModel(
            obtainUserSettings = ObtainUserSettings(repository),
            saveUserSettings = SaveUserSettings(repository),
            obtainActiveLanguage = ObtainActiveLanguage(repository),
            obtainAvailableLanguages = obtainAvailableLanguages,
            selectActiveLanguage = SelectActiveLanguage(obtainAvailableLanguages, repository),
        )
    }

    /**
     * Fake local de [SettingsRepository]: devuelve los ajustes configurados por idioma destino y recuerda el
     * idioma destino activo y los últimos guardados. [appliedSettings]/[recordAppliedSettings] no se ejercitan
     * en este test (no entra en práctica), así que devuelven/ignoran sin más.
     */
    private class TestSettingsRepository(initial: UserSettings) : SettingsRepository {
        private val settingsByLocale = mutableMapOf(LOCALE to initial)
        private var activeTargetLocale: LanguageTag = LOCALE

        override suspend fun settings(targetLocale: LanguageTag): UserSettings =
            settingsByLocale[targetLocale] ?: UserSettings.DEFAULT

        override suspend fun save(targetLocale: LanguageTag, settings: UserSettings) {
            settingsByLocale[targetLocale] = settings
        }

        override suspend fun activeTargetLocale(): LanguageTag = activeTargetLocale

        override suspend fun setActiveTargetLocale(targetLocale: LanguageTag) {
            activeTargetLocale = targetLocale
        }

        override suspend fun appliedSettings(targetLocale: LanguageTag): UserSettings? = null

        override suspend fun recordAppliedSettings(targetLocale: LanguageTag, settings: UserSettings) = Unit
    }

    /** Fake local de [CatalogRepository]: solo expone los idiomas destino disponibles configurados, sin frases. */
    private class TestCatalogRepository(private val targetLocales: List<LanguageTag>) : CatalogRepository {
        override suspend fun availableTargetLocales(): List<LanguageTag> = targetLocales

        override suspend fun availableSourceLocales(targetLocale: LanguageTag): List<LanguageTag> = emptyList()

        override suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult =
            CatalogLoadResult.Loaded(emptyList())
    }

    private companion object {
        const val WAIT_TIMEOUT_MILLIS = 5_000L
        val LOCALE = LanguageTag("pt-PT")
        val OTHER_LOCALE = LanguageTag("pt-BR")
    }
}
