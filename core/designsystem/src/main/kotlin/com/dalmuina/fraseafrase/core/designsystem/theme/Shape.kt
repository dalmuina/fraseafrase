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
