package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dalmuina.fraseafrase.core.designsystem.component.DsProgressBar
import com.dalmuina.fraseafrase.core.designsystem.component.DsStatTile
import com.dalmuina.fraseafrase.core.designsystem.component.DsStatTileSize
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
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
    onNavigateToProgress: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState is PracticeSessionUiState.Practicing && !uiState.completed) {
                PracticeHeader(index = uiState.index, total = uiState.total)
            }
        },
        floatingActionButton = {
            if (uiState is PracticeSessionUiState.Practicing && uiState.canRevertLastAnswer) {
                RevertLastAnswerButton(onAction = onAction)
            }
        },
        floatingActionButtonPosition = FabPosition.Start,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(AppTheme.spacing.s),
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
                is PracticeSessionUiState.Practicing ->
                    PracticingContent(uiState, onAction, onNavigateToProgress)
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
        DsProgressBar(
            progress = index / total.toFloat(),
            modifier = Modifier.testTag("practice_progress_indicator"),
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
    onNavigateToProgress: () -> Unit,
) {
    if (state.completed) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
        ) {
            CompletedMessage(
                hits = state.hits,
                misses = state.misses,
                levelUps = state.levelUps,
                enabled = !state.busy,
                onAction = onAction,
                onNavigateToProgress = onNavigateToProgress,
            )
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
                RevealButton(onAction = onAction)
            }
        }
    }
}

/**
 * Botón para revelar la traducción, con icono y 56dp de alto (design D8, fix-redesign-ui-mismatches).
 *
 * Usa [Icons.Default.Search] en vez del ojo (`Icons.Default.Visibility`) previsto en el diseño porque
 * ese icono solo está en `material-icons-extended`, que `:feature:session` no declara como dependencia
 * (igual que `ListenButton` en `PracticeSessionPhraseCard.kt` sustituyó `VolumeUp` por `PlayArrow`);
 * `Search` ya está disponible en `material-icons-core` y es la metáfora más cercana a "revelar/ver
 * contenido oculto" sin añadir esa dependencia.
 */
@Composable
private fun RevealButton(onAction: (PracticeSessionAction) -> Unit) {
    Button(
        onClick = { onAction(PracticeSessionAction.Reveal) },
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.primaryActionHeight),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier
                .size(ButtonDefaults.IconSize)
                .testTag("practice_reveal_icon"),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.practice_reveal))
    }
}

/**
 * Mensaje positivo al completar una vuelta a la tanda (D7), con el desglose de aciertos, fallos y subidas
 * de nivel de esa misma tanda (spec: practice-session, "El mensaje muestra el desglose de aciertos,
 * fallos y subidas de nivel"), el botón para generar la siguiente sesión y un enlace a Progreso.
 */
@Composable
private fun CompletedMessage(
    hits: Int,
    misses: Int,
    levelUps: Int,
    enabled: Boolean,
    onAction: (PracticeSessionAction) -> Unit,
    onNavigateToProgress: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(AppTheme.spacing.xl + AppTheme.spacing.xl)
            .testTag("practice_completed_success_icon"),
    )
    Text(
        text = stringResource(R.string.practice_batch_completed),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.practice_batch_completed_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        DsStatTile(
            value = hits.toString(),
            label = stringResource(R.string.practice_outcome_hits),
            size = DsStatTileSize.Compact,
            modifier = Modifier.weight(1f),
        )
        DsStatTile(
            value = misses.toString(),
            label = stringResource(R.string.practice_outcome_misses),
            size = DsStatTileSize.Compact,
            modifier = Modifier.weight(1f),
        )
        DsStatTile(
            value = levelUps.toString(),
            label = stringResource(R.string.practice_outcome_level_ups),
            size = DsStatTileSize.Compact,
            modifier = Modifier.weight(1f),
        )
    }
    GenerateSessionButton(enabled = enabled, onAction = onAction)
    TextButton(onClick = onNavigateToProgress) {
        Text(
            text = stringResource(R.string.practice_view_progress),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Botón para generar la siguiente sesión, con icono y 56dp de alto (design D8, fix-redesign-ui-mismatches). */
@Composable
private fun GenerateSessionButton(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Button(
        onClick = { onAction(PracticeSessionAction.GenerateSession) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.primaryActionHeight),
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier
                .size(ButtonDefaults.IconSize)
                .testTag("practice_generate_session_icon"),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.practice_generate_session))
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
            modifier = Modifier
                .weight(1f)
                .height(AppTheme.dimensions.primaryActionHeight),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier
                    .size(ButtonDefaults.IconSize)
                    .testTag("practice_miss_icon"),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.practice_miss))
        }
        Button(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.HIT)) },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            modifier = Modifier
                .weight(1f)
                .height(AppTheme.dimensions.primaryActionHeight),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .size(ButtonDefaults.IconSize)
                    .testTag("practice_hit_icon"),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
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
