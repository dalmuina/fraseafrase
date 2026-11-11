package com.dalmuina.fraseafrase.feature.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.audio.PhrasePlayer
import com.dalmuina.fraseafrase.core.domain.audio.PlayPhraseAudio
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogLoadResult
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogPhrase
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.catalog.LanguageTag
import com.dalmuina.fraseafrase.core.domain.catalog.Topic
import com.dalmuina.fraseafrase.core.domain.practice.ActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.AnswerPhrase
import com.dalmuina.fraseafrase.core.domain.practice.GenerateSession
import com.dalmuina.fraseafrase.core.domain.practice.ObtainActiveBatch
import com.dalmuina.fraseafrase.core.domain.practice.ObtainPracticeSession
import com.dalmuina.fraseafrase.core.domain.practice.PracticeChange
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeSnapshot
import com.dalmuina.fraseafrase.core.domain.practice.RevertLastAnswer
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.settings.UserSettings
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.ComposeNextBatch
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseId
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.PhraseProgress
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Streak
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Tests instrumentados de [PracticeSessionRoute] (ADR-0007, design D4 de `add-settings-apply-flow`). Construye un
 * [PracticeSessionViewModel] real sobre fakes locales de los puertos del dominio, sin Hilt: el `Route` admite
 * sustituir su `viewModel` por defecto, así que no hace falta el grafo de producción para comprobar que vuelve a
 * leer la sesión al entrar en composición. No usa `:core:testing` porque es un módulo JVM puro (JUnit 5) que no se
 * declara como dependencia de `androidTest` (JUnit 4 instrumentado, ADR-0007/ADR-0014).
 */
class PracticeSessionRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Scenario: design D4 - la tanda no cambió, se conserva revealed y la posición
    @Test
    fun givenRevealedPhraseAndUnchangedBatch_whenRouteRecomposes_thenRevealedStateIsKept() {
        // Given: una tanda activa A, B, C; el Route montado y la frase A revelada
        val practice = TestPracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = practiceSessionViewModel(practice, catalogOf("A", "B", "C"))
        var mounted by mutableStateOf(true)
        composeTestRule.setContent {
            AppTheme {
                if (mounted) {
                    PracticeSessionRoute(viewModel = viewModel)
                }
            }
        }
        composeTestRule.onNodeWithText("Mostrar traducción").performClick()
        composeTestRule.onNodeWithText("Bom dia").assertIsDisplayed()

        // When: el Route se desmonta y se vuelve a montar sin que la tanda cambie en el repositorio. `waitForIdle()`
        // entre los dos cambios fuerza la recomposición que desmonta de verdad el Route antes de remontarlo: sin
        // ella, Compose puede coalescer las dos escrituras y nunca llegar a desmontarlo.
        mounted = false
        composeTestRule.waitForIdle()
        mounted = true

        // Then: la traducción revelada sigue mostrándose
        composeTestRule.onNodeWithText("Bom dia").assertIsDisplayed()
    }

    // Scenario: design D4 - la tanda cambió mientras el Route estaba desmontado
    @Test
    fun givenBatchChangedWhileUnmounted_whenRouteRemounts_thenTheNewBatchIsShown() {
        // Given: una tanda activa A, B, C; el Route montado y la frase A revelada
        val practice = TestPracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = practiceSessionViewModel(practice, catalogOf("A", "B", "C", "D", "E", "F"))
        var mounted by mutableStateOf(true)
        composeTestRule.setContent {
            AppTheme {
                if (mounted) {
                    PracticeSessionRoute(viewModel = viewModel)
                }
            }
        }
        composeTestRule.onNodeWithText("Mostrar traducción").performClick()
        composeTestRule.onNodeWithText("Bom dia").assertIsDisplayed()

        // When: el Route se desmonta, la tanda activa cambia a D, E, F, y se vuelve a montar. `waitForIdle()` tras
        // desmontar fuerza la recomposición que lo desmonta de verdad antes de cambiar la tanda y remontarlo.
        mounted = false
        composeTestRule.waitForIdle()
        runBlocking {
            practice.transact { snapshot -> PracticeChange(snapshot.withActiveBatch(listOf("D", "E", "F")), Unit) }
        }
        mounted = true

        // Then: se muestra la nueva tanda, sin revelar
        composeTestRule.onNodeWithText("Bom dia").assertDoesNotExist()
        composeTestRule.onNodeWithText("Mostrar traducción").assertIsDisplayed()
    }

    // Scenario: Doble toque en revertir solo revierte una vez (spec: practice-session, design D5); cobertura
    // de la tarea 2.9 a nivel de UI
    @Test
    fun givenRevertButtonVisible_whenTappedTwice_thenOnlyOneRevertIsAppliedByTheViewModel() {
        // Given: tanda A, B, C; A revelada y autoevaluada con acierto; el botón de revertir visible
        val practice = TestPracticeRepository(activeBatchSnapshot(order = listOf("A", "B", "C")))
        val viewModel = practiceSessionViewModel(practice, catalogOf("A", "B", "C"))
        composeTestRule.setContent {
            AppTheme {
                PracticeSessionRoute(viewModel = viewModel)
            }
        }
        composeTestRule.onNodeWithText("Mostrar traducción").performClick()
        composeTestRule.onNodeWithText("Acierto").performClick()
        val revertButton = composeTestRule.onNodeWithTag("practice_revert_last_answer_button")
        revertButton.assertIsDisplayed()

        // When: se pulsa el botón de revertir dos veces seguidas. El guard síncrono de design D5 oculta el botón
        // en cuanto se procesa el primer toque, así que el segundo puede coincidir con esa desaparición a mitad
        // de la inyección del gesto y fallar con "Failed to inject touch input" en vez de limitarse a no
        // encontrar el nodo; `runCatching` tolera ambos desenlaces, porque los dos demuestran que el segundo
        // toque no tiene efecto (la aserción real está en el orden final de la tanda, más abajo).
        revertButton.performClick()
        runCatching { revertButton.performClick() }
        composeTestRule.waitForIdle()

        // Then: solo se aplica una reversión, sin ningún efecto adicional ni excepción: la tanda vuelve a su
        // orden original
        val orderAfter = runBlocking { practice.read() }.activeBatch?.order?.map { it.value }
        assertEquals(listOf("A", "B", "C"), orderAfter)
    }

    private fun activeBatchSnapshot(order: List<String>): PracticeSnapshot = PracticeSnapshot(
        progress = order.map { phraseProgress(it) },
        activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
        sessionCount = 0,
    )

    private fun PracticeSnapshot.withActiveBatch(order: List<String>): PracticeSnapshot = copy(
        progress = progress +
            order.filter { id -> progress.none { it.id == PhraseId(id) } }.map { phraseProgress(it) },
        activeBatch = ActiveBatch(order = order.map { PhraseId(it) }, capacity = BatchSize(10)),
    )

    private fun phraseProgress(id: String): PhraseProgress = PhraseProgress(
        id = PhraseId(id),
        level = Level(0),
        streak = Streak(0),
        answeredInBatch = false,
        dueAtSession = 0,
    )

    private fun catalogOf(vararg ids: String): CatalogLoadResult.Loaded {
        val phrases: List<CatalogPhrase> = ids.map { id ->
            if (id == "A") {
                catalogPhrase(id = id, source = "Buenos días", target = "Bom dia")
            } else {
                catalogPhrase(id = id, source = "fuente-$id", target = "destino-$id")
            }
        }
        return CatalogLoadResult.Loaded(phrases)
    }

    private fun catalogPhrase(id: String, source: String, target: String): CatalogPhrase = CatalogPhrase(
        id = PhraseId(id),
        level = CefrLevel.A1,
        topic = Topic("saludos"),
        targetLocale = LanguageTag("pt-PT"),
        sourceLocale = LanguageTag("es-ES"),
        target = target,
        source = source,
    )

    /** ViewModel con los casos de uso reales sobre fakes locales de los puertos del dominio (sin Hilt). */
    private fun practiceSessionViewModel(
        practice: PracticeRepository,
        catalog: CatalogLoadResult,
    ): PracticeSessionViewModel {
        val catalogRepository = TestCatalogRepository(catalog)
        val settingsRepository = TestSettingsRepository()
        val composeNextBatch = ComposeNextBatch()
        val obtainActiveBatch = ObtainActiveBatch(
            composeNextBatch = composeNextBatch,
            catalogRepository = catalogRepository,
            settingsRepository = settingsRepository,
            practiceRepository = practice,
        )
        return PracticeSessionViewModel(
            obtainPracticeSession = ObtainPracticeSession(obtainActiveBatch, catalogRepository),
            answerPhrase = AnswerPhrase(practice),
            generateSession = GenerateSession(
                practiceRepository = practice,
                composeNextBatch = composeNextBatch,
                catalogRepository = catalogRepository,
                settingsRepository = settingsRepository,
            ),
            playPhraseAudio = PlayPhraseAudio(TestPhrasePlayer()),
            revertLastAnswer = RevertLastAnswer(practice),
        )
    }

    /** Fake local de [PracticeRepository], en memoria, serializado con un [Mutex] como [FakePracticeRepository]. */
    private class TestPracticeRepository(initial: PracticeSnapshot) : PracticeRepository {

        private val mutex = Mutex()
        private var state: PracticeSnapshot = initial

        override suspend fun read(): PracticeSnapshot = mutex.withLock { state }

        override suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T = mutex.withLock {
            val outcome = change(state)
            state = outcome.next
            outcome.result
        }
    }

    private class TestCatalogRepository(private val result: CatalogLoadResult) : CatalogRepository {
        override suspend fun phrases(): CatalogLoadResult = result
    }

    private class TestSettingsRepository(initial: UserSettings = UserSettings.DEFAULT) : SettingsRepository {
        private var settings: UserSettings = initial
        override suspend fun settings(): UserSettings = settings
        override suspend fun save(settings: UserSettings) {
            this.settings = settings
        }
    }

    private class TestPhrasePlayer : PhrasePlayer {
        override fun play(text: String, locale: LanguageTag) = Unit
    }
}
