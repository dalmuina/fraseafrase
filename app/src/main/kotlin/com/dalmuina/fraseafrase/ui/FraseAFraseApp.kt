package com.dalmuina.fraseafrase.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.dalmuina.fraseafrase.feature.session.PracticeSessionRoute
import com.dalmuina.fraseafrase.feature.settings.SettingsRoute
import com.dalmuina.fraseafrase.ui.navigation.Practice
import com.dalmuina.fraseafrase.ui.navigation.Settings

/**
 * Raíz de la UI. Muestra el destino inicial, la pantalla de práctica (design.md D7), y navega a los ajustes y
 * de vuelta con un *back stack* en memoria de Navigation 3 (design.md - Decisiones, "Navegación: Navigation 3
 * básico, sin *back stack* guardable, vive en `:app`").
 */
@Composable
fun FraseAFraseApp(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<Any>(Practice) }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                Practice ->
                    NavEntry(key) {
                        PracticeSessionRoute(
                            onOpenSettings = { backStack.add(Settings) },
                            modifier = modifier,
                        )
                    }
                Settings ->
                    NavEntry(key) {
                        SettingsRoute(
                            onBack = { backStack.removeLastOrNull() },
                            modifier = modifier,
                        )
                    }
                else -> error("Unknown route: $key")
            }
        },
    )
}
