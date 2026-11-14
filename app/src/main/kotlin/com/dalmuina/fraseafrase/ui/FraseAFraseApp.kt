package com.dalmuina.fraseafrase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import com.dalmuina.fraseafrase.core.designsystem.theme.AppCornerShapes
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
    // La barra flotante ya no es el `bottomBar` del `Scaffold` (design D2, bottom-bar-navigation):
    // vive como overlay hermano del contenido, igual que `RevertLastAnswerButton` en
    // `:feature:session`, para poder controlar ella misma su `windowInsetsPadding` sin el recorte de
    // altura fija que causaba el defecto original. El `SnackbarHost` sigue en el mismo slot del
    // `Scaffold` raíz (no cambia de posición en el árbol de dibujado).
    // Hueco reservado para la barra flotante: su alto de contenido más el margen flotante a ambos
    // lados (arriba y abajo de la `Row`, design D1/D4). Se suma a los insets por defecto del
    // `Scaffold` (en vez de pasarse como `bottomBar`, que ya no existe) para que tanto el contenido
    // de cada pantalla como el `SnackbarHost` -- ambos posicionados por el propio `Scaffold` a partir
    // de `contentWindowInsets` -- queden igualmente por encima de la barra flotante (design.md,
    // Riesgos: la confirmación de guardado no debe quedar tapada por ella).
    val bottomBarReservedHeight = AppTheme.dimensions.navHeight + AppTheme.dimensions.navFloatingMargin * 2
    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                .add(WindowInsets(bottom = bottomBarReservedHeight)),
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
        FraseAFraseBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedDestination = selectedDestination,
            onDestinationSelected = { selectedDestination = it },
        )
    }
}

/**
 * Barra de navegación inferior propiamente dicha, extraída de [FraseAFraseApp] para mantener su
 * longitud dentro del límite de detekt. Cada destino muestra uno de los `ic_nav_*` del mock además
 * de su etiqueta de texto (spec `app-navigation`, design D5; adopt-reference-mock-ui design D7).
 *
 * Superficie flotante en cápsula, no `NavigationBar` a sangre (bottom-bar-navigation, design D1):
 * aplica ella misma el inset de la barra de navegación del sistema y, encima, el margen flotante
 * (design D1/D4), en vez de depender de los insets internos de `NavigationBar` combinados con una
 * altura externa fija -- eso era precisamente lo que recortaba el contenido de los tres destinos
 * contra la barra del sistema.
 */
@Composable
private fun FraseAFraseBottomBar(
    selectedDestination: BottomDestination,
    onDestinationSelected: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // El margen flotante se aplica en un `Box` envolvente, no directamente en el `Surface`: los
    // destinos de `NavigationBarItem` reparten el ancho con `Modifier.weight` (interno de Material 3),
    // así que un `Surface` con margen y contenido ponderado en la misma cadena de modificadores acaba
    // ocupando igualmente el ancho completo disponible (el margen se "cancela" matemáticamente); al
    // reservar el margen en el `Box` exterior, el `Surface` interior sí queda con el ancho reducido.
    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(AppTheme.dimensions.navFloatingMargin),
    ) {
        Surface(
            modifier = Modifier.testTag("bottom-nav-bar"),
            shape = AppCornerShapes.FloatingNav,
            shadowElevation = AppTheme.dimensions.navFloatingElevation,
        ) {
            Row(modifier = Modifier.height(AppTheme.dimensions.navHeight)) {
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
                            .background(color = AppColors.AccentSoft, shape = AppCornerShapes.FloatingNav),
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
