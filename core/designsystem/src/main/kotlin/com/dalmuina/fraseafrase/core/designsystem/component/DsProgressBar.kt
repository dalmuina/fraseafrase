package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Barra de progreso lineal con la forma y el color del tema del proyecto: extremos redondeados
 * (`MaterialTheme.shapes.extraLarge`) y color primario (design - Componentes Ds* nuevos). Envuelve
 * [LinearProgressIndicator] de Material 3 en vez de añadir una implementación propia, igual que ya
 * decidió el cambio `fix-style-pass-two`.
 */
@Composable
fun DsProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
        strokeCap = StrokeCap.Round
    )
}

@PreviewLightDark
@Composable
private fun DsProgressBarPartialPreview() {
    AppTheme {
        DsProgressBar(progress = 0.4f)
    }
}

@PreviewLightDark
@Composable
private fun DsProgressBarCompletePreview() {
    AppTheme {
        DsProgressBar(progress = 1f)
    }
}
