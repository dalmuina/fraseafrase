package com.dalmuina.fraseafrase.feature.session

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.dalmuina.fraseafrase.core.designsystem.theme.AppColors
import com.dalmuina.fraseafrase.core.designsystem.theme.AppCornerShapes
import com.dalmuina.fraseafrase.core.designsystem.theme.AppTheme
import com.dalmuina.fraseafrase.core.designsystem.R as DesignSystemR

/**
 * Botón, con la forma de un `FloatingActionButton`, para deshacer la autoevaluación más reciente de la
 * tanda activa (spec: revert-selection; design D6, `fix-redesign-ui-mismatches`). Ya no vive en el slot
 * `floatingActionButton` del `Scaffold` (pensado para flotar sobre el contenido, no para reservar su propio
 * espacio), sino como hermano, posicionado con offsets absolutos desde los bordes de la pantalla, de la
 * `Column` de contenido de [PracticeSessionScreen] (adopt-reference-mock-ui, tarea 6.1), envuelto en
 * `AnimatedVisibility` en el lugar de la llamada. Solo se pinta mientras `canRevertLastAnswer` es `true` y
 * la tanda no está completada. Al pulsarlo, emite [PracticeSessionAction.RevertLastAnswer]. Usa la variante
 * `secondary` (naranja) del tema (design `redesign-app-ui`, mock) para distinguirlo del resto de controles,
 * que usan `primary`/`tertiary`/`error`.
 *
 * Usa el `VectorDrawable` `ic_undo_previous` del mock de referencia (adopt-reference-mock-ui, design -
 * decisión 3, tarea 6.1) en vez de `Icons.AutoMirrored.Filled.Undo`, que se eligió en su momento como
 * sustituto provisional (solo disponible entonces en `material-icons-extended`, no declarada por
 * `:feature:session` hasta ese punto); `ic_undo_previous` ya está empaquetado en `:core:designsystem` sin
 * esa dependencia. El color de contenido usa el token dedicado [AppColors.FabContent] (`#2B0E02`), distinto
 * de `onSecondary` (reservado para el mensaje "Te faltan N aciertos..."), y el radio usa
 * [AppCornerShapes.Fab] (18dp) en vez del redondeo por defecto del `FloatingActionButton`.
 */
@Composable
internal fun RevertLastAnswerButton(onAction: (PracticeSessionAction) -> Unit) {
    FloatingActionButton(
        onClick = { onAction(PracticeSessionAction.RevertLastAnswer) },
        shape = AppCornerShapes.Fab,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = AppColors.FabContent,
        modifier = Modifier
            .size(AppTheme.dimensions.revertFabSize)
            .testTag("practice_revert_last_answer_button"),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = DesignSystemR.drawable.ic_undo_previous),
            contentDescription = stringResource(R.string.practice_revert_last_answer),
            modifier = Modifier
                .size(AppTheme.dimensions.revertFabIconSize)
                .testTag("practice_revert_last_answer_icon"),
        )
    }
}
