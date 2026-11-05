package com.dalmuina.fraseafrase.feature.session

import com.dalmuina.fraseafrase.core.domain.spacedrepetition.SelfAssessment

/** Acciones del usuario sobre la pantalla de práctica. Llegan por un único `onAction()` (ADR-0006). */
sealed interface PracticeSessionAction {

    /** Muestra la traducción en portugués de la frase actual. */
    data object Reveal : PracticeSessionAction

    /** Autoevalúa la frase revelada como acierto o fallo (ADR-0012). */
    data class Answer(val assessment: SelfAssessment) : PracticeSessionAction

    /** Reproduce el audio en portugués de la frase revelada (spec: phrase-audio). */
    data object PlayAudio : PracticeSessionAction

    /** Pide cerrar la tanda: abre el aviso con el número de frases no certificadas (ADR-0012). */
    data object RequestClose : PracticeSessionAction

    /** Cancela el aviso de cierre: la tanda no cambia. */
    data object CancelClose : PracticeSessionAction

    /** Confirma el cierre con el número que mostró el aviso. */
    data object ConfirmClose : PracticeSessionAction

    /**
     * Vuelve a leer la sesión al entrar en la pestaña (design D4 de `add-settings-apply-flow`). Solo sustituye el
     * estado si la tanda leída es distinta de la ya cargada; si es la misma, no toca `revealed`, `busy`,
     * `closeWarning` ni `index`.
     */
    data object Resume : PracticeSessionAction
}
