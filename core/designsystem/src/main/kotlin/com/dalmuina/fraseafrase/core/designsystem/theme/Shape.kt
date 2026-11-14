package com.dalmuina.fraseafrase.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Esquinas muy redondeadas fijadas en el cambio de OpenSpec new-style (design D2), acorde a los
// mocks: tarjetas ~24dp, chips/botones con esquina muy redondeada, pill-shaped en extraLarge.
internal val AppShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Radios del mock de referencia que no caben en los 5 slots de [Shapes] de Material 3
 * (adopt-reference-mock-ui, design - decisión 2): panel de traducción y zona discontinua
 * ([Panel]), botón flotante de volver ([Fab]), y controles compactos -- mensaje de rango, botones
 * de nivel, stepper +/- y campo numérico ([Hint]). Valores exactos de
 * `design/mock/design-tokens.md`, sección "Radios".
 */
object AppCornerShapes {
    val Panel: RoundedCornerShape = RoundedCornerShape(20.dp)
    val Fab: RoundedCornerShape = RoundedCornerShape(18.dp)
    val Hint: RoundedCornerShape = RoundedCornerShape(14.dp)

    // Cápsula completa de la barra de navegación flotante (bottom-bar-navigation, design D1),
    // reutilizada también por el indicador de la pestaña seleccionada.
    val FloatingNav: RoundedCornerShape = RoundedCornerShape(percent = 50)
}
