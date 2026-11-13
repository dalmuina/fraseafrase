package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppCornerShapes
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.BatchSize
import com.dalmuina.fraseafrase.core.designsystem.R as DesignSystemR

/**
 * Pantalla de ajustes sin estado (ADR-0006): pinta [uiState] y reenvía las acciones con [onAction]. La conexión
 * con el ViewModel está en [SettingsRoute]. La confirmación de guardado se muestra en el `Scaffold` de `:app`,
 * que sobrevive al cambio de pestaña (design D6 revisada de `add-settings-apply-flow`). La cabecera deja la
 * `TopAppBar` de Material 3 por el mismo patrón de título grande sin barra propia que usa `PracticeHeader` en
 * `PracticeSessionScreen.kt` (design D7, fix-redesign-ui-mismatches).
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        SettingsUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        is SettingsUiState.Loaded -> LoadedContent(
            state = uiState,
            onAction = onAction,
            modifier = modifier,
        )
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
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
        )
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
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.dimensions.primaryActionHeight)
                .testTag("settings_save_button"),
            shape = MaterialTheme.shapes.medium,
            onClick = { onAction(SettingsAction.Save) },
            enabled = state.isBatchSizeValid,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_check_save),
                contentDescription = null,
                modifier = Modifier
                    .size(ButtonDefaults.IconSize)
                    .testTag("settings_save_icon"),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.settings_save))
        }
    }
}

/**
 * Tarjeta de selección del nivel CEFR con una rejilla de cinco botones propios (mock "Ajustes", 52dp de
 * alto, radio [AppCornerShapes.Hint]), en vez de `SingleChoiceSegmentedButtonRow`/`SegmentedButton` de
 * Material 3: su forma de píldora conectada no coincide con la rejilla de botones independientes del mock
 * (design.md decisión 4, adopt-reference-mock-ui). El contenedor aplica `Modifier.selectableGroup()` y
 * cada botón expone `role = Role.RadioButton` mediante `Modifier.selectable()`, para no perder la
 * semántica accesible de grupo de radio que `SegmentedButtonRow` ya daba (design.md - Risks/Trade-offs).
 */
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
            Text(
                text = stringResource(R.string.settings_cefr_level_help),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                CefrLevel.entries.forEach { level ->
                    CefrLevelButton(
                        level = level,
                        selected = level == selectedLevel,
                        onClick = { onSelectLevel(level) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Un botón de la rejilla de nivel CEFR (mock "Ajustes"): 52dp de alto, borde 2dp, fondo/borde/texto en
 * `accent`/blanco cuando está seleccionado, y `track`/`textOnLevel` cuando no lo está. */
@Composable
private fun CefrLevelButton(
    level: CefrLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else AppColors.Track
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else AppColors.TextOnLevel
    Box(
        modifier = modifier
            .height(AppTheme.dimensions.settingsControlHeight)
            .clip(AppCornerShapes.Hint)
            .background(containerColor)
            .border(BorderStroke(CEFR_LEVEL_BORDER_WIDTH, borderColor), AppCornerShapes.Hint)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.name,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
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
            Text(
                text = stringResource(R.string.settings_batch_size_help),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
            )
            val currentBatchSize = batchSizeText.toIntOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                DecreaseBatchSizeButton(
                    enabled = currentBatchSize != null && currentBatchSize > BatchSize.MIN,
                    onClick = { onBatchSizeTextChange(steppedBatchSizeText(batchSizeText, -1)) },
                )
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
                IncreaseBatchSizeButton(
                    enabled = currentBatchSize != null && currentBatchSize < BatchSize.MAX,
                    onClick = { onBatchSizeTextChange(steppedBatchSizeText(batchSizeText, 1)) },
                )
            }
        }
    }
}

/** Botón de disminuir N (design D5), con el alto de control de Ajustes y deshabilitado en [BatchSize.MIN]. */
@Composable
private fun DecreaseBatchSizeButton(enabled: Boolean, onClick: () -> Unit) {
    BatchSizeStepButton(
        enabled = enabled,
        onClick = onClick,
        description = stringResource(R.string.settings_batch_size_decrease),
    ) {
        Text(text = MINUS_SIGN, style = MaterialTheme.typography.titleLarge)
    }
}

/** Botón de aumentar N (design D5), con el alto de control de Ajustes y deshabilitado en [BatchSize.MAX]. */
@Composable
private fun IncreaseBatchSizeButton(enabled: Boolean, onClick: () -> Unit) {
    BatchSizeStepButton(
        enabled = enabled,
        onClick = onClick,
        description = stringResource(R.string.settings_batch_size_increase),
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
    }
}

/**
 * Botón +/- compartido del stepper de tamaño de tanda (mock "Ajustes", design.md decisión 2,
 * adopt-reference-mock-ui): 52dp de alto ([AppTheme.dimensions.settingsControlHeight]), sin borde,
 * fondo/texto `accentSoft`/`accentStrong` cuando está habilitado y `trackSoft`/`disabled` cuando no.
 */
@Composable
private fun BatchSizeStepButton(
    enabled: Boolean,
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit,
) {
    FilledIconButton(
        modifier = Modifier
            .size(AppTheme.dimensions.settingsControlHeight)
            .semantics { contentDescription = description },
        onClick = onClick,
        enabled = enabled,
        shape = AppCornerShapes.Hint,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = AppColors.AccentSoft,
            contentColor = AppColors.AccentStrong,
            disabledContainerColor = AppColors.TrackSoft,
            disabledContentColor = AppColors.Disabled,
        ),
        content = content,
    )
}

/** Resultado de pulsar +/- sobre el texto en edición: solo cambia si ya es un entero; si no, lo deja intacto. */
private fun steppedBatchSizeText(batchSizeText: String, delta: Int): String =
    batchSizeText.toIntOrNull()?.let { (it + delta).toString() } ?: batchSizeText

// Signo menos tipográfico (no el guion ASCII) para el botón de decremento, como en el mock "Ajustes"; no hay
// icono de "quitar" en material-icons-core (solo en la versión extended, no disponible en esta feature).
private const val MINUS_SIGN = "−"

// Borde de los botones de nivel CEFR: 2dp, como en `design/mock/screens/05-ajustes.html`.
private val CEFR_LEVEL_BORDER_WIDTH = 2.dp
