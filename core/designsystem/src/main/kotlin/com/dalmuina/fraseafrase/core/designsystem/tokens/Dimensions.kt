package com.dalmuina.fraseafrase.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Alturas de controles compartidas. Las features usan `AppTheme.dimensions.primaryActionHeight`
 * en lugar de repetir `56.dp` como literal en cada pantalla (fix-redesign-ui-mismatches,
 * design D8).
 */
@Immutable
data class Dimensions(
    val primaryActionHeight: Dp = 56.dp
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
