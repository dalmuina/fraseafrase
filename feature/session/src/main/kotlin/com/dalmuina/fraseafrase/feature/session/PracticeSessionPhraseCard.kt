package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import com.dalmuina.fraseafrase.core.designsystem.component.DsInfoBanner
import com.dalmuina.fraseafrase.core.designsystem.component.DsInfoBannerSegment
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadgeChange
import com.dalmuina.fraseafrase.core.designsystem.motion.Motion
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppCornerShapes
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import com.dalmuina.fraseafrase.core.domain.spacedrepetition.Level
import kotlinx.coroutines.delay
import com.dalmuina.fraseafrase.core.designsystem.R as DesignSystemR

/**
 * Tarjeta con la frase en español, la traducción y el control de escuchar solo si está revelada, y el contador de
 * aciertos que faltan para la insignia del siguiente nivel (spec: practice-session).
 */
@Composable
internal fun PhraseCard(
    item: PracticeItem,
    revealed: Boolean,
    enabled: Boolean,
    remainingHitsForNextLevel: Int,
    nextLevelBadgeLabel: String,
    badgeChange: BadgeChange?,
    onAction: (PracticeSessionAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.m),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.spacing.l),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                PhraseCardHeader(
                    cefrLevel = item.cefrLevel,
                    level = item.level.value,
                    badgeChange = badgeChange,
                    revealed = revealed,
                    enabled = enabled,
                    onAction = onAction,
                )
                Text(text = item.source, style = MaterialTheme.typography.headlineSmall)
                DsInfoBanner(
                    icon = nextLevelBadgeIcon(item.level),
                    iconContentDescription = stringResource(
                        R.string.practice_remaining_hits_icon_description,
                        nextLevelBadgeLabel,
                    ),
                    segments = listOf(
                        DsInfoBannerSegment(text = stringResource(R.string.practice_remaining_hits_prefix)),
                        DsInfoBannerSegment(text = remainingHitsForNextLevel.toString(), emphasized = true),
                        DsInfoBannerSegment(text = stringResource(R.string.practice_remaining_hits_middle)),
                        DsInfoBannerSegment(text = nextLevelBadgeLabel, emphasized = true),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (revealed) {
            TranslationCard(target = item.target)
        }
    }
}

/**
 * Cabecera de [PhraseCard] (design `redesign-app-ui`, mock "Alternativa B · audio en la tarjeta"): a la
 * izquierda, la pastilla del nivel CEFR ([CefrLevelPill]) y la insignia de rango (o su transición
 * animada, [BadgeChangeIndicator]); a la derecha, el control de escuchar como icono simple
 * ([ListenButton]), solo mientras la frase está revelada. El control vivía bajo la traducción, dentro de
 * `TranslationCard`; se mueve aquí para que combine visualmente con las otras dos pastillas, sin
 * cambiar ninguna de sus condiciones de visibilidad o habilitación (spec: phrase-audio).
 */
@Composable
private fun PhraseCardHeader(
    cefrLevel: CefrLevel,
    level: Int,
    badgeChange: BadgeChange?,
    revealed: Boolean,
    enabled: Boolean,
    onAction: (PracticeSessionAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CefrLevelPill(cefrLevel = cefrLevel)
            if (badgeChange != null) {
                BadgeChangeIndicator(badgeChange = badgeChange, onAction = onAction)
            } else {
                DsLevelBadge(level = level)
            }
        }
        if (revealed) {
            ListenButton(enabled = enabled, onAction = onAction)
        }
    }
}

/** Pastilla del nivel CEFR de la frase: fondo `primary`, texto blanco (design `redesign-app-ui`, mock). */
@Composable
private fun CefrLevelPill(cefrLevel: CefrLevel) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = cefrLevel.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = AppTheme.spacing.s, vertical = AppTheme.spacing.xs),
        )
    }
}

/**
 * Control de escuchar como icono simple, sin texto visible junto a él en ningún estado en el que es
 * visible, incluido deshabilitado, con una descripción accesible equivalente al texto que ya no se
 * muestra (spec: phrase-audio, "Botón de escuchar tras revelar"; design D5, fix-redesign-ui-mismatches).
 *
 * Círculo de 44dp con el icono `ic_volume` del mock de referencia sobre fondo `accent` (primary), con la
 * sombra de color del mock (`box-shadow: 0 4px 12px rgba(91,61,245,.3)` en
 * `design/mock/screens/02-practicar-traduccion.html`), según `design/mock/design-tokens.md`, "Tamaños y
 * espaciado" (adopt-reference-mock-ui, design - decisión 2 y 3, tarea 5.3). Sustituye a
 * [Icons.Default.PlayArrow], elegido en su momento como aproximación provisional del altavoz porque
 * `Icons.Default.VolumeUp` solo estaba en `material-icons-extended`, sin declarar entonces por
 * `:feature:session`; `ic_volume` ya está empaquetado en `:core:designsystem` sin esa dependencia.
 */
@Composable
private fun ListenButton(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    FilledIconButton(
        onClick = { onAction(PracticeSessionAction.PlayAudio) },
        enabled = enabled,
        modifier = Modifier
            .size(AppTheme.dimensions.listenButtonSize)
            .shadow(
                elevation = AppTheme.spacing.xs,
                shape = CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary,
            )
            .testTag("practice_listen_button"),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_volume),
            contentDescription = stringResource(R.string.practice_listen),
            modifier = Modifier
                .size(AppTheme.dimensions.listenButtonIconSize)
                .testTag("listen_icon"),
        )
    }
}

/**
 * Tracking de la etiqueta "TRADUCCIÓN" (`design/mock/design-tokens.md`, "Tipografía": +0.8), aplicado
 * con `.copy()` sobre `labelMedium` en vez de en el token compartido, igual que documenta `Type.kt` para
 * este mismo caso (dos usos del mismo slot con valores de tracking distintos).
 */
private val TranslationLabelLetterSpacing = 0.8.sp

/**
 * Panel de la traducción revelada (spec: practice-session); el control de escuchar vive en la cabecera.
 * Fondo `accentSoft`, radio `AppCornerShapes.Panel` (20dp) y etiqueta "TRADUCCIÓN" en mayúsculas sobre
 * ella (adopt-reference-mock-ui, design - decisión 2, tarea 5.1).
 */
@Composable
private fun TranslationCard(target: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("practice_translation_card"),
        shape = AppCornerShapes.Panel,
        color = AppColors.AccentSoft,
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            Text(
                text = stringResource(R.string.practice_translation_label).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = TranslationLabelLetterSpacing),
                color = AppColors.AccentStrong,
            )
            Text(
                text = target,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Indicador animado de cambio de insignia (spec: add-level-change-animation), en el sitio de
 * [DsLevelBadge] dentro de la cabecera de [PhraseCard]: la insignia de la propia frase respondida
 * transiciona ahí, sin ocultar el resto de la tarjeta (spec: new-badge-animation, design, Decisión 2).
 * Se oculta sola, pasada la duración correspondiente a su [BadgeChange.direction], sin bloquear los
 * controles de esa misma frase, que siguen presentes (solo deshabilitados por `busy`).
 */
@Composable
private fun BadgeChangeIndicator(
    badgeChange: BadgeChange,
    onAction: (PracticeSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = AppTheme.motion
    LaunchedEffect(badgeChange) {
        delay(badgeChange.direction.visibleDurationMillis(motion))
        onAction(PracticeSessionAction.ConsumeBadgeChange)
    }
    DsLevelBadgeChange(
        fromLevel = badgeChange.fromLevel,
        toLevel = badgeChange.toLevel,
        emphasized = badgeChange.direction == BadgeChange.Direction.UP,
        modifier = modifier.testTag("practice_badge_change_indicator"),
    )
}

/**
 * Duración total que el indicador permanece visible antes de consumirse, acorde a los mismos tokens de
 * [Motion] con los que [DsLevelBadgeChange] resuelve su propia animación (design.md D4): más larga para
 * una subida que para una bajada.
 */
private fun BadgeChange.Direction.visibleDurationMillis(motion: Motion): Long = when (this) {
    BadgeChange.Direction.UP ->
        (motion.durationShortMillis + motion.durationLongMillis + motion.durationMediumMillis).toLong()
    BadgeChange.Direction.DOWN ->
        (motion.durationMediumMillis + motion.durationShortMillis + motion.durationMediumMillis).toLong()
}

/**
 * Insignia del icono del mensaje "Te faltan N aciertos para X" (spec: practice-session, "Aciertos para
 * el próximo nivel visibles"; design.md decisión 8, adopt-reference-mock-ui): la del nivel SIGUIENTE a
 * [level], nunca un icono genérico ni la del nivel actual (`design/mock/design-tokens.md`, "El icono...
 * es la insignia del rango X, el siguiente, no una estrella ni el rango actual").
 */
@Composable
private fun nextLevelBadgeIcon(level: Level): ImageVector {
    val nextLevel = (level.value + 1).coerceAtMost(Level.MAX_LEVEL)
    return ImageVector.vectorResource(id = nextLevelBadgeIconRes(nextLevel))
}

private const val LEVEL_BRONCE = 1
private const val LEVEL_PLATA = 2
private const val LEVEL_ORO = 3
private const val LEVEL_PLATINO = 4

/** Icono multicolor de la insignia de rango de 1 (Bronce) a 5 (Diamante), sin tinte (ADR-0010). */
private fun nextLevelBadgeIconRes(nextLevel: Int): Int = when (nextLevel) {
    LEVEL_BRONCE -> DesignSystemR.drawable.ic_rank_bronce
    LEVEL_PLATA -> DesignSystemR.drawable.ic_rank_plata
    LEVEL_ORO -> DesignSystemR.drawable.ic_rank_oro
    LEVEL_PLATINO -> DesignSystemR.drawable.ic_rank_platino
    else -> DesignSystemR.drawable.ic_rank_diamante
}
