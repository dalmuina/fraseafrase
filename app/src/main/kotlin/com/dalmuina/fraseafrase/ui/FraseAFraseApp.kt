package com.dalmuina.fraseafrase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import com.dalmuina.fraseafrase.R
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.feature.progress.ProgressRoute
import com.dalmuina.fraseafrase.feature.session.PracticeSessionRoute
import com.dalmuina.fraseafrase.feature.settings.SettingsRoute
import com.dalmuina.fraseafrase.ui.navigation.BottomDestination
import kotlinx.coroutines.launch
import com.dalmuina.fraseafrase.core.designsystem.R as DesignSystemR

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
                // El mock (05-ajustes.html) pinta la confirmación como texto centrado sin caja ni
                // fondo oscuro. `Snackbar` no expone un parámetro público para anular su
                // `shadowElevation` (fijo a `SnackbarTokens.ContainerElevation`, ver
                // `androidx.compose.material3.Snackbar`), así que con `containerColor =
                // Color.Transparent` queda sin el fondo oscuro por defecto de Material 3 pero
                // conserva una sombra tenue bajo la forma del contenedor; `shape = RectangleShape`
                // evita que esa sombra residual dibuje además una esquina redondeada visible
                // (adopt-reference-mock-ui, tarea 12.6).
                Snackbar(
                    snackbarData = data,
                    shape = RectangleShape,
                    containerColor = Color.Transparent,
                    contentColor = AppColors.SuccessStatus,
                )
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
 * longitud dentro del límite de detekt. Cada destino muestra uno de los `ic_nav_*` del mock además
 * de su etiqueta de texto (spec `app-navigation`, design D5; adopt-reference-mock-ui design D7).
 */
@Composable
private fun FraseAFraseBottomBar(
    selectedDestination: BottomDestination,
    onDestinationSelected: (BottomDestination) -> Unit,
) {
    NavigationBar(modifier = Modifier.testTag("bottom-nav-bar").height(AppTheme.dimensions.navHeight)) {
        FraseAFraseBottomBarItem(
            tag = "nav-practice",
            selected = selectedDestination == BottomDestination.Practice,
            onClick = { onDestinationSelected(BottomDestination.Practice) },
            iconRes = DesignSystemR.drawable.ic_nav_practicar,
            label = stringResource(R.string.nav_practice),
        )
        FraseAFraseBottomBarItem(
            tag = "nav-progress",
            selected = selectedDestination == BottomDestination.Progress,
            onClick = { onDestinationSelected(BottomDestination.Progress) },
            iconRes = DesignSystemR.drawable.ic_nav_progreso,
            label = stringResource(R.string.nav_progress),
        )
        FraseAFraseBottomBarItem(
            tag = "nav-settings",
            selected = selectedDestination == BottomDestination.Settings,
            onClick = { onDestinationSelected(BottomDestination.Settings) },
            iconRes = DesignSystemR.drawable.ic_nav_ajustes,
            label = stringResource(R.string.nav_settings),
        )
    }
}

/**
 * Un destino de la barra de navegación inferior. El indicador de píldora detrás del icono
 * seleccionado (64x32dp `accentSoft`) se dibuja a mano porque Material 3 fija internamente su
 * ancho a `NavigationBarVerticalItemTokens.ActiveIndicatorWidth` (56dp, sin parámetro público para
 * cambiarlo): el indicador por defecto se desactiva con `indicatorColor = Color.Transparent`
 * (adopt-reference-mock-ui, design D7).
 */
@Composable
private fun RowScope.FraseAFraseBottomBarItem(
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    label: String,
) {
    NavigationBarItem(
        modifier = Modifier.testTag(tag),
        selected = selected,
        onClick = onClick,
        icon = {
            Box(contentAlignment = Alignment.Center) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .testTag("$tag-indicator")
                            .size(
                                width = AppTheme.dimensions.navIndicatorWidth,
                                height = AppTheme.dimensions.navIndicatorHeight,
                            )
                            .background(color = AppColors.AccentSoft, shape = RoundedCornerShape(percent = 50)),
                    )
                }
                Icon(
                    modifier = Modifier
                        .testTag("$tag-icon")
                        .size(AppTheme.dimensions.navIconSize),
                    imageVector = ImageVector.vectorResource(id = iconRes),
                    tint = if (selected) MaterialTheme.colorScheme.primary else AppColors.TextSecondary,
                    contentDescription = null,
                )
            }
        },
        label = {
            Text(
                modifier = Modifier.testTag("$tag-label"),
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onSurface else AppColors.TextSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        },
        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
    )
}
