package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel

/**
 * Pantalla de ajustes sin estado (ADR-0006): pinta [uiState] y reenvía las acciones con [onAction]. La conexión
 * con el ViewModel está en [SettingsRoute]. Su `Scaffold` solo aporta el `topBar`: la confirmación de guardado se
 * muestra en el `Scaffold` de `:app`, que sobrevive al cambio de pestaña (design D6 revisada de
 * `add-settings-apply-flow`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is SettingsUiState.Loaded -> LoadedContent(
                state = uiState,
                onAction = onAction,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LoadedContent(
    state: SettingsUiState.Loaded,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
    ) {
        CefrLevelCard(
            selectedLevel = state.cefrLevel,
            onSelectLevel = { onAction(SettingsAction.SelectCefrLevel(it)) },
        )
        BatchSizeCard(
            batchSizeText = state.batchSizeText,
            isValid = state.isBatchSizeValid,
            onBatchSizeTextChange = { onAction(SettingsAction.ChangeBatchSizeText(it)) },
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            onClick = { onAction(SettingsAction.Save) },
            enabled = state.isBatchSizeValid,
        ) {
            Text(text = stringResource(R.string.settings_save))
        }
    }
}

/** Tarjeta de selección del nivel CEFR con un chip por nivel (mock "Ajustes"). */
@Composable
private fun CefrLevelCard(
    selectedLevel: CefrLevel,
    onSelectLevel: (CefrLevel) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.settings_cefr_level_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                CefrLevel.entries.forEach { level ->
                    val selected = level == selectedLevel
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selected,
                        onClick = { onSelectLevel(level) },
                        label = { Text(text = level.name) },
                        shape = MaterialTheme.shapes.medium,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
    }
}

/** Tarjeta del tamaño de tanda N, con un stepper +/- alrededor del texto editable (mock "Ajustes"). */
@Composable
private fun BatchSizeCard(
    batchSizeText: String,
    isValid: Boolean,
    onBatchSizeTextChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
        ) {
            Text(
                text = stringResource(R.string.settings_batch_size_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                OutlinedIconButton(
                    onClick = { onBatchSizeTextChange(steppedBatchSizeText(batchSizeText, -1)) },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(text = MINUS_SIGN, style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = batchSizeText,
                    onValueChange = onBatchSizeTextChange,
                    shape = MaterialTheme.shapes.medium,
                    isError = !isValid,
                    supportingText = {
                        if (!isValid) {
                            Text(text = stringResource(R.string.settings_batch_size_error))
                        }
                    },
                )
                FilledIconButton(
                    onClick = { onBatchSizeTextChange(steppedBatchSizeText(batchSizeText, 1)) },
                    shape = MaterialTheme.shapes.medium,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

/** Resultado de pulsar +/- sobre el texto en edición: solo cambia si ya es un entero; si no, lo deja intacto. */
private fun steppedBatchSizeText(batchSizeText: String, delta: Int): String =
    batchSizeText.toIntOrNull()?.let { (it + delta).toString() } ?: batchSizeText

// Signo menos tipográfico (no el guion ASCII) para el botón de decremento, como en el mock "Ajustes"; no hay
// icono de "quitar" en material-icons-core (solo en la versión extended, no disponible en esta feature).
private const val MINUS_SIGN = "−"
