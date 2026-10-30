package com.dalmuina.fraseafrase.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaciados. Las features usan estos tokens en lugar de valores `dp` literales. */
@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
