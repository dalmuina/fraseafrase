package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta definitiva fijada en el cambio de OpenSpec new-style (design D1), actualizada por
// redesign-app-ui al índigo del mock visual (primary #5B3DF5), con roles dedicados de éxito
// (tertiary), fallo (error) y aviso (secondary, banda "te faltan N aciertos" / botón de
// revertir). Fondo gris muy claro sin cambios. Contraste 4.5:1 verificado para cada combinación
// texto/fondo tocada (ver AppThemeTest).
internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF5B3DF5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECE7FF),
    onPrimaryContainer = Color(0xFF1B2559),
    secondary = Color(0xFFFF7A50),
    onSecondary = Color(0xFF5C2A12),
    secondaryContainer = Color(0xFFFFF1EA),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = Color(0xFF087F45),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3F6EC),
    onTertiaryContainer = Color(0xFF08693A),
    error = Color(0xFFB4232F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFECEE),
    onErrorContainer = Color(0xFFB4232F),
    // background corregido por adopt-reference-mock-ui (design - decisión 2) a #F7F5FF, el fondo
    // de pantalla y de campo numérico de design/mock/design-tokens.md.
    background = Color(0xFFF7F5FF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171F)
)

// Misma familia de matices que LightColors, con luminosidad ajustada para contraste AA sobre
// fondo oscuro. Sin mock oscuro de referencia (ninguno de los mocks entregados es oscuro):
// derivada por criterio de implementación, igual que ya hacía design D1 con el primario anterior
// (redesign-app-ui, design - Risks: contraste 4.5:1 revisado para cada combinación tocada).
internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF9886F9),
    onPrimary = Color(0xFF1B2559),
    primaryContainer = Color(0xFF32248D),
    onPrimaryContainer = Color(0xFFECE7FF),
    secondary = Color(0xFFFFA98A),
    onSecondary = Color(0xFF2B1206),
    secondaryContainer = Color(0xFF5C2A12),
    onSecondaryContainer = Color(0xFFFFF1EA),
    tertiary = Color(0xFF7FCBA0),
    onTertiary = Color(0xFF0B2E1A),
    tertiaryContainer = Color(0xFF0C4D27),
    onTertiaryContainer = Color(0xFFE3F6EC),
    error = Color(0xFFE26A74),
    onError = Color(0xFF2B0A0E),
    errorContainer = Color(0xFF5C0F17),
    onErrorContainer = Color(0xFFFFECEE),
    background = Color(0xFF14171F),
    surface = Color(0xFF1E2130),
    onSurface = Color(0xFFE6E8F0)
)

/**
 * Tokens de color del mock de referencia que no corresponden a ningún rol estándar de
 * `ColorScheme` de Material 3 (adopt-reference-mock-ui, design - decisión 2). Los valores salen
 * literalmente de `design/mock/design-tokens.md`; `textSecondary` unifica la inconsistencia del
 * propio mock entre `#5A5670` y `#5A6070` en el primero. `accent` (`#5B3DF5`) y `fab`
 * (`#FF7A50`) ya existen como [LightColors.primary]/[LightColors.secondary]: no se duplican aquí.
 * Sin variante oscura: el mock no define una (design - Non-Goals).
 */
object AppColors {
    val AccentStrong: Color = Color(0xFF3A1FC2)
    val AccentSoft: Color = Color(0xFFECE7FF)
    val TextSecondary: Color = Color(0xFF5A5670)
    val TextOnLevel: Color = Color(0xFF3A404D)
    val Track: Color = Color(0xFFE4DFF5)
    val TrackSoft: Color = Color(0xFFEFECF8)
    val DashedBorder: Color = Color(0xFFD3CCF0)
    val Disabled: Color = Color(0xFF8A909C)
    val HintBg: Color = Color(0xFFFFF1EA)
    val HintText: Color = Color(0xFF5C2A12)
    val RankUpText: Color = Color(0xFF9A3412)
    val FabContent: Color = Color(0xFF2B0E02)
    val Success: Color = Color(0xFF0B8A4A)
    val SuccessBg: Color = Color(0xFFE3F6EC)
    val SuccessText: Color = Color(0xFF08693A)
    val Error: Color = Color(0xFFD92D3F)
    val ErrorBg: Color = Color(0xFFFFECEE)
    val ErrorText: Color = Color(0xFFB4232F)

    // `successStatus`: color del mensaje "Ajustes guardados" (adopt-reference-mock-ui, tarea 9.4).
    val SuccessStatus: Color = Color(0xFF1B6B44)
}
