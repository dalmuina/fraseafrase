package com.dalmuina.fraseafrase.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dalmuina.fraseafrase.feature.session.PracticeSessionRoute

/**
 * Raíz de la UI. Muestra el destino inicial, la pantalla de práctica (design.md D7). La navegación
 * llegará con `:feature:settings`.
 */
@Composable
fun FraseAFraseApp(modifier: Modifier = Modifier) {
    PracticeSessionRoute(modifier = modifier)
}
