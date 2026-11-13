package com.example.frases.ui.theme // TODO: ajustar al paquete del proyecto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Valores tomados literalmente del mock (design/mock). 1 px = 1 dp. */
object AppColors {
    val Accent = Color(0xFF5B3DF5)
    val AccentStrong = Color(0xFF3A1FC2)
    val AccentSoft = Color(0xFFECE7FF)
    val Background = Color(0xFFF7F5FF)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF14171F)
    val TextSecondary = Color(0xFF5A5670)
    val TextOnLevel = Color(0xFF3A404D)
    val Track = Color(0xFFE4DFF5)
    val TrackSoft = Color(0xFFEFECF8)
    val DashedBorder = Color(0xFFD3CCF0)
    val Disabled = Color(0xFF8A909C)

    val Fab = Color(0xFFFF7A50)
    val FabContent = Color(0xFF2B0E02)

    val HintBg = Color(0xFFFFF1EA)
    val HintText = Color(0xFF5C2A12)

    val Success = Color(0xFF0B8A4A)
    val SuccessBg = Color(0xFFE3F6EC)
    val SuccessText = Color(0xFF08693A)
    val SuccessStatus = Color(0xFF1B6B44)

    val Error = Color(0xFFD92D3F)
    val ErrorBg = Color(0xFFFFECEE)
    val ErrorText = Color(0xFFB4232F)

    val RankUpText = Color(0xFF9A3412)
}

enum class Rank(val outer: Color, val inner: Color, val chipText: Color?, val chipBg: Color?) {
    SinRango(Color(0xFF8A909C), Color(0xFF8A909C), null, null),
    Bronce(Color(0xFFB4693A), Color(0xFFE3A47C), null, null),
    Plata(Color(0xFF8E98A5), Color(0xFFD5DAE1), Color(0xFF3D434D), Color(0xFFEEF0F3)),
    Oro(Color(0xFFD9A21B), Color(0xFFF4CF6B), Color(0xFF6B4A06), Color(0xFFFBF1D6)),
    Platino(Color(0xFF3E9C9C), Color(0xFFA9DCDC), null, null),
    Diamante(Color(0xFF4F7DF0), Color(0xFFC9D8FF), null, null),
}

object AppShapes {
    val Card = RoundedCornerShape(24.dp)
    val Panel = RoundedCornerShape(20.dp)          // traducción, zona discontinua
    val Fab = RoundedCornerShape(18.dp)
    val Button = RoundedCornerShape(16.dp)         // primario, Fallo, Acierto (NO píldora)
    val Stat = RoundedCornerShape(16.dp)
    val Hint = RoundedCornerShape(14.dp)           // mensaje de rango, nivel, +/−, campo
    val Chip = RoundedCornerShape(8.dp)
    val Pill = RoundedCornerShape(percent = 50)    // contador, barras, indicador nav
}

object AppDimens {
    val ScreenHPadding = 20.dp
    val HeaderTop = 24.dp
    val HeaderBottom = 12.dp
    val HeaderGap = 14.dp
    val ProgressBarHeight = 8.dp
    val ContentTop = 8.dp
    val ContentGap = 16.dp

    val ButtonHeight = 56.dp
    val ButtonIcon = 22.dp
    val ButtonIconGap = 10.dp

    val FabSize = 56.dp
    val FabIcon = 24.dp
    val FabBottomOffset = 172.dp   // desde el borde inferior de la pantalla
    val FabStart = 20.dp

    val ListenButton = 44.dp
    val ChipBadge = 18.dp
    val HintBadge = 24.dp

    val NavHeight = 80.dp
    val NavIndicatorWidth = 64.dp
    val NavIndicatorHeight = 32.dp
    val NavIcon = 22.dp

    val SettingsControlHeight = 52.dp
    val RankRowIcon = 40.dp
    val RankBarHeight = 6.dp
}

object AppType {
    // Space Grotesk
    val ScreenTitle = 28.sp            // 700, tracking -0.5
    val PhraseShort = 28.sp            // 600, lh 1.25, tracking -0.3
    val PhraseLong = 22.sp             // 600, lh 1.3
    val BigNumber = 48.sp              // 700, lh 1.1
    val StatNumber = 24.sp             // 700
    // Inter
    val Button = 17.sp                 // 600
    val Translation = 17.sp            // 500, lh 1.4
    val Counter = 15.sp                // 600
    val Body = 14.sp                   // 400, lh 1.4
    val Chip = 13.sp                   // 700 nivel / 600 rango
    val Label = 12.sp                  // nav 700/500; TRADUCCIÓN 700 +0.8 mayúsculas
}

/** «Sin rango»: círculo discontinuo (r 9, trazo 2, guion 3/3 sobre viewBox 24). */
@Composable
fun SinRangoBadge(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        drawCircle(
            color = AppColors.Disabled,
            radius = 9f * k,
            style = Stroke(
                width = 2f * k,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * k, 3f * k)),
            ),
        )
    }
}
