package com.dalmuina.fraseafrase.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.R
import com.dalmuina.fraseafrase.feature.progress.ProgressRoute
import com.dalmuina.fraseafrase.feature.session.PracticeSessionRoute
import com.dalmuina.fraseafrase.feature.settings.SettingsRoute
import com.dalmuina.fraseafrase.ui.navigation.BottomDestination
import kotlinx.coroutines.launch

/**
 * Barra de navegación inferior de tres destinos de igual nivel (design.md - Decisions, "Barra de
 * navegación inferior en `:app`"): práctica, progreso y ajustes. Cambiar de pestaña selecciona qué
 * `Route` se compone, sin pila de navegación (ADR-0010: solo `:app` conoce las tres features). Este
 * `Scaffold` también aloja el `snackbarHost` de la confirmación de guardado de Ajustes (design D6
 * revisada de `add-settings-apply-flow`): como nunca se desmonta al cambiar de pestaña, la corrutina que
 * muestra el snackbar sobrevive aunque la navegación ya haya cambiado de pantalla.
 */
@Composable
fun FraseAFraseApp(modifier: Modifier = Modifier) {
    var selectedDestination by remember {
        mutableStateOf<BottomDestination>(BottomDestination.Practice)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    modifier = Modifier.testTag("nav-practice"),
                    selected = selectedDestination == BottomDestination.Practice,
                    onClick = { selectedDestination = BottomDestination.Practice },
                    icon = {},
                    label = { Text(text = stringResource(R.string.nav_practice)) },
                )
                NavigationBarItem(
                    modifier = Modifier.testTag("nav-progress"),
                    selected = selectedDestination == BottomDestination.Progress,
                    onClick = { selectedDestination = BottomDestination.Progress },
                    icon = {},
                    label = { Text(text = stringResource(R.string.nav_progress)) },
                )
                NavigationBarItem(
                    modifier = Modifier.testTag("nav-settings"),
                    selected = selectedDestination == BottomDestination.Settings,
                    onClick = { selectedDestination = BottomDestination.Settings },
                    icon = {},
                    label = { Text(text = stringResource(R.string.nav_settings)) },
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedDestination) {
            BottomDestination.Practice -> PracticeSessionRoute(modifier = contentModifier)
            BottomDestination.Progress -> ProgressRoute(modifier = contentModifier)
            BottomDestination.Settings ->
                SettingsRoute(
                    modifier = contentModifier,
                    onNavigateToPractice = { selectedDestination = BottomDestination.Practice },
                    onShowSaveConfirmation = { message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
        }
    }
}
