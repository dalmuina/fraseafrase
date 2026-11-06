package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Insignia visual de un nivel de consolidación (0 a 5, ADR-0012/ADR-0015). Recibe un [Int] en vez
 * del tipo `Level` del dominio, ya que `:core:designsystem` no depende de ningún módulo del
 * proyecto (ADR-0010, design D1). El nivel 0 no pinta nada.
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
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.tint,
            modifier = Modifier.testTag(spec.iconTestTag)
        )
        Text(text = spec.label, color = spec.tint)
    }
}

/** Icono, color y etiqueta de un nivel de consolidación de 1 a 5 (design D2). */
private data class LevelBadgeSpec(
    val icon: ImageVector,
    val iconTestTag: String,
    val tint: Color,
    val label: String
)

private const val LEVEL_BRONCE = 1
private const val LEVEL_PLATA = 2
private const val LEVEL_ORO = 3
private const val LEVEL_PLATINO = 4
private const val LEVEL_DIAMANTE = 5

/** Nombre de insignia de un nivel de consolidación de 1 a 5, o `null` fuera de rango (design D6). */
fun levelBadgeLabel(level: Int): String? = when (level) {
    LEVEL_BRONCE -> "Bronce"
    LEVEL_PLATA -> "Plata"
    LEVEL_ORO -> "Oro"
    LEVEL_PLATINO -> "Platino"
    LEVEL_DIAMANTE -> "Diamante"
    else -> null
}

@Composable
private fun levelBadgeSpec(level: Int): LevelBadgeSpec? {
    val label = levelBadgeLabel(level) ?: return null
    return when (level) {
        LEVEL_BRONCE -> LevelBadgeSpec(
            icon = Icons.Filled.MilitaryTech,
            iconTestTag = "ds-level-badge-icon-military-tech-bronce",
            tint = MaterialTheme.colorScheme.tertiary,
            label = label
        )
        LEVEL_PLATA -> LevelBadgeSpec(
            icon = Icons.Filled.MilitaryTech,
            iconTestTag = "ds-level-badge-icon-military-tech-plata",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            label = label
        )
        LEVEL_ORO -> LevelBadgeSpec(
            icon = Icons.Filled.WorkspacePremium,
            iconTestTag = "ds-level-badge-icon-workspace-premium-oro",
            tint = MaterialTheme.colorScheme.primary,
            label = label
        )
        LEVEL_PLATINO -> LevelBadgeSpec(
            icon = Icons.Filled.WorkspacePremium,
            iconTestTag = "ds-level-badge-icon-workspace-premium-platino",
            tint = MaterialTheme.colorScheme.secondary,
            label = label
        )
        LEVEL_DIAMANTE -> LevelBadgeSpec(
            icon = Icons.Filled.Diamond,
            iconTestTag = "ds-level-badge-icon-diamond-diamante",
            tint = MaterialTheme.colorScheme.tertiaryContainer,
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
