package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.R
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Insignia visual de un nivel de consolidación (0 a 5, ADR-0012/ADR-0015). Recibe un [Int] en vez
 * del tipo `Level` del dominio, ya que `:core:designsystem` no depende de ningún módulo del
 * proyecto (ADR-0010, design D1). El nivel 0 pinta la insignia "Sin rango" (design D3).
 */
@Composable
fun DsLevelBadge(level: Int, modifier: Modifier = Modifier) {
    val spec = levelBadgeSpec(level) ?: return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
    ) {
        Icon(
            painter = painterResource(id = spec.drawableRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.testTag(spec.iconTestTag)
        )
        Text(text = spec.label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Icono y etiqueta de un nivel de consolidación de 0 a 5 (design D3). */
private data class LevelBadgeSpec(
    val drawableRes: Int,
    val iconTestTag: String,
    val label: String
)

private const val LEVEL_SIN_RANGO = 0
private const val LEVEL_BRONCE = 1
private const val LEVEL_PLATA = 2
private const val LEVEL_ORO = 3
private const val LEVEL_PLATINO = 4
private const val LEVEL_DIAMANTE = 5

/** Nombre de insignia de un nivel de consolidación de 0 a 5, o `null` fuera de rango (design D3). */
fun levelBadgeLabel(level: Int): String? = when (level) {
    LEVEL_SIN_RANGO -> "Sin rango"
    LEVEL_BRONCE -> "Bronce"
    LEVEL_PLATA -> "Plata"
    LEVEL_ORO -> "Oro"
    LEVEL_PLATINO -> "Platino"
    LEVEL_DIAMANTE -> "Diamante"
    else -> null
}

private fun levelBadgeSpec(level: Int): LevelBadgeSpec? {
    val label = levelBadgeLabel(level) ?: return null
    return when (level) {
        LEVEL_SIN_RANGO -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_sin_rango,
            iconTestTag = "ds-level-badge-icon-sin-rango",
            label = label
        )
        LEVEL_BRONCE -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_bronce,
            iconTestTag = "ds-level-badge-icon-bronce",
            label = label
        )
        LEVEL_PLATA -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_plata,
            iconTestTag = "ds-level-badge-icon-plata",
            label = label
        )
        LEVEL_ORO -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_oro,
            iconTestTag = "ds-level-badge-icon-oro",
            label = label
        )
        LEVEL_PLATINO -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_platino,
            iconTestTag = "ds-level-badge-icon-platino",
            label = label
        )
        LEVEL_DIAMANTE -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_diamante,
            iconTestTag = "ds-level-badge-icon-diamante",
            label = label
        )
        else -> null
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeLevel0Preview() {
    AppTheme {
        DsLevelBadge(level = 0)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeLevel1Preview() {
    AppTheme {
        DsLevelBadge(level = 1)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeLevel2Preview() {
    AppTheme {
        DsLevelBadge(level = 2)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeLevel3Preview() {
    AppTheme {
        DsLevelBadge(level = 3)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeLevel4Preview() {
    AppTheme {
        DsLevelBadge(level = 4)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeLevel5Preview() {
    AppTheme {
        DsLevelBadge(level = 5)
    }
}
