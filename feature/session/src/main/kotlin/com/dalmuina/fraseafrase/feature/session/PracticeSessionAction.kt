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

    /**
     * Pide generar la sesión siguiente, tras completar una vuelta a la tanda activa (ADR-0012, spec:
     * practice-session). Se ignora mientras hay una respuesta en curso.
     */
    data object GenerateSession : PracticeSessionAction

    /**
     * Vuelve a leer la sesión al entrar en la pestaña (design D4 de `add-settings-apply-flow`). Solo sustituye el
     * estado si la tanda leída es distinta de la ya cargada; si es la misma, no toca `revealed`, `busy`,
     * `completed` ni `index`.
     */
    data object Resume : PracticeSessionAction

    /**
     * Confirma que el indicador de cambio de insignia ya se mostró: lo limpia y libera los controles
     * (spec: add-level-change-animation, design D2/D3).
     */
    data object ConsumeBadgeChange : PracticeSessionAction

    /**
     * Revierte la autoevaluación más reciente de la tanda activa, devolviendo la frase afectada a su posición,
     * nivel y racha anteriores (spec: revert-selection). Se ignora si no hay ninguna autoevaluación que revertir
     * o mientras hay una respuesta en curso (design D5).
     */
    data object RevertLastAnswer : PracticeSessionAction
}
