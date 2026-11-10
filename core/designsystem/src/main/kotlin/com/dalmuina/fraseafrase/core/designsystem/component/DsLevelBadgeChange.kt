package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.motion.LocalMotion
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

private const val HALO_TEST_TAG = "ds-level-badge-change-halo"
private val HALO_SIZE = 56.dp
private const val HALO_ALPHA = 0.25f

/**
 * Indicador animado de un cambio de nivel (subida o bajada, ADR-0012/ADR-0015). A diferencia de
 * [DsLevelBadge], que solo muestra el estado actual, este composable representa una transición:
 * [emphasized] distingue el estilo de una subida (con halo, `true`) de una bajada (sin halo,
 * `false`), design-add-level-change-animation D4. Reutiliza el icono y la etiqueta de
 * [DsLevelBadge].
 */
@Composable
fun DsLevelBadgeChange(level: Int, emphasized: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    var animateIn by remember { mutableStateOf(false) }
    val haloAlpha by animateFloatAsState(
        targetValue = if (animateIn) HALO_ALPHA else 0f,
        animationSpec = tween(
            durationMillis = motion.durationShortMillis,
            easing = motion.emphasizedEasing
        ),
        label = "ds-level-badge-change-halo-alpha"
    )
    LaunchedEffect(emphasized) {
        animateIn = emphasized
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
        DsLevelBadge(level = level)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeChangeEmphasizedPreview() {
    AppTheme {
        DsLevelBadgeChange(level = 2, emphasized = true)
    }
}

@PreviewLightDark
@Composable
private fun DsLevelBadgeChangeNotEmphasizedPreview() {
    AppTheme {
        DsLevelBadgeChange(level = 2, emphasized = false)
    }
}
