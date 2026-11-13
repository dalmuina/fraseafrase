package com.dalmuina.fraseafrase.feature.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dalmuina.fraseafrase.core.designsystem.component.DsProgressBar
import com.dalmuina.fraseafrase.core.designsystem.component.DsStatTile
import com.dalmuina.fraseafrase.core.designsystem.component.DsStatTileSize
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppCornerShapes
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment
import com.dalmuina.fraseafrase.core.designsystem.R as DesignSystemR

/**
 * Pantalla de práctica sin estado (ADR-0006): pinta [uiState] y reenvía las acciones con [onAction]. La conexión con el
 * ViewModel está en [PracticeSessionRoute].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PracticeSessionScreen(
    uiState: PracticeSessionUiState,
    onAction: (PracticeSessionAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProgress: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (uiState is PracticeSessionUiState.Practicing && !uiState.completed) {
                PracticeHeader(index = uiState.index, total = uiState.total)
            }
        },
    ) { innerPadding ->
        // Caja de pantalla completa, sin más padding que el del `topBar` (`innerPadding`): aloja el
        // contenido centrado de siempre y, como hermano suyo, el FAB de revertir posicionado con offsets
        // absolutos desde sus bordes (adopt-reference-mock-ui, tarea 6.1, `design/mock/design-tokens.md`
        // "Botón flotante de volver": 20dp desde la izquierda, 172dp desde abajo), en vez de contar desde el
        // padding adicional de la zona de contenido.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("practice_session_content_area"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                when (uiState) {
                    PracticeSessionUiState.Loading -> CircularProgressIndicator()
                    PracticeSessionUiState.NothingToPractice -> EmptyMessage(
                        text = stringResource(R.string.practice_nothing_to_practice),
                    )
                    PracticeSessionUiState.CatalogError -> EmptyMessage(
                        text = stringResource(R.string.practice_catalog_error),
                    )
                    is PracticeSessionUiState.Practicing ->
                        PracticingContent(uiState, onAction, onNavigateToProgress)
                }
            }
            if (uiState is PracticeSessionUiState.Practicing && !uiState.completed) {
                // Apilado encima del botón principal, no superpuesto (design D6, fix-redesign-ui-mismatches):
                // ya no vive en el slot `floatingActionButton` del `Scaffold`, que reservaba su propio espacio
                // flotante sobre el contenido en vez de desplazarlo. `AnimatedVisibility` repone la animación
                // de aparición/desaparición que ese slot daba automáticamente.
                AnimatedVisibility(
                    visible = uiState.canRevertLastAnswer,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(
                            x = AppTheme.dimensions.revertFabHorizontalOffset,
                            y = -AppTheme.dimensions.revertFabBottomOffset,
                        ),
                ) {
                    RevertLastAnswerButton(onAction = onAction)
                }
            }
        }
    }
}

/**
 * Cabecera fija de Practicar: título, píldora de posición en la tanda y barra de progreso (spec:
 * practice-session, "Posición en la tanda visible"; design D1 de `fix-style-pass-two`). Paddings, gap y
 * alto de barra ajustados a los valores exactos del mock (adopt-reference-mock-ui, design - decisión 2,
 * `design/mock/design-tokens.md` "Cabecera": 24 arriba, 20 lados, 12 abajo, 14 entre título y barra, barra
 * de 8 de alto; se aproximan con la escala de [AppTheme.spacing] ya existente, sin introducir `dp`
 * literales nuevos, igual que ya hace [PhraseCard] con el padding de 20 de la tarjeta de frase).
 */
@Composable
private fun PracticeHeader(index: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                PaddingValues(
                    start = AppTheme.spacing.l,
                    end = AppTheme.spacing.l,
                    top = AppTheme.spacing.l,
                    bottom = AppTheme.spacing.s,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.practice_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = AppColors.AccentSoft,
            ) {
                Text(
                    text = stringResource(R.string.practice_position, index, total),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.AccentStrong,
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                )
            }
        }
        DsProgressBar(
            progress = index / total.toFloat(),
            modifier = Modifier
                .height(AppTheme.spacing.s)
                .testTag("practice_progress_indicator"),
        )
    }
}

/**
 * Frase actual con sus botones de revelar o autoevaluar, o el mensaje de tanda completada en su lugar.
 *
 * La frase y la traducción viven en una zona desplazable con [Modifier.weight] para que, si no caben en
 * el espacio disponible, el usuario pueda desplazarlas; el control de revelar o los de acierto/fallo
 * quedan fuera de esa zona, al final de la columna, para que nunca se recorten ni se muevan (spec:
 * practice-session, "Controles de acción siempre visibles y pulsables"; design D2).
 */
@Composable
private fun PracticingContent(
    state: PracticeSessionUiState.Practicing,
    onAction: (PracticeSessionAction) -> Unit,
    onNavigateToProgress: () -> Unit,
) {
    if (state.completed) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
        ) {
            CompletedMessage(
                hits = state.hits,
                misses = state.misses,
                levelUps = state.levelUps,
                enabled = !state.busy,
                onAction = onAction,
                onNavigateToProgress = onNavigateToProgress,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("practice_scrollable_content"),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.l),
            ) {
                PhraseCard(
                    item = state.current,
                    revealed = state.revealed,
                    enabled = !state.busy,
                    remainingHitsForNextLevel = state.remainingHitsForNextLevel,
                    nextLevelBadgeLabel = state.nextLevelBadgeLabel,
                    badgeChange = state.badgeChange,
                    onAction = onAction,
                )
                if (!state.revealed) {
                    TranslateMentallyHint()
                }
            }
            // El FAB de revertir ya no vive aquí dentro, apilado en esta misma `Column`: vive como hermano de
            // esta `Column` en [PracticeSessionScreen], posicionado con offsets absolutos desde los bordes de
            // la pantalla (adopt-reference-mock-ui, tarea 6.1).
            if (state.revealed) {
                AssessmentButtons(enabled = !state.busy, onAction = onAction)
            } else {
                RevealButton(onAction = onAction)
            }
        }
    }
}

/**
 * Botón para revelar la traducción, con icono y 56dp de alto (design D8, fix-redesign-ui-mismatches).
 *
 * Usa el `VectorDrawable` `ic_eye` del mock de referencia (adopt-reference-mock-ui, design - decisión 3)
 * en vez de [Icons.Default.Search], que se eligió en su momento como sustituto provisional del ojo
 * (`Icons.Default.Visibility`, solo disponible entonces en `material-icons-extended`, no declarada por
 * `:feature:session`); `ic_eye` ya está empaquetado en `:core:designsystem` sin esa dependencia.
 */
@Composable
private fun RevealButton(onAction: (PracticeSessionAction) -> Unit) {
    Button(
        onClick = { onAction(PracticeSessionAction.Reveal) },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.primaryActionHeight)
            .testTag("practice_reveal_button"),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_eye),
            contentDescription = null,
            modifier = Modifier
                .size(ButtonDefaults.IconSize)
                .testTag("practice_reveal_icon"),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.practice_reveal))
    }
}

/**
 * Anchura del trazo discontinuo de [TranslateMentallyHint] (`design/mock/design-tokens.md`, "Radios":
 * borde 2 de la zona discontinua).
 */
private val TranslateMentallyHintBorderWidth = 2.dp

/** Longitud de cada guion y hueco del trazo discontinuo de [TranslateMentallyHint]. */
private val TranslateMentallyHintDashLength = 6.dp

/**
 * Cuadro de ayuda con borde discontinuo que invita a traducir la frase mentalmente antes de comprobar la
 * traducción (spec: practice-session, "Mensaje de ayuda para traducir mentalmente";
 * adopt-reference-mock-ui, design - decisión 5): visible solo mientras la frase actual no está revelada.
 * Nace como composable privado de esta pantalla porque ninguna otra feature lo necesita todavía (regla de
 * promoción del proyecto a `:core:designsystem`).
 */
@Composable
private fun TranslateMentallyHint() {
    val borderColor = AppColors.DashedBorder
    val shape = AppCornerShapes.Panel
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(shape.topStart.toPx(size, this)),
                    style = Stroke(
                        width = TranslateMentallyHintBorderWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                TranslateMentallyHintDashLength.toPx(),
                                TranslateMentallyHintDashLength.toPx(),
                            ),
                        ),
                    ),
                )
            }
            .padding(AppTheme.spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.practice_translate_mentally_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Insignia compuesta de felicitación de [CompletedMessage] (`design/mock/design-tokens.md`, "Sesión
 * completada"; adopt-reference-mock-ui, design - decisión 6): hexágono exterior en `accentSoft`,
 * hexágono interior en `accent` centrado dentro de él, check blanco centrado sobre ambos
 * (`ic_check_complete`) y un mini-hexágono decorativo en la esquina superior derecha con el color del
 * FAB (`ic_hex_solid`, también usado para los dos hexágonos: el mismo vector monocromo tintable,
 * variando solo tamaño, color y posición).
 *
 * Los tamaños y el desplazamiento viven como variables locales, en vez de constantes de nivel de
 * archivo, para que Kover los excluya junto con el resto del cuerpo de este composable (ADR-0007: la
 * UI se verifica con tests de UI, no con cobertura de líneas); una constante de nivel de archivo
 * acaba en el inicializador estático de la clase, que no está anotado con `@Composable` y sí cuenta
 * para la cobertura de líneas de `:feature:session`.
 */
@Composable
private fun CompletedBadge() {
    val outerHexSize = 128.dp
    val innerHexSize = 92.dp
    val checkSize = 44.dp
    val miniHexSize = 28.dp
    // Desplazamiento del mini-hexágono decorativo desde la esquina superior derecha del hexágono
    // exterior (mock: `right: -6px; top: 4px`), hacia fuera y hacia abajo.
    val miniHexOffsetX = 6.dp
    val miniHexOffsetY = 4.dp
    Box(
        modifier = Modifier.size(outerHexSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_hex_solid),
            contentDescription = null,
            tint = AppColors.AccentSoft,
            modifier = Modifier.size(outerHexSize),
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_hex_solid),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(innerHexSize),
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_check_complete),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(checkSize)
                .testTag("practice_completed_success_icon"),
        )
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_hex_solid),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .size(miniHexSize)
                .align(Alignment.TopEnd)
                .offset(x = miniHexOffsetX, y = miniHexOffsetY)
                .testTag("practice_completed_badge_mini_hex"),
        )
    }
}

/**
 * Mensaje positivo al completar una vuelta a la tanda (D7), con el desglose de aciertos, fallos y subidas
 * de nivel de esa misma tanda (spec: practice-session, "El mensaje muestra el desglose de aciertos,
 * fallos y subidas de nivel"), el botón para generar la siguiente sesión y un enlace a Progreso.
 */
@Composable
private fun CompletedMessage(
    hits: Int,
    misses: Int,
    levelUps: Int,
    enabled: Boolean,
    onAction: (PracticeSessionAction) -> Unit,
    onNavigateToProgress: () -> Unit,
) {
    CompletedBadge()
    Text(
        text = stringResource(R.string.practice_batch_completed),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.practice_batch_completed_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    OutcomeBreakdownRow(hits = hits, misses = misses, levelUps = levelUps)
    GenerateSessionButton(enabled = enabled, onAction = onAction)
    TextButton(onClick = onNavigateToProgress) {
        Text(
            text = stringResource(R.string.practice_view_progress),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Fila con las tres tarjetas de resultado de [CompletedMessage], con colores diferenciados por rol
 * (verde para aciertos, rojo para fallos, naranja para "sube de rango") y la misma altura entre las
 * tres aunque alguna etiqueta ocupe más líneas que otra (spec: practice-session, "El mensaje muestra
 * el desglose de aciertos, fallos y subidas de nivel"; design D4, fix-redesign-ui-mismatches).
 */
@Composable
private fun OutcomeBreakdownRow(hits: Int, misses: Int, levelUps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        DsStatTile(
            value = hits.toString(),
            label = stringResource(R.string.practice_outcome_hits),
            size = DsStatTileSize.Compact,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("practice_outcome_hits_tile"),
        )
        DsStatTile(
            value = misses.toString(),
            label = stringResource(R.string.practice_outcome_misses),
            size = DsStatTileSize.Compact,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("practice_outcome_misses_tile"),
        )
        DsStatTile(
            value = levelUps.toString(),
            label = stringResource(R.string.practice_outcome_level_ups),
            size = DsStatTileSize.Compact,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("practice_outcome_level_ups_tile"),
        )
    }
}

/** Botón para generar la siguiente sesión, con icono y 56dp de alto (design D8, fix-redesign-ui-mismatches). */
@Composable
private fun GenerateSessionButton(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Button(
        onClick = { onAction(PracticeSessionAction.GenerateSession) },
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.primaryActionHeight),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_new_session),
            contentDescription = null,
            modifier = Modifier
                .size(ButtonDefaults.IconSize)
                .testTag("practice_generate_session_icon"),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(R.string.practice_generate_session))
    }
}

/**
 * Anchura del borde de los botones Fallo/Acierto (`design/mock/design-tokens.md`, "Tamaños y
 * espaciado": "Botones Fallo/Acierto: ... borde 2").
 */
private val AssessmentButtonBorderWidth = 2.dp

/**
 * Acierto y fallo para la frase revelada (ADR-0012). Se deshabilitan mientras hay una respuesta en
 * curso. Colores e iconos exactos del mock (adopt-reference-mock-ui, design - decisión 2 y 3, tarea
 * 5.2): Fallo con borde `error`, fondo `errorBg` y texto `errorText`; Acierto con fondo `success` y
 * texto blanco. `ic_close`/`ic_check` sustituyen a [Icons.Default.Close]/[Icons.Default.Check].
 */
@Composable
private fun AssessmentButtons(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
    ) {
        OutlinedButton(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.MISS)) },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AppColors.ErrorBg,
                contentColor = AppColors.ErrorText,
            ),
            border = BorderStroke(width = AssessmentButtonBorderWidth, color = AppColors.Error),
            modifier = Modifier
                .weight(1f)
                .height(AppTheme.dimensions.primaryActionHeight)
                .testTag("practice_miss_button"),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier
                    .size(ButtonDefaults.IconSize)
                    .testTag("practice_miss_icon"),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.practice_miss))
        }
        Button(
            onClick = { onAction(PracticeSessionAction.Answer(SelfAssessment.HIT)) },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Success,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .weight(1f)
                .height(AppTheme.dimensions.primaryActionHeight)
                .testTag("practice_hit_button"),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier
                    .size(ButtonDefaults.IconSize)
                    .testTag("practice_hit_icon"),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.practice_hit))
        }
    }
}
