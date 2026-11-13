package com.dalmuina.fraseafrase.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Alturas y medidas de controles compartidas. Las features usan `AppTheme.dimensions.xxx` en
 * lugar de repetir valores `dp` como literal en cada pantalla (fix-redesign-ui-mismatches,
 * design D8; medidas del mock de referencia añadidas por adopt-reference-mock-ui, design -
 * decisión 2, con los valores exactos de `design/mock/design-tokens.md`).
 */
@Immutable
data class Dimensions(
    val primaryActionHeight: Dp = 56.dp,
    // Botón primario / Fallo / Acierto: icono y separación con el texto.
    val primaryActionIconSize: Dp = 22.dp,
    val primaryActionIconGap: Dp = 10.dp,
    // Nav inferior: alto, indicador del tab activo e iconos.
    val navHeight: Dp = 80.dp,
    val navIndicatorWidth: Dp = 64.dp,
    val navIndicatorHeight: Dp = 32.dp,
    val navIconSize: Dp = 22.dp,
    // Botón flotante de volver a la frase anterior: tamaño, offsets e icono.
    val revertFabSize: Dp = 56.dp,
    val revertFabHorizontalOffset: Dp = 20.dp,
    val revertFabBottomOffset: Dp = 172.dp,
    val revertFabIconSize: Dp = 24.dp,
    // Botón Escuchar: tamaño circular e icono.
    val listenButtonSize: Dp = 44.dp,
    val listenButtonIconSize: Dp = 22.dp,
    // Insignia de los chips de nivel y rango.
    val chipBadgeSize: Dp = 18.dp,
    // Icono del mensaje "Te faltan N aciertos...", normal y en su versión compacta.
    val rankMessageIconSize: Dp = 24.dp,
    val rankMessageIconSizeCompact: Dp = 22.dp,
    // Ajustes: botones de nivel, stepper +/- y campo numérico.
    val settingsControlHeight: Dp = 52.dp,
    // Fila de rango de Progreso: icono y barra de progreso.
    val rankRowIconSize: Dp = 40.dp,
    val rankBarHeight: Dp = 6.dp
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
