package com.dalmuina.fraseafrase.feature.progress

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dalmuina.fraseafrase.core.designsystem.component.DsHeroStat
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.component.DsProgressBar
import com.dalmuina.fraseafrase.core.designsystem.component.levelBadgeAccentColor
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import com.dalmuina.fraseafrase.core.designsystem.R as DsR

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
            decoration = { HeroDecoration() },
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
                        color = levelBadgeAccentColor(value) ?: MaterialTheme.colorScheme.primary,
                        trackColor = AppColors.TrackSoft,
                        modifier = Modifier
                            .weight(1f)
                            .height(AppTheme.dimensions.rankBarHeight)
                            .testTag("progress_level_bar_$value"),
                    )
                    Text(
                        text = if (value == 0) {
                            stringResource(R.string.progress_no_rank_format, count)
                        } else {
                            stringResource(R.string.progress_phrases_count_format, count)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = RankRowCountFontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

// Cifra de cada fila de rango (design/mock/screens/04-progreso.html): 18sp/700, un tamaño y peso
// propios de esta fila sin rol equivalente en MaterialTheme.typography, aplicados con `.copy()`
// sobre titleMedium (familia Inter) como ya documenta Type.kt para variaciones locales de un
// mismo slot (adopt-reference-mock-ui, tasks 8.2).
private val RankRowCountFontSize = 18.sp

// Opacidad de los hexágonos decorativos de la tarjeta destacada, igual que el mock
// (design/mock/screens/04-progreso.html).
private const val HERO_DECORATION_LARGE_HEX_ALPHA = 0.18f
private const val HERO_DECORATION_SMALL_HEX_ALPHA = 0.9f

/**
 * Hexágonos decorativos de fondo de la tarjeta destacada de Progreso (adopt-reference-mock-ui,
 * tasks 8.1): uno grande semitransparente que desborda la esquina superior derecha y uno pequeño
 * con el color de acento que desborda la esquina inferior derecha, igual que
 * `design/mock/screens/04-progreso.html`. `Modifier.matchParentSize()` evita que su propio
 * tamaño, mayor que la tarjeta, la agrande: [DsHeroStat] recorta el contenido a su forma, igual
 * que el `overflow: hidden` del mock.
 */
@Composable
private fun BoxScope.HeroDecoration() {
    // El Box interno usa matchParentSize() (tamaño igual al de DsHeroStat, sin verse afectado por
    // sus propios hijos) para que los hexágonos, con tamaño propio mayor que la tarjeta, no la
    // agranden; DsHeroStat recorta lo que sobresalga con la forma de la tarjeta.
    Box(modifier = Modifier.matchParentSize()) {
        Image(
            painter = painterResource(DsR.drawable.ic_hex_solid),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = HERO_DECORATION_LARGE_HEX_ALPHA)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = AppTheme.dimensions.heroDecorationLargeHexOverflowX,
                    y = -AppTheme.dimensions.heroDecorationLargeHexOverflowY,
                )
                .size(AppTheme.dimensions.heroDecorationLargeHexSize)
                .testTag("progress_hero_decoration_large_hex"),
        )
        Image(
            painter = painterResource(DsR.drawable.ic_hex_solid),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.secondary.copy(alpha = HERO_DECORATION_SMALL_HEX_ALPHA),
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = -AppTheme.dimensions.heroDecorationSmallHexInsetX,
                    y = AppTheme.dimensions.heroDecorationSmallHexOverflowY,
                )
                .size(AppTheme.dimensions.heroDecorationSmallHexSize)
                .testTag("progress_hero_decoration_small_hex"),
        )
    }
}
