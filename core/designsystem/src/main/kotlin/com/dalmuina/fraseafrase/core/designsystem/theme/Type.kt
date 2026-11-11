package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dalmuina.fraseafrase.core.designsystem.R

// Tipografía definitiva fijada en el cambio de OpenSpec new-style (design D4): Baloo 2 para
// titulares redondeados de trazo grueso, Plus Jakarta Sans para el resto de la escala. Ambas se
// empaquetan como ficheros .ttf estáticos (offline-first, ADR-0004), no como downloadable fonts.
// redesign-app-ui ajusta tamaño/peso/letterSpacing de displayLarge/headlineLarge/titleLarge
// (Baloo 2) y titleMedium/bodyLarge (Plus Jakarta Sans) para acercarse al ritmo visual de Space
// Grotesk/Inter del mock (titulares 22-28dp con letterSpacing negativo, 48dp para el número
// grande de la tarjeta destacada de Progreso; cuerpo 13-17dp), sin añadir ninguna familia nueva.

internal val Baloo2Family: FontFamily = FontFamily(
    Font(R.font.baloo2_semibold, weight = FontWeight.SemiBold),
    Font(R.font.baloo2_bold, weight = FontWeight.Bold)
)

internal val PlusJakartaSansFamily: FontFamily = FontFamily(
    Font(R.font.plusjakartasans_regular, weight = FontWeight.Normal),
    Font(R.font.plusjakartasans_medium, weight = FontWeight.Medium),
    Font(R.font.plusjakartasans_semibold, weight = FontWeight.SemiBold)
)

private val defaultTypography = Typography()

internal val AppTypography: Typography = defaultTypography.copy(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = Baloo2Family,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = Baloo2Family,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = Baloo2Family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Medium
    )
)
