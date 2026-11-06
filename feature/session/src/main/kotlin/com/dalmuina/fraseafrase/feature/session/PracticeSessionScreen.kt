package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment

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
        topBar = { TopAppBar(title = {}) },
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

/** Frase actual con sus botones de revelar o autoevaluar, o el mensaje de tanda completada en su lugar. */
@Composable
private fun PracticingContent(
    state: PracticeSessionUiState.Practicing,
    onAction: (PracticeSessionAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
    ) {
        if (state.completed) {
            CompletedMessage(enabled = !state.busy, onAction = onAction)
        } else {
            Text(
                text = stringResource(R.string.practice_position, state.index, state.total),
                style = MaterialTheme.typography.labelLarge,
            )
            PhraseCard(
                item = state.current,
                revealed = state.revealed,
                enabled = !state.busy,
                remainingHitsForNextLevel = state.remainingHitsForNextLevel,
                nextLevelBadgeLabel = state.nextLevelBadgeLabel,
                onAction = onAction,
            )
            if (state.revealed) {
                AssessmentButtons(enabled = !state.busy, onAction = onAction)
            } else {
                Button(onClick = { onAction(PracticeSessionAction.Reveal) }) {
                    Text(text = stringResource(R.string.practice_reveal))
                }
            }
        }
    }
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
    onAction: (PracticeSessionAction) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = item.cefrLevel.name, style = MaterialTheme.typography.labelLarge)
                DsLevelBadge(level = item.level.value)
            }
            Text(text = item.source, style = MaterialTheme.typography.headlineSmall)
            if (revealed) {
                Text(
                    text = item.target,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    onClick = { onAction(PracticeSessionAction.PlayAudio) },
                    enabled = enabled,
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
}

/** Acierto y fallo para la frase revelada (ADR-0012). Se deshabilitan mientras hay una respuesta en curso. */
@Composable
private fun AssessmentButtons(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.m)) {
        OutlinedButton(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.MISS)) },
            enabled = enabled,
        ) {
            Text(text = stringResource(R.string.practice_miss))
        }
        Button(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.HIT)) },
            enabled = enabled,
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
