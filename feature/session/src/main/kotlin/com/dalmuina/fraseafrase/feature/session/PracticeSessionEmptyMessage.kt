package com.dalmuina.fraseafrase.feature.session

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign

/**
 * Mensaje centrado para los estados sin frases que practicar de [PracticeSessionScreen] (sin catálogo
 * disponible o con error al cargarlo). Vive en un archivo propio, separado de `PracticeSessionScreen.kt`,
 * para no superar el límite de funciones por archivo de detekt (`TooManyFunctions`) tras añadir el
 * composable de la insignia compuesta de "sesión completada" (adopt-reference-mock-ui, design - decisión
 * 6); no corresponde a ninguna decisión de diseño que exija mantenerlo en ese archivo.
 */
@Composable
internal fun EmptyMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}
