package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.dalmuina.fraseafrase.core.designsystem.R

// Tipografía definitiva fijada en el cambio de OpenSpec new-style (design D4): Baloo 2 para
// titulares redondeados de trazo grueso, Plus Jakarta Sans para el resto de la escala. Ambas se
// empaquetan como ficheros .ttf estáticos (offline-first, ADR-0004), no como downloadable fonts.

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
        fontWeight = FontWeight.Bold
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = Baloo2Family,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = Baloo2Family,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal
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
