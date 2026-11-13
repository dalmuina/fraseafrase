package com.dalmuina.fraseafrase.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
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
 * muestra el snackbar sobrevive aunque la navegación ya haya cambiado de pantalla. Este es el único
 * uso de `SnackbarHost` de la app y es exclusivamente la confirmación de guardado de Ajustes: por
 * eso su texto usa directamente el token `successStatus` (adopt-reference-mock-ui, tarea 9.4), sin
 * necesidad de distinguir el mensaje de ningún otro uso futuro.
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data, contentColor = AppColors.SuccessStatus)
            }
        },
        bottomBar = {
            FraseAFraseBottomBar(
                selectedDestination = selectedDestination,
                onDestinationSelected = { selectedDestination = it },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedDestination) {
            BottomDestination.Practice -> PracticeSessionRoute(
                modifier = contentModifier,
                onNavigateToProgress = { selectedDestination = BottomDestination.Progress },
            )
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

/**
 * Barra de navegación inferior propiamente dicha, extraída de [FraseAFraseApp] para mantener su
 * longitud dentro del límite de detekt. Cada destino muestra un icono de `material-icons-extended`
 * además de su etiqueta de texto (spec `app-navigation`, design D5).
 */
@Composable
private fun FraseAFraseBottomBar(
    selectedDestination: BottomDestination,
    onDestinationSelected: (BottomDestination) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            modifier = Modifier.testTag("nav-practice"),
            selected = selectedDestination == BottomDestination.Practice,
            onClick = { onDestinationSelected(BottomDestination.Practice) },
            icon = {
                Icon(
                    modifier = Modifier.testTag("nav-practice-icon"),
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                )
            },
            label = { Text(text = stringResource(R.string.nav_practice)) },
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav-progress"),
            selected = selectedDestination == BottomDestination.Progress,
            onClick = { onDestinationSelected(BottomDestination.Progress) },
            icon = {
                Icon(
                    modifier = Modifier.testTag("nav-progress-icon"),
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                )
            },
            label = { Text(text = stringResource(R.string.nav_progress)) },
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav-settings"),
            selected = selectedDestination == BottomDestination.Settings,
            onClick = { onDestinationSelected(BottomDestination.Settings) },
            icon = {
                Icon(
                    modifier = Modifier.testTag("nav-settings-icon"),
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                )
            },
            label = { Text(text = stringResource(R.string.nav_settings)) },
        )
    }
}
