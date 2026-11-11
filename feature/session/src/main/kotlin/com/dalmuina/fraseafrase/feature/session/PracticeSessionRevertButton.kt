package com.dalmuina.fraseafrase.feature.session

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/**
 * Botón flotante en la esquina inferior izquierda de [PracticeSessionScreen] para deshacer la autoevaluación
 * más reciente de la tanda activa (spec: revert-selection; design D6). Solo se pinta mientras
 * `canRevertLastAnswer` es `true`. Al pulsarlo, emite [PracticeSessionAction.RevertLastAnswer].
 */
@Composable
internal fun RevertLastAnswerButton(onAction: (PracticeSessionAction) -> Unit) {
    FloatingActionButton(
        onClick = { onAction(PracticeSessionAction.RevertLastAnswer) },
        modifier = Modifier.testTag("practice_revert_last_answer_button"),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.practice_revert_last_answer),
        )
    }
}
