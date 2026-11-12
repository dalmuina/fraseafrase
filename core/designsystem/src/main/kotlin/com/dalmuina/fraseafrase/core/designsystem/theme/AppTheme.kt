package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.dalmuina.fraseafrase.core.designsystem.motion.LocalMotion
import com.dalmuina.fraseafrase.core.designsystem.motion.Motion
import com.dalmuina.fraseafrase.core.designsystem.tokens.Dimensions
import com.dalmuina.fraseafrase.core.designsystem.tokens.LocalDimensions
import com.dalmuina.fraseafrase.core.designsystem.tokens.LocalSpacing
import com.dalmuina.fraseafrase.core.designsystem.tokens.Spacing

/**
 * Tema de la aplicación (ADR-0010). Envuelve a [MaterialTheme] y expone los tokens propios.
 * Toda pantalla se renderiza dentro de [AppTheme].
 */
@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalMotion provides Motion(),
        LocalDimensions provides Dimensions()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Acceso a los tokens propios: `AppTheme.spacing.m`, `AppTheme.motion.durationMediumMillis`. */
object AppTheme {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val motion: Motion
        @Composable
        @ReadOnlyComposable
        get() = LocalMotion.current

    val dimensions: Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}
