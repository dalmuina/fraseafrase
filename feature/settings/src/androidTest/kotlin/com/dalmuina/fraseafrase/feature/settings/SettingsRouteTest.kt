package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.ForceCloseActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.settings.ObtainUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SaveUserSettings
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.CarryQueue
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ResolveBatchClosure
import com.dalmuina.fraseafrase.core.domain.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Tests instrumentados de [SettingsRoute] (ADR-0007, design D5 de `add-settings-apply-flow`). Construye un
 * [SettingsViewModel] real sobre fakes locales de los puertos del dominio, sin Hilt: el `Route` admite sustituir
 * su `viewModel` por defecto, así que no hace falta el grafo de producción para comprobar la navegación a
 * práctica. No usa `:core:testing` porque es un módulo JVM puro (JUnit 5) que no se declara como dependencia de
 * `androidTest` (JUnit 4 instrumentado, ADR-0007/ADR-0014).
 */
class SettingsRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: Guardar con el nivel CEFR cambiado navega a práctica (spec: settings-screen)
    @Test
    fun givenCefrLevelChanged_whenSaving_thenOnNavigateToPracticeIsCalled() {
        // Given: ajustes con nivel CEFR A1 persistido, y la pantalla montada con un callback de navegación
        val repository = TestSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository)
        var navigated = false
        composeTestRule.setContent {
            SettingsRoute(viewModel = viewModel, onNavigateToPractice = { navigated = true })
        }
        composeTestRule.waitForIdle()

        // When: el usuario elige el nivel B1 y confirma el guardado
        viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))
        viewModel.onAction(SettingsAction.Save)

        // Then: se llama a onNavigateToPractice
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) { navigated }
        assertTrue(navigated)
    }

    // Scenario: Confirmación tras guardar, combinado con "Guardar con el nivel CEFR cambiado navega a práctica"
    // (spec: settings-screen) — design D6 (revisada): la confirmación no depende de que ningún showSnackbar
    // termine, así que no se pierde aunque la navegación saque a SettingsRoute de composición en el mismo frame.
    @Test
    fun givenCefrLevelChanged_whenSaving_thenOnShowSaveConfirmationIsCalledWithTheConfirmationText() {
        // Given: ajustes con nivel CEFR A1 persistido, y la pantalla montada con un callback de confirmación
        val repository = TestSettingsRepository(UserSettings(CefrLevel.A1, BatchSize(20)))
        val viewModel = settingsViewModel(repository)
        var confirmationMessage: String? = null
        composeTestRule.setContent {
            SettingsRoute(
                viewModel = viewModel,
                onShowSaveConfirmation = { message -> confirmationMessage = message },
            )
        }
        composeTestRule.waitForIdle()

        // When: el usuario elige el nivel B1 y confirma el guardado
        viewModel.onAction(SettingsAction.SelectCefrLevel(CefrLevel.B1))
        viewModel.onAction(SettingsAction.Save)

        // Then: se llama a onShowSaveConfirmation con el texto de confirmación, sin esperar a ningún snackbar
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) { confirmationMessage != null }
        assertEquals("Ajustes guardados", confirmationMessage)
    }

    private fun settingsViewModel(repository: SettingsRepository): SettingsViewModel {
        val clock = TestClock(LocalDate.of(2026, 3, 10))
        return SettingsViewModel(
            obtainUserSettings = ObtainUserSettings(repository),
            saveUserSettings = SaveUserSettings(repository),
            forceCloseActiveBatch = ForceCloseActiveBatch(TestPracticeRepository(), ResolveBatchClosure(clock)),
        )
    }

    /** Fake local de [SettingsRepository]: devuelve los ajustes configurados y recuerda el último guardado. */
    private class TestSettingsRepository(initial: UserSettings) : SettingsRepository {
        private var settings: UserSettings = initial
        override suspend fun settings(): UserSettings = settings
        override suspend fun save(settings: UserSettings) {
            this.settings = settings
        }
    }

    /** Fake local de [PracticeRepository], sin tanda activa: basta para que [ForceCloseActiveBatch] no haga nada. */
    private class TestPracticeRepository : PracticeRepository {
        private val mutex = Mutex()
        private var state = PracticeSnapshot(
            progress = emptyList(),
            activeBatch = null,
            carry = CarryQueue(emptyList()),
        )

        override suspend fun read(): PracticeSnapshot = mutex.withLock { state }

        override suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T = mutex.withLock {
            val outcome = change(state)
            state = outcome.next
            outcome.result
        }
    }

    private class TestClock(private val date: LocalDate) : Clock {
        override fun today(): LocalDate = date
    }

    private companion object {
        const val WAIT_TIMEOUT_MILLIS = 5_000L
    }
}
