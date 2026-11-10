package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.motion.LocalMotion
import com.dalmuina.fraseafrase.core.designsystem.motion.Motion
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

private const val HALO_TEST_TAG = "ds-level-badge-change-halo"
private val HALO_SIZE = 56.dp
private const val HALO_ALPHA = 0.25f
private const val POP_START_SCALE = 0.8f
private const val POP_END_SCALE = 1f

/**
 * Indicador animado de un cambio de nivel (subida o bajada, ADR-0012/ADR-0015). A diferencia de
 * [DsLevelBadge], que solo muestra el estado actual, este composable representa una transición de
 * verdad entre [fromLevel] y [toLevel] (crossfade + "pop" de escala, new-badge-animation design
 * D4): [emphasized] distingue el estilo de una subida (con halo, `true`) de una bajada (sin halo,
 * `false`). Reconstruye la misma estructura `Row(Icon, Text)` que [DsLevelBadge] (reutilizando
 * [levelBadgeSpec]), pero con un `Crossfade` por elemento: el del icono comparte `Box` con el
 * halo para que ambos queden centrados sobre el mismo punto, en vez de centrar el halo respecto
 * al pill completo (icono + etiqueta), que lo desplazaba fuera del icono (new-badge-animation
 * design D4, corrección de la prueba manual tras el grupo 5). Se asume que este composable se
 * inserta de nuevo en el árbol en cada aparición (no sobrevive entre cambios sucesivos), por lo
 * que [LaunchedEffect] se dispara una sola vez por instancia con `Unit` como clave.
 */
@Composable
fun DsLevelBadgeChange(fromLevel: Int, toLevel: Int, emphasized: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    var animateIn by remember { mutableStateOf(false) }
    var displayedLevel by remember { mutableStateOf(fromLevel) }
    val haloAlpha by animateFloatAsState(
        targetValue = if (animateIn) HALO_ALPHA else 0f,
        animationSpec = tween(
            durationMillis = motion.durationMediumMillis,
            easing = motion.emphasizedEasing
        ),
        label = "ds-level-badge-change-halo-alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (animateIn) POP_END_SCALE else POP_START_SCALE,
        animationSpec = tween(
            durationMillis = motion.durationMediumMillis,
            easing = motion.standardEasing
        ),
        label = "ds-level-badge-change-scale"
    )
    LaunchedEffect(Unit) {
        animateIn = true
        displayedLevel = toLevel
    }

    Row(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
    ) {
        BadgeIconWithHalo(
            displayedLevel = displayedLevel,
            emphasized = emphasized,
            haloAlpha = haloAlpha,
            motion = motion
        )
        BadgeLabel(displayedLevel = displayedLevel, motion = motion)
    }
}

/** Icono con el halo detrás, ambos centrados sobre el mismo punto (design D4). */
@Composable
private fun BadgeIconWithHalo(displayedLevel: Int, emphasized: Boolean, haloAlpha: Float, motion: Motion) {
    Box(contentAlignment = Alignment.Center) {
        if (emphasized) {
            val haloColor = MaterialTheme.colorScheme.tertiary
            Canvas(
                modifier = Modifier
                    .size(HALO_SIZE)
                    .testTag(HALO_TEST_TAG)
            ) {
                drawCircle(
                    color = haloColor.copy(alpha = haloAlpha),
                    radius = size.minDimension / 2f
                )
            }
        }
        Crossfade(
            targetState = displayedLevel,
            animationSpec = tween(durationMillis = motion.durationMediumMillis, easing = motion.standardEasing),
            label = "ds-level-badge-change-icon-crossfade"
        ) { level ->
            val spec = levelBadgeSpec(level)
            if (spec != null) {
                Icon(
                    painter = painterResource(id = spec.drawableRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.testTag(spec.iconTestTag)
                )
            }
        }
    }
}

/** Etiqueta de texto del nivel, en su propio `Crossfade` sincronizado con el del icono (design D4). */
@Composable
private fun BadgeLabel(displayedLevel: Int, motion: Motion) {
    Crossfade(
        targetState = displayedLevel,
        animationSpec = tween(durationMillis = motion.durationMediumMillis, easing = motion.standardEasing),
        label = "ds-level-badge-change-label-crossfade"
    ) { level ->
        val spec = levelBadgeSpec(level)
        if (spec != null) {
            Text(text = spec.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeChangeEmphasizedPreview() {
    AppTheme {
        DsLevelBadgeChange(fromLevel = 1, toLevel = 2, emphasized = true)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeChangeNotEmphasizedPreview() {
    AppTheme {
        DsLevelBadgeChange(fromLevel = 2, toLevel = 1, emphasized = false)
    }
}
