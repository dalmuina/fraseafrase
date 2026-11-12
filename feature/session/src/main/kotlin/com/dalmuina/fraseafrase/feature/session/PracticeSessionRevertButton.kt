package com.dalmuina.fraseafrase.feature.session

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

/**
 * Botón, con la forma de un `FloatingActionButton`, apilado encima del botón principal/los de autoevaluar
 * en [PracticeSessionScreen] para deshacer la autoevaluación más reciente de la tanda activa (spec:
 * revert-selection; design D6, `fix-redesign-ui-mismatches`). Ya no vive en el slot `floatingActionButton`
 * del `Scaffold` (pensado para flotar sobre el contenido, no para reservar su propio espacio), sino dentro
 * de la misma `Column` que esos botones, envuelto en `AnimatedVisibility` en el lugar de la llamada. Solo se
 * pinta mientras `canRevertLastAnswer` es `true`. Al pulsarlo, emite [PracticeSessionAction.RevertLastAnswer].
 * Usa la variante `secondary` (naranja) del tema (design `redesign-app-ui`, mock) para distinguirlo del resto
 * de controles, que usan `primary`/`tertiary`/`error`.
 *
 * Usa `Icons.AutoMirrored.Filled.Undo` (icono de recarga/deshacer, no una flecha de "volver atrás") en vez
 * de `Icons.AutoMirrored.Filled.ArrowBack` (design D6). A diferencia de `ListenButton` y `RevealButton`
 * (`PracticeSessionPhraseCard.kt`/`PracticeSessionScreen.kt`), que evitaron `material-icons-extended`
 * eligiendo un icono equivalente ya disponible en `material-icons-core`, aquí no hay ninguna alternativa en
 * `material-icons-core` que no sea otra vez una flecha (la propia `ArrowBack`), así que se añade
 * `material-icons-extended` como dependencia de `:feature:session` (`feature/session/build.gradle.kts`),
 * igual que ya hacen `:core:designsystem` y `:app`.
 */
@Composable
internal fun RevertLastAnswerButton(onAction: (PracticeSessionAction) -> Unit) {
    FloatingActionButton(
        onClick = { onAction(PracticeSessionAction.RevertLastAnswer) },
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.testTag("practice_revert_last_answer_button"),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = stringResource(R.string.practice_revert_last_answer),
            modifier = Modifier.testTag("practice_revert_last_answer_icon"),
        )
    }
}
