package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadge
import com.dalmuina.fraseafrase.core.designsystem.component.DsLevelBadgeChange
import com.dalmuina.fraseafrase.core.designsystem.motion.Motion
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.domain.catalog.CefrLevel
import com.dalmuina.fraseafrase.core.domain.practice.PracticeItem
import kotlinx.coroutines.delay

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
                Text(
                    text = stringResource(
                        R.string.practice_remaining_hits_for_next_level,
                        remainingHitsForNextLevel,
                        nextLevelBadgeLabel,
                    ),
                    style = MaterialTheme.typography.labelLarge,
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
 * animada, [BadgeChangeIndicator]); a la derecha, el control de escuchar como pastilla ([ListenPill]),
 * solo mientras la frase está revelada. El control vivía bajo la traducción, dentro de
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
            ListenPill(enabled = enabled, onAction = onAction)
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
 * Control de escuchar como pastilla, con el mismo estilo que las otras dos de la cabecera (design
 * `redesign-app-ui`, mock): icono y etiqueta de texto siempre visibles juntos, también deshabilitado
 * (spec: phrase-audio, "Botón de escuchar tras revelar").
 */
@Composable
private fun ListenPill(enabled: Boolean, onAction: (PracticeSessionAction) -> Unit) {
    Surface(
        onClick = { onAction(PracticeSessionAction.PlayAudio) },
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppTheme.spacing.s, vertical = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.testTag("listen_icon"),
            )
            Text(text = stringResource(R.string.practice_listen), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Tarjeta de la traducción revelada (spec: practice-session); el control de escuchar vive en la cabecera. */
@Composable
private fun TranslationCard(target: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            text = target,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(AppTheme.spacing.l),
        )
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
