package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta definitiva fijada en el cambio de OpenSpec new-style (design D1), muestreada de los
// mocks de alta fidelidad: primario índigo, acierto en verde, fondo gris muy claro.

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF3346C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E9FB),
    onPrimaryContainer = Color(0xFF1B2559),
    tertiary = Color(0xFF1B6B44),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F6FA),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171F)
)

// Misma familia de matices que LightColors, con luminosidad ajustada para contraste AA sobre
// fondo oscuro. Sin mock de referencia (ninguno de los 4 entregados es oscuro): derivada por
// criterio de implementación (design D1).
internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB9C3F2),
    onPrimary = Color(0xFF1B2559),
    primaryContainer = Color(0xFF2B3A8F),
    onPrimaryContainer = Color(0xFFE6E9FB),
    tertiary = Color(0xFF7FCBA0),
    onTertiary = Color(0xFF0B2E1A),
    background = Color(0xFF14171F),
    surface = Color(0xFF1E2130),
    onSurface = Color(0xFFE6E8F0)
)
