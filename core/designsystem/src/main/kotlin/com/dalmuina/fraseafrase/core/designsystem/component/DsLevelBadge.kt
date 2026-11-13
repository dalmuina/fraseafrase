package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.R
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Insignia visual de un nivel de consolidación (0 a 5, ADR-0012/ADR-0015). Recibe un [Int] en vez
 * del tipo `Level` del dominio, ya que `:core:designsystem` no depende de ningún módulo del
 * proyecto (ADR-0010, design D1). El nivel 0 pinta la insignia "Sin rango" (design D3) como un
 * círculo discontinuo dibujado con [Canvas] (adopt-reference-mock-ui, design - decisión 2): un
 * `VectorDrawable` no admite trazo discontinuo nativo.
 */
@Composable
fun DsLevelBadge(level: Int, modifier: Modifier = Modifier) {
    val spec = levelBadgeSpec(level) ?: return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)
    ) {
        if (level == LEVEL_SIN_RANGO) {
            SinRangoBadgeIcon(modifier = Modifier.testTag(spec.iconTestTag))
        } else {
            Icon(
                painter = painterResource(id = spec.drawableRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.testTag(spec.iconTestTag)
            )
        }
        Text(text = spec.label, style = MaterialTheme.typography.labelMedium)
    }
}

private val SinRangoBadgeSize = 40.dp
private const val SIN_RANGO_VIEWPORT_SIZE = 24f
private const val SIN_RANGO_RADIUS = 9f
private const val SIN_RANGO_STROKE_WIDTH = 2f
private const val SIN_RANGO_DASH_LENGTH = 3f

/**
 * Círculo discontinuo de la insignia "Sin rango", equivalente a la función `SinRangoBadge` del
 * mock de referencia (`design/mock/compose/DesignTokens.kt`), adaptada al estilo de este
 * componente: tamaño fijo igual al resto de insignias (40dp, viewport 24) en vez de un parámetro
 * `size`, y color tomado de [levelBadgeColors] en vez de `AppColors.Disabled` directamente, para
 * que "Sin rango" comparta la misma fuente de verdad de color que el resto de niveles.
 */
@Composable
private fun SinRangoBadgeIcon(modifier: Modifier = Modifier) {
    val color = levelBadgeColors(LEVEL_SIN_RANGO)?.exterior ?: Color.Unspecified
    Canvas(modifier = modifier.size(SinRangoBadgeSize)) {
        val scale = size.minDimension / SIN_RANGO_VIEWPORT_SIZE
        drawCircle(
            color = color,
            radius = SIN_RANGO_RADIUS * scale,
            style = Stroke(
                width = SIN_RANGO_STROKE_WIDTH * scale,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(SIN_RANGO_DASH_LENGTH * scale, SIN_RANGO_DASH_LENGTH * scale)
                )
            )
        )
    }
}

/**
 * Icono y etiqueta de un nivel de consolidación de 0 a 5 (design D3). `internal`: lo reutiliza
 * [DsLevelBadgeChange] para reconstruir la misma estructura `Row(Icon, Text)` con un `Crossfade`
 * por elemento (design new-badge-animation D4), sin salir de `:core:designsystem` (ADR-0010).
 */
internal data class LevelBadgeSpec(
    val drawableRes: Int,
    val iconTestTag: String,
    val label: String
)

private const val LEVEL_SIN_RANGO = 0
private const val LEVEL_BRONCE = 1
private const val LEVEL_PLATA = 2
private const val LEVEL_ORO = 3
private const val LEVEL_PLATINO = 4
private const val LEVEL_DIAMANTE = 5

/**
 * Color exterior exacto de la insignia de un nivel de consolidación de 0 a 5, o `null` fuera de
 * rango (design - decisión 2). Lo usa la fila de rango de Progreso para pintar su
 * [DsProgressBar] con el color exacto de ese rango (adopt-reference-mock-ui, tasks 8.2).
 */
fun levelBadgeAccentColor(level: Int): Color? = levelBadgeColors(level)?.exterior

/** Nombre de insignia de un nivel de consolidación de 0 a 5, o `null` fuera de rango (design D3). */
fun levelBadgeLabel(level: Int): String? = when (level) {
    LEVEL_SIN_RANGO -> "Sin rango"
    LEVEL_BRONCE -> "Bronce"
    LEVEL_PLATA -> "Plata"
    LEVEL_ORO -> "Oro"
    LEVEL_PLATINO -> "Platino"
    LEVEL_DIAMANTE -> "Diamante"
    else -> null
}

/**
 * Colores exterior/interior exactos de la insignia de un nivel de consolidación de 0 a 5, según la
 * tabla "Rangos" de `design/mock/design-tokens.md` (adopt-reference-mock-ui, design - decisión 2).
 * Para "Sin rango" [interior] es `null`: la insignia es un único trazo discontinuo, sin relleno.
 * Para "Diamante" [interior] es el color de las facetas (trazo interno), no un relleno como en el
 * resto de niveles. Es la fuente de verdad en Kotlin que usa [SinRangoBadgeIcon]; los niveles 1 a
 * 5 siguen pintándose con los `VectorDrawable` `ic_rango_*`, que ya embeben estos mismos valores.
 */
internal data class LevelBadgeColors(val exterior: Color, val interior: Color?)

// Valores literales como propiedades de nivel de archivo (MagicNumber los ignora con
// ignorePropertyDeclaration, igual que el resto de tokens de AppColors).
private val BronceExterior = Color(0xFFB4693A)
private val BronceInterior = Color(0xFFE3A47C)
private val PlataExterior = Color(0xFF8E98A5)
private val PlataInterior = Color(0xFFD5DAE1)
private val OroExterior = Color(0xFFD9A21B)
private val OroInterior = Color(0xFFF4CF6B)
private val PlatinoExterior = Color(0xFF3E9C9C)
private val PlatinoInterior = Color(0xFFA9DCDC)
private val DiamanteExterior = Color(0xFF4F7DF0)
private val DiamanteFacetas = Color(0xFFC9D8FF)

internal fun levelBadgeColors(level: Int): LevelBadgeColors? = when (level) {
    LEVEL_SIN_RANGO -> LevelBadgeColors(exterior = AppColors.Disabled, interior = null)
    LEVEL_BRONCE -> LevelBadgeColors(exterior = BronceExterior, interior = BronceInterior)
    LEVEL_PLATA -> LevelBadgeColors(exterior = PlataExterior, interior = PlataInterior)
    LEVEL_ORO -> LevelBadgeColors(exterior = OroExterior, interior = OroInterior)
    LEVEL_PLATINO -> LevelBadgeColors(exterior = PlatinoExterior, interior = PlatinoInterior)
    LEVEL_DIAMANTE -> LevelBadgeColors(exterior = DiamanteExterior, interior = DiamanteFacetas)
    else -> null
}

internal fun levelBadgeSpec(level: Int): LevelBadgeSpec? {
    val label = levelBadgeLabel(level) ?: return null
    return when (level) {
        LEVEL_SIN_RANGO -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_sin_rango,
            iconTestTag = "ds-level-badge-icon-sin-rango",
            label = label
        )
        LEVEL_BRONCE -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_bronce,
            iconTestTag = "ds-level-badge-icon-bronce",
            label = label
        )
        LEVEL_PLATA -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_plata,
            iconTestTag = "ds-level-badge-icon-plata",
            label = label
        )
        LEVEL_ORO -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_oro,
            iconTestTag = "ds-level-badge-icon-oro",
            label = label
        )
        LEVEL_PLATINO -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_platino,
            iconTestTag = "ds-level-badge-icon-platino",
            label = label
        )
        LEVEL_DIAMANTE -> LevelBadgeSpec(
            drawableRes = R.drawable.ic_rango_diamante,
            iconTestTag = "ds-level-badge-icon-diamante",
            label = label
        )
        else -> null
    }
}

private val AllLevels = listOf(0, 1, 2, 3, 4, 5)

// Un único preview con los 6 niveles en columna (en vez de uno por nivel) para no superar el
// límite de funciones por archivo de detekt (TooManyFunctions) al añadir levelBadgeColors y
// SinRangoBadgeIcon.
@PreviewLightDark
@Composable
private fun DsLevelBadgeAllLevelsPreview() {
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
            AllLevels.forEach { level -> DsLevelBadge(level = level) }
        }
    }
}
