# Proposal

## Why

Con las reglas de ADR-0025 la tanda sale menor que N cuando hay pocas pendientes: en emulador, con N = 10, las
primeras sesiones tuvieron 5 y 9 frases. El usuario ha fijado como prioridad 1 que las sesiones tengan siempre el
tamaño elegido mientras el catálogo del nivel lo permita.

## What Changes

- La tanda se rellena hasta N en este orden: pendientes por prioridad → nuevas dentro del límite de ADR-0025 →
  **repaso adelantado** de frases con progreso aún no pendientes → más nuevas si aún falta.
- La primera tanda de un nivel CEFR pasa de `floor(N × 0,5)` nuevas a `min(N, frases del nivel)`.
- Un **acierto** sobre una frase cuyo `dueAtSession` es posterior a la sesión actual no cambia nivel, racha ni
  `dueAtSession`; un fallo cuenta como siempre (ADR-0024).
- **BREAKING (comportamiento):** "no hay frases para practicar" solo aparece si el nivel CEFR elegido no tiene
  frases en el catálogo; con solo repasos futuros se compone una tanda de repaso adelantado.
- ADR-0026 (Propuesto) sustituye en parte a ADR-0025; se acepta al archivar.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `practice-batch`: composición hasta N, candidatas (repaso adelantado), número de nuevas con relleno, reparto
  con repaso adelantado y ausencia de tanda.
- `spaced-repetition`: resolución de un acierto sobre una frase aún no pendiente.
- `practice-session`: estado vacío solo sin frases del nivel elegido.
- `practice-persistence`: el primer uso crea una tanda de `min(N, frases del nivel)` nuevas.

## Impact

- ADR afectados: ADR-0025 (sustituido en parte por ADR-0026). Se respetan ADR-0021 (filtro CEFR exacto),
  ADR-0024 (fallo) y la terminología vigente (tanda, nivel, racha, pendiente, certificada). Las reglas de ADR-0012
  están sustituidas; el cambio queda dentro del alcance del MVP.
- Código: `:core:domain` (`ComposeNextBatch`, `PhraseProgress`). Sin cambios de esquema, puertos ni librerías.
- Tests que dependen del comportamiento antiguo: `ComposeNextBatchTest`, `PhraseProgressTest`,
  `ObtainPracticeSessionTest`, `ObtainActiveBatchTest`, `CloseThenObtainTest` (`:core:domain`) y
  `PracticeSessionViewModelTest`, `PracticeSessionCloseTest` (`:feature:session`), cuyos datos de "nada que
  practicar" usan repasos futuros.

## Fuera de alcance

- Composición concurrente al primer arranque y `hasSameBatchAs` sin idioma destino (siguiente cambio).
- Textos de ayuda CEFR y reglas antiguas en `CLAUDE.md` y comentarios (vía rápida posterior).
- Algoritmo anti-bloqueo y huecos reservados para fallos (ADR futuro).
- Cambios de UI: el resumen de la tanda sigue contando los aciertos de repaso adelantado como aciertos.
