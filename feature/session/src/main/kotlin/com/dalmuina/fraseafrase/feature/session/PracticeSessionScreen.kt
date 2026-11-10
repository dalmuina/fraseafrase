package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadgeChange
import com.dalmuina.fraseafrase.core.designsystem.motion.Motion
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import kotlinx.coroutines.delay

/**
 * Pantalla de práctica sin estado (ADR-0006): pinta [uiState] y reenvía las acciones con [onAction]. La conexión con el
 * ViewModel está en [PracticeSessionRoute].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PracticeSessionScreen(
    uiState: PracticeSessionUiState,
    onAction: (PracticeSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState is PracticeSessionUiState.Practicing && !uiState.completed) {
                PracticeHeader(index = uiState.index, total = uiState.total)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(AppTheme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                PracticeSessionUiState.Loading -> CircularProgressIndicator()
                PracticeSessionUiState.NothingToPractice -> EmptyMessage(
                    text = stringResource(R.string.practice_nothing_to_practice),
                )
                PracticeSessionUiState.CatalogError -> EmptyMessage(
                    text = stringResource(R.string.practice_catalog_error),
                )
                is PracticeSessionUiState.Practicing -> PracticingContent(uiState, onAction)
            }
        }
    }
}

/**
 * Cabecera fija de Practicar: título, píldora de posición en la tanda y barra de progreso (spec:
 * practice-session, "Posición en la tanda visible"; design D1 de `fix-style-pass-two`).
 */
@Composable
private fun PracticeHeader(index: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.practice_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.practice_position, index, total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                )
            }
        }
        LinearProgressIndicator(
            progress = { index / total.toFloat() },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("practice_progress_indicator"),
        )
    }
}

/**
 * Frase actual con sus botones de revelar o autoevaluar, o el mensaje de tanda completada en su lugar.
 *
 * La frase y la traducción viven en una zona desplazable con [Modifier.weight] para que, si no caben en
 * el espacio disponible, el usuario pueda desplazarlas; el control de revelar o los de acierto/fallo
 * quedan fuera de esa zona, al final de la columna, para que nunca se recorten ni se muevan (spec:
 * practice-session, "Controles de acción siempre visibles y pulsables"; design D2).
 */
@Composable
private fun PracticingContent(
    state: PracticeSessionUiState.Practicing,
    onAction: (PracticeSessionAction) -> Unit,
) {
    if (state.completed) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
        ) {
            CompletedMessage(enabled = !state.busy, onAction = onAction)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("practice_scrollable_content"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
            ) {
                PhraseCard(
                    item = state.current,
                    revealed = state.revealed,
                    enabled = !state.busy,
                    remainingHitsForNextLevel = state.remainingHitsForNextLevel,
                    nextLevelBadgeLabel = state.nextLevelBadgeLabel,
                    badgeChange = state.badgeChange,
                    onAction = onAction,
                )
            }
            if (state.revealed) {
                AssessmentButtons(enabled = !state.busy, onAction = onAction)
            } else {
                Button(
                    onClick = { onAction(PracticeSessionAction.Reveal) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.practice_reveal))
                }
            }
        }
    }
}

/**
 * Indicador animado de cambio de insignia (spec: add-level-change-animation), en el sitio de
 * [DsLevelBadge] dentro de la fila de nivel CEFR de [PhraseCard]: la insignia de la propia frase
 * respondida transiciona ahí, sin ocultar el resto de la tarjeta (spec: new-badge-animation, design,
 * Decisión 2). Se oculta sola, pasada la duración correspondiente a su [BadgeChange.direction], sin
 * bloquear los controles de esa misma frase, que siguen presentes (solo deshabilitados por `busy`).
 */
@Composable
private fun BadgeChangeIndicator(
    badgeChange: BadgeChange,
    onAction: (PracticeSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AppTheme.motion
    LaunchedEffect(badgeChange) {
        delay(badgeChange.direction.visibleDurationMillis(motion))
        onAction(PracticeSessionAction.ConsumeBadgeChange)
    }
    DsLevelBadgeChange(
        fromLevel = badgeChange.fromLevel,
        toLevel = badgeChange.toLevel,
        emphasized = badgeChange.direction == BadgeChange.Direction.UP,
        modifier = modifier.testTag("practice_badge_change_indicator"),
    )
}

/**
 * Duración total que el indicador permanece visible antes de consumirse, acorde a los mismos tokens de
 * [Motion] con los que [DsLevelBadgeChange] resuelve su propia animación (design.md D4): más larga para
 * una subida que para una bajada.
 */
private fun BadgeChange.Direction.visibleDurationMillis(motion: Motion): Long = when (this) {
    BadgeChange.Direction.UP ->
        (motion.durationShortMillis + motion.durationLongMillis + motion.durationMediumMillis).toLong()
    BadgeChange.Direction.DOWN ->
        (motion.durationMediumMillis + motion.durationShortMillis + motion.durationMediumMillis).toLong()
}

/** Mensaje positivo al completar una vuelta a la tanda, con el botón para generar la siguiente sesión (D7). */
@Composable
private fun CompletedMessage(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Text(
        text = stringResource(R.string.practice_batch_completed),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Button(
        onClick = { onAction(PracticeSessionAction.GenerateSession) },
        enabled = enabled,
    ) {
        Text(text = stringResource(R.string.practice_generate_session))
    }
}

/**
 * Tarjeta con la frase en español, la traducción y el control de escuchar solo si está revelada, y el contador de
 * aciertos que faltan para la insignia del siguiente nivel (spec: practice-session).
 */
@Composable
private fun PhraseCard(
    item: PracticeItem,
    revealed: Boolean,
    enabled: Boolean,
    remainingHitsForNextLevel: Int,
    nextLevelBadgeLabel: String,
    badgeChange: BadgeChange?,
    onAction: (PracticeSessionAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = item.cefrLevel.name, style = MaterialTheme.typography.labelMedium)
                    if (badgeChange != null) {
                        BadgeChangeIndicator(badgeChange = badgeChange, onAction = onAction)
                    } else {
                        DsLevelBadge(level = item.level.value)
                    }
                }
                Text(text = item.source, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(
                        R.string.practice_remaining_hits_for_next_level,
                        remainingHitsForNextLevel,
                        nextLevelBadgeLabel,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (revealed) {
            TranslationCard(target = item.target, enabled = enabled, onAction = onAction)
        }
    }
}

/** Tarjeta de la traducción revelada, con el control de escuchar (spec: practice-session, phrase-audio). */
@Composable
private fun TranslationCard(
    target: String,
    enabled: Boolean,
    onAction: (PracticeSessionAction) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            Text(
                text = target,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(
                onClick = { onAction(PracticeSessionAction.PlayAudio) },
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.testTag("listen_icon"),
                    )
                    Text(text = stringResource(R.string.practice_listen))
                }
            }
        }
    }
}

/** Acierto y fallo para la frase revelada (ADR-0012). Se deshabilitan mientras hay una respuesta en curso. */
@Composable
private fun AssessmentButtons(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
    ) {
        OutlinedButton(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.MISS)) },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(
                width = ButtonDefaults.outlinedButtonBorder(enabled).width,
                color = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.practice_miss))
        }
        Button(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.HIT)) },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.practice_hit))
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}
