# Proposal

## Why

Hoy la práctica obliga al usuario a gestionar dos parámetros a la vez por frase (racha y nivel de consolidación)
y a decidir explícitamente cuándo "cerrar tanda" tras ver un aviso con el número de frases no certificadas. Esto
no aporta valor: una tanda se practica y, cuando se ha dado una vuelta completa, lo natural es ofrecer generar la
siguiente. Además, la composición de la tanda siguiente no reserva hueco para repasar frases ya dominadas, y un
cambio de nivel CEFR en Ajustes certifica y resetea la racha de toda la tanda activa como si fuera un cierre
normal, cuando debería limitarse a cambiar qué frases se ofrecen.

## What Changes

- **BREAKING**: se elimina la acción "cerrar tanda" con aviso previo de no certificadas
  (`RequestClose`/`CancelClose`/`ConfirmClose`). Se sustituye por "generar sesión": al completar una vuelta
  completa a la tanda activa (todas sus frases con `answeredInBatch = true`), la pantalla muestra un mensaje
  positivo con la opción de generar la siguiente sesión; esa acción resuelve la tanda (certifica, sube/mantiene
  nivel, actualiza arrastradas) y compone la siguiente en un solo paso, sin aviso ni confirmación.
- **BREAKING**: se elimina el indicador de racha ("Racha N") y la marca "Sabida" de la pantalla de práctica. Se
  sustituyen por un único indicador "N aciertos para `<insignia>`", donde N es `max(0, 3 - racha)` e
  `<insignia>` es el nombre de la insignia de nivel `min(nivel + 1, 5)` (contador literal, sin texto especial al
  certificar ni en nivel máximo).
- La composición de la tanda siguiente deja de llenarse solo por prioridad estricta: reserva cuotas fijas del
  tamaño de tanda N — ~40 % frases nuevas, ~40 % a reforzar (arrastradas y pendientes, con las arrastradas con
  prioridad absoluta dentro de este bloque, igual que hoy), ~20 % frases ya dominadas (nivel de consolidación 4 o
  5) para repaso ligero de retención, aunque no estén aún pendientes de repaso. Si un bloque no tiene suficiente
  material, el hueco lo cubren los otros en el orden reforzar &gt; nuevas &gt; dominadas.
- Cambiar el nivel CEFR en Ajustes recompone la tanda activa de inmediato y de forma invisible, pero **sin**
  ejecutar la resolución de cierre: no certifica, no sube ni baja ningún nivel de consolidación y no toca ningún
  `nextReview`. Solo cambia qué frases están activas, con las mismas cuotas de composición. Cambiar solo N (sin
  cambiar el CEFR) sigue forzando el cierre resuelto como hoy.
- Se promueve el nombre de cada insignia de nivel de consolidación (hoy privado en `DsLevelBadge`) a una función
  pública del design system, para reutilizarlo en el nuevo indicador de práctica.

## Fuera de alcance

- No cambia el cálculo de racha, nivel ni intervalos de repaso (`spaced-repetition`): acierto/fallo, bajada
  instantánea de nivel (ADR-0015) y `nextReviewAfter` siguen igual.
- No cambia el comportamiento al cambiar N en Ajustes, solo o junto con el nivel CEFR: en ambos casos sigue
  forzando el cierre resuelto (certifica, resetea racha), como ya hace `ForceCloseActiveBatch` hoy. Solo cambiar
  el nivel CEFR en solitario evita ese cierre.
- No cambia la pantalla de Progreso ni las insignias en sí (`level-badges`, `progress-overview`): solo se expone
  públicamente un nombre que ya existía de forma privada.
- No cambia la persistencia de ajustes (`user-settings`): valores por defecto, rango de N y niveles CEFR válidos
  siguen igual.
- No introduce niveles de usuario globales ni cambia el catálogo de frases.

## ADR

Este cambio sustituye, en parte, a ADR-0015 (bajada instantánea de nivel, que a su vez sustituía a ADR-0012
salvo en esa regla): mantiene vigente la lista circular, la racha, la certificación por racha ≥ 3 y la bajada
instantánea de nivel, pero sustituye el cierre manual con aviso por la generación de sesión, y la composición
estrictamente por prioridad por una composición por cuotas. Se redacta
`docs/adr/0016-generar-sesion-por-cuotas.md`, estado `Propuesto`, como parte de esta propuesta, y se revisa
junto con ella (no es una tarea de `/opsx:apply`).

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `practice-batch`: sustituye el aviso previo al cierre y el ciclo de cierre-y-composición por el ciclo de
  "generar sesión"; sustituye la composición estrictamente por prioridad por la composición por cuotas; añade el
  requisito de "tanda completa" (pasada completa) y el de regeneración por cambio de CEFR sin resolución de
  cierre.
- `practice-session`: sustituye las acciones y el aviso de cierre por el mensaje de tanda completada y la acción
  "generar sesión"; sustituye el indicador de racha/sabida por el indicador "aciertos para el próximo nivel".
- `settings-screen`: diferencia el efecto de cambiar el nivel CEFR (regenera sin resolución de cierre) del
  efecto de cambiar solo N (sigue forzando el cierre resuelto, como hoy).

## Impact

- `:core:domain`: `ComposeNextBatch`, `CloseActiveBatch`, `ForceCloseActiveBatch`, `ObtainActiveBatch`,
  `ObtainPracticeSession`/`PracticeSessionResult` (composición por cuotas, nuevo caso de uso "generar sesión",
  nuevo caso de uso de regeneración por CEFR sin cierre, señal de "tanda completa").
- `:core:designsystem`: `DsLevelBadge` (nombre de insignia público).
- `:feature:session`: `PracticeSessionScreen`, `PracticeSessionUiState`, `PracticeSessionAction`,
  `PracticeSessionViewModel`, `strings.xml` (nuevo estado/acción de generar sesión, nuevo indicador).
- `:feature:settings`: `SettingsViewModel` (distinguir cambio de CEFR de cambio de N al guardar).
- `openspec/specs/practice-batch`, `practice-session`, `settings-screen` (deltas).
- `docs/adr/0016-generar-sesion-por-cuotas.md` (nuevo ADR).
