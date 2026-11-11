package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Tarjeta de recuento con un número grande ([value]) y una etiqueta ([label]) debajo, sin estado
 * ni tipos del dominio: recibe [value] ya formateado como [String] (ADR-0010, design D1), igual
 * que [DsLevelBadge] recibe un [Int] en vez del tipo `Level` del dominio.
 */
@Composable
fun DsStatTile(
    value: String,
    label: String,
    size: DsStatTileSize = DsStatTileSize.Compact,
    modifier: Modifier = Modifier
) {
    val valueStyle = statTileValueStyle(size)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(statTilePadding(size)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
        ) {
            Text(
                text = value,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun statTileValueStyle(size: DsStatTileSize): TextStyle = when (size) {
    DsStatTileSize.Compact -> MaterialTheme.typography.headlineLarge
    DsStatTileSize.Large -> MaterialTheme.typography.displayLarge
}

@Composable
private fun statTilePadding(size: DsStatTileSize) = when (size) {
    DsStatTileSize.Compact -> AppTheme.spacing.m
    DsStatTileSize.Large -> AppTheme.spacing.l
}

@PreviewLightDark
@Composable
private fun DsStatTileCompactPreview() {
    AppTheme {
        DsStatTile(value = "12", label = "Aciertos", size = DsStatTileSize.Compact)
    }
}

@PreviewLightDark
@Composable
private fun DsStatTileLargePreview() {
    AppTheme {
        DsStatTile(value = "248", label = "Frases disponibles", size = DsStatTileSize.Large)
    }
}
