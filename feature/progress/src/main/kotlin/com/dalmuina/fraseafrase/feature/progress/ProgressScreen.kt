package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

/**
 * Pantalla de progreso sin estado (ADR-0006): pinta [uiState], sin acciones de usuario todavía (ver
 * [ProgressAction]). La conexión con el ViewModel está en [ProgressRoute].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProgressScreen(
    uiState: ProgressUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.progress_title)) }) },
    ) { innerPadding ->
        when (uiState) {
            ProgressUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is ProgressUiState.Content -> ContentState(
                state = uiState,
                modifier = Modifier.padding(innerPadding),
            )
            ProgressUiState.CatalogError -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.progress_catalog_error))
            }
        }
    }
}

@Composable
private fun ContentState(state: ProgressUiState.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
    ) {
        val certifiedInBatch = state.certifiedInBatch
        Text(
            text = if (certifiedInBatch != null) {
                stringResource(
                    R.string.progress_certified_format,
                    certifiedInBatch.certified,
                    certifiedInBatch.total,
                )
            } else {
                stringResource(R.string.progress_no_active_batch)
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s)) {
            for (value in 0..Level.MAX_LEVEL) {
                val count = state.byLevel[Level(value)] ?: 0
                Row(
                    modifier = Modifier.testTag("progress_level_row_$value"),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DsLevelBadge(value)
                    Text(
                        text = if (value == 0) {
                            stringResource(R.string.progress_no_rank_format, count)
                        } else {
                            stringResource(R.string.progress_phrases_count_format, count)
                        },
                    )
                }
            }
        }
    }
}
