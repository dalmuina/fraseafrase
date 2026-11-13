package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.dalmuina.fraseafrase.core.designsystem.R

// Tipografía del mock de referencia (adopt-reference-mock-ui, design - decisiones 1 y 2):
// Space Grotesk (titulares, frases, números) e Inter (resto), sustituyendo a Baloo 2 + Plus
// Jakarta Sans. Empaquetadas como .ttf estáticos (offline-first, ADR-0013), no como downloadable
// fonts. Mapeo de rol del mock a propiedad de [Typography], de `design/mock/design-tokens.md`:
// - displayLarge:   número grande (Progreso)                 48sp/700/lh1.1/track0, Space Grotesk
// - headlineLarge:  título de pantalla                        28sp/700/track-0.5,   Space Grotesk
// - headlineMedium: frase a traducir (corta)                  28sp/600/lh1.25/track-0.3, Space Grotesk
// - headlineSmall:  frase a traducir (larga)                  22sp/600/lh1.3/track0, Space Grotesk
// - titleLarge:     número de estadística / campo numérico    24sp/700/track0, Space Grotesk
// - titleMedium:    botón primario / Fallo / Acierto          17sp/600/track0, Inter
// - titleSmall:     traducción                                17sp/500/lh1.4, Inter
// - bodyLarge:      contador "Frase N de M" / nombre de rango 15sp/600/track0, Inter
// - bodyMedium:     texto de ayuda / mensaje de rango         14sp/400/lh1.4, Inter
// - bodySmall:      (sin rol propio del mock; solo familia Inter)
// - labelLarge:     chips de nivel y rango                    13sp/700, Inter
// - labelMedium:    etiqueta "TRADUCCIÓN" / etiqueta del nav  12sp/700 (activo), Inter
// - labelSmall:     (sin rol propio del mock; solo familia Inter)
// El tracking +0.8 de la etiqueta "TRADUCCIÓN" y el peso 500 de la etiqueta inactiva del nav se
// aplican con `.copy()` en el composable concreto, no en el token compartido (dos usos del mismo
// slot con valores distintos).

internal val SpaceGroteskFamily: FontFamily = FontFamily(
    Font(R.font.spacegrotesk_regular, weight = FontWeight.Normal),
    Font(R.font.spacegrotesk_medium, weight = FontWeight.Medium),
    Font(R.font.spacegrotesk_semibold, weight = FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold, weight = FontWeight.Bold)
)

internal val InterFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_bold, weight = FontWeight.Bold)
)

private val defaultTypography = Typography()

internal val AppTypography: Typography = defaultTypography.copy(
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.8.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.6.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.8.sp
    ),
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.6.sp
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium
    )
)

/**
 * Previsualización manual (tasks.md 2.4) para comprobar visualmente que los roles de título y
 * cuerpo usan Space Grotesk e Inter respectivamente, con los tamaños y pesos del mock.
 */
@PreviewLightDark
@Composable
private fun TypographyPreview() {
    AppTheme {
        Column {
            Text(text = "Título de pantalla", style = MaterialTheme.typography.headlineLarge)
            Text(text = "Frase a traducir corta", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Traducción revelada", style = MaterialTheme.typography.titleSmall)
        }
    }
}
