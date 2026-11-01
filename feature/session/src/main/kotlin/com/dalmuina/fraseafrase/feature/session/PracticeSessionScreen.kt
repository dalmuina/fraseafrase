package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment

/**
 * Pantalla de práctica sin estado (ADR-0006): pinta [uiState] y reenvía las acciones con [onAction]. La conexión con el
 * ViewModel está en [PracticeSessionRoute].
 */
@Composable
internal fun PracticeSessionScreen(
    uiState: PracticeSessionUiState,
    onAction: (PracticeSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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

/** Frase actual, botones de revelar o autoevaluar, botón de cerrar y aviso de cierre si está abierto. */
@Composable
private fun PracticingContent(
    state: PracticeSessionUiState.Practicing,
    onAction: (PracticeSessionAction) -> Unit,
) {
    val current = state.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
    ) {
        if (current == null) {
            EmptyMessage(text = stringResource(R.string.practice_nothing_to_practice))
        } else {
            PhraseCard(item = current, revealed = state.revealed)
            if (state.revealed) {
                AssessmentButtons(enabled = !state.busy, onAction = onAction)
            } else {
                Button(onClick = { onAction(PracticeSessionAction.Reveal) }) {
                    Text(text = stringResource(R.string.practice_reveal))
                }
            }
        }
        TextButton(
            onClick = { onAction(PracticeSessionAction.RequestClose) },
            enabled = !state.busy,
        ) {
            Text(text = stringResource(R.string.practice_close))
        }
    }
    state.closeWarning?.let { count ->
        CloseWarningDialog(count = count, onAction = onAction)
    }
}

/** Tarjeta con la frase en español, la traducción solo si está revelada, la racha y la marca de sabida. */
@Composable
private fun PhraseCard(item: PracticeItem, revealed: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            Text(text = item.source, style = MaterialTheme.typography.headlineSmall)
            if (revealed) {
                Text(
                    text = item.target,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.practice_streak, item.streak.value),
                style = MaterialTheme.typography.labelLarge,
            )
            if (item.isKnown) {
                Text(
                    text = stringResource(R.string.practice_known),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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

/** Diálogo modal de cierre con el número de frases no certificadas que pasarán a la siguiente tanda (D4, D5). */
@Composable
private fun CloseWarningDialog(count: Int, onAction: (PracticeSessionAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(PracticeSessionAction.CancelClose) },
        title = { Text(text = stringResource(R.string.practice_close_title)) },
        text = { Text(text = pluralStringResource(R.plurals.practice_close_warning, count, count)) },
        confirmButton = {
            TextButton(onClick = { onAction(PracticeSessionAction.ConfirmClose) }) {
                Text(text = stringResource(R.string.practice_close_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(PracticeSessionAction.CancelClose) }) {
                Text(text = stringResource(R.string.practice_close_cancel))
            }
        },
    )
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}
