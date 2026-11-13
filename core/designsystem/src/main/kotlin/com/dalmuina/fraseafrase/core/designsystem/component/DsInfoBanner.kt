package com.dalmuina.fraseafrase.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme

/**
 * Caja destacada con un icono y un texto compuesto por [segments], algunos en negrita, sobre un
 * fondo de color configurable (design D2). Sustituye los `Text` sueltos de aviso como "Te faltan N
 * aciertos para subir a X", con "N aciertos" y "X" como tramos en negrita.
 *
 * [iconTint] se aplica al icono con `tint =`; por defecto es [contentColor], igual que antes. Pasa
 * `Color.Unspecified` cuando [icon] ya es multicolor (p. ej. una insignia de rango,
 * `design-tokens.md`), para que no se tiña de un solo color (adopt-reference-mock-ui, tasks 12.7).
 */
@Composable
fun DsInfoBanner(
    icon: ImageVector,
    iconContentDescription: String?,
    segments: List<DsInfoBannerSegment>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconTint: Color = contentColor,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(AppTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = iconTint,
            )
            Text(
                text = buildAnnotatedString {
                    segments.forEach { segment ->
                        if (segment.emphasized) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(segment.text)
                            }
                        } else {
                            append(segment.text)
                        }
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DsInfoBannerPreview() {
    AppTheme {
        DsInfoBanner(
            icon = Icons.Default.MilitaryTech,
            iconContentDescription = null,
            segments = listOf(
                DsInfoBannerSegment(text = "Te faltan "),
                DsInfoBannerSegment(text = "3 aciertos", emphasized = true),
                DsInfoBannerSegment(text = " para la insignia "),
                DsInfoBannerSegment(text = "Plata", emphasized = true),
            ),
        )
    }
}
