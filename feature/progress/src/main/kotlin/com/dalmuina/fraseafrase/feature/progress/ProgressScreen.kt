package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.core.designsystem.component.DsHeroStat
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.component.DsProgressBar
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level

/**
 * Pantalla de progreso sin estado (ADR-0006): pinta [uiState], sin acciones de usuario todavía (ver
 * [ProgressAction]). La conexión con el ViewModel está en [ProgressRoute]. La cabecera deja la
 * `TopAppBar` de Material 3 por el mismo patrón de título grande sin barra propia que usa
 * `PracticeHeader` en `PracticeSessionScreen.kt` (design D7, fix-redesign-ui-mismatches).
 */
@Composable
internal fun ProgressScreen(
    uiState: ProgressUiState,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        ProgressUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        is ProgressUiState.Content -> ContentState(state = uiState, modifier = modifier)
        ProgressUiState.CatalogError -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.progress_catalog_error))
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
        Text(
            text = stringResource(R.string.progress_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        DsHeroStat(
            value = state.totalAvailable.toString(),
            label = stringResource(R.string.progress_available_label),
            secondaryLine = stringResource(R.string.progress_cefr_level_format, state.cefrLevel.name),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("progress_available_tile"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s)) {
            for (value in 0..Level.MAX_LEVEL) {
                val count = state.byLevel[Level(value)] ?: 0
                val proportion = if (state.totalAvailable > 0) count / state.totalAvailable.toFloat() else 0f
                Row(
                    modifier = Modifier.testTag("progress_level_row_$value"),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DsLevelBadge(value)
                    DsProgressBar(
                        progress = proportion,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("progress_level_bar_$value"),
                    )
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
