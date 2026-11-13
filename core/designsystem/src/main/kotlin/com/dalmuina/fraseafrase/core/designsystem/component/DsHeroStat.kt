package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Tarjeta destacada de ancho completo con fondo de color, un valor grande ([value]), una etiqueta
 * ([label]) y una línea secundaria ([secondaryLine]), todos alineados a la izquierda, con espacio
 * para una composición decorativa opcional ([decoration]) que se pinta detrás del contenido
 * (design D3). Sustituye el `DsStatTile`(Large) + `Text` suelto de la cabecera de Progreso.
 * [decoration] recibe [BoxScope] para que quien lo use pueda alinear sus elementos
 * (`Modifier.align`) y, con `Modifier.matchParentSize()`, evitar que su propio tamaño (p. ej. una
 * decoración mayor que la tarjeta, pensada para desbordar y recortarse) agrande la tarjeta
 * (adopt-reference-mock-ui, tasks 8.1).
 */
@Composable
fun DsHeroStat(
    value: String,
    label: String,
    secondaryLine: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    decoration: @Composable BoxScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Box {
            decoration()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.l),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayLarge,
                    color = contentColor,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                Text(
                    text = secondaryLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DsHeroStatPreview() {
    AppTheme {
        DsHeroStat(
            value = "128",
            label = "Frases disponibles",
            secondaryLine = "Nivel actual: B1",
        )
    }
}
