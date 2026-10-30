package com.dalmuina.fraseafrase.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** Duraciones y curvas de animación compartidas. Las features no definen las suyas propias. */
@Immutable
data class Motion(
    val durationShortMillis: Int = 150,
    val durationMediumMillis: Int = 300,
    val durationLongMillis: Int = 500,
    val standardEasing: Easing = StandardEasing,
    val emphasizedEasing: Easing = EmphasizedEasing
) {
    companion object {
        private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        private val EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    }
}

val LocalMotion = staticCompositionLocalOf { Motion() }
