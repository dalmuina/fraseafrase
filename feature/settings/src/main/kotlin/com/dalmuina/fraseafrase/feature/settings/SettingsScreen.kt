package com.dalmuina.fraseafrase.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel

/**
 * Pantalla de ajustes sin estado (ADR-0006): pinta [uiState] y reenvía las acciones con [onAction]. La conexión
 * con el ViewModel está en [SettingsRoute].
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

@OptIn(ExperimentalMaterial3Api::class)
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
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                value = state.cefrLevel.name,
                onValueChange = {},
                label = { Text(text = stringResource(R.string.settings_cefr_level_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                CefrLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = { Text(text = level.name) },
                        onClick = {
                            expanded = false
                            onAction(SettingsAction.SelectCefrLevel(level))
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.batchSizeText,
            onValueChange = { onAction(SettingsAction.ChangeBatchSizeText(it)) },
            label = { Text(text = stringResource(R.string.settings_batch_size_label)) },
            isError = !state.isBatchSizeValid,
            supportingText = {
                if (!state.isBatchSizeValid) {
                    Text(text = stringResource(R.string.settings_batch_size_error))
                }
            },
        )

        Button(
            onClick = { onAction(SettingsAction.Save) },
            enabled = state.isBatchSizeValid,
        ) {
            Text(text = stringResource(R.string.settings_save))
        }
    }
}
