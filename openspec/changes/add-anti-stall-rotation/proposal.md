# Proposal

## Why

Con ADR-0025 y ADR-0026 el usuario se atasca: en simulación (N = 10, 70 % de aciertos) las sesiones 2 a 6 no
tienen nuevas y repiten el 100 %, una frase llega a 15-19 sesiones seguidas y un 30 % de las falladas tarda 3-5
sesiones en volver. El usuario ha fijado objetivos concretos de rotación (máx. `floor(N/5)` nuevas tras la
primera tanda, 25-40 % de repetidas, máx. 6 sesiones seguidas, falladas de vuelta en 1-2 sesiones) que solo se
pueden garantizar guardando cuándo se respondió y se falló cada frase.

## What Changes

- Cada frase con progreso registra, al autoevaluarse, su **última sesión respondida**, sus **sesiones seguidas**
  y su **última sesión fallada**. Revertir la última autoevaluación los restaura; componer o recomponer no los toca.
- La tanda nueva se compone por tramos (ADR-0027): falladas por volver → repetidas hasta `ceil(N/4)` → pendientes
  → repaso adelantado (estos tres sin pasar de `floor(2N/5)` repetidas) → relleno → más nuevas → frases en
  descanso (6 o más sesiones seguidas). La tanda sigue llena hasta N (ADR-0026).
- Nuevas tras la primera tanda de un nivel CEFR: `min(disponibles, floor(N/5))` mientras las frases de nivel 0 del
  nivel CEFR sean menos de `N + floor(N/5)`; si no, 0. Sustituye a techo, hueco y `N − pendientes` de ADR-0025.
- **BREAKING (datos):** `PracticeDatabase` pasa a la versión 5 con tres columnas nuevas en `phrase_progress`, sin
  `Migration`: actúa el `fallbackToDestructiveMigration()` ya configurado y se pierde el progreso guardado.
- ADR-0027 (Propuesto) sustituye en parte a ADR-0025 y ADR-0026; se acepta al archivar.
- Se actualizan `openspec/config.yaml` y `.claude/agents/android-architect.md`, que aún remitían a ADR-0012 como
  fuente única del método.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `practice-batch`: candidatas (frases en descanso), número de nuevas, orden de composición por tramos, prioridad
  y repaso adelantado dentro de los tramos, reparto de las nuevas y recomposición por cambio de nivel CEFR.
- `spaced-repetition`: registro de respuestas por frase al autoevaluar, su reversión y sus invariantes.
- `practice-persistence`: persistencia del registro de respuestas y descarte del estado con un esquema anterior.
- `practice-session`: el texto de la generación de sesión remite al orden por tramos; dos escenarios cambian solo
  su nota explicativa (el resultado esperado no cambia).

## Impact

- ADR afectados: ADR-0025 y ADR-0026 (sustituidos en parte por ADR-0027). Se respetan ADR-0021 (filtro CEFR
  exacto), ADR-0024 (fallo), ADR-0010 (módulos) y el alcance del MVP (ADR-0018). Terminología vigente: tanda,
  sesión generada, nivel, racha, certificada, pendiente, repaso adelantado.
- Código: `:core:domain` (`PhraseProgress`, tipo nuevo para el registro de respuestas, `ComposeNextBatch`),
  `:core:data` (`PhraseProgressEntity`, `PracticeMapping`, `PracticeDatabase` v5 y `schemas/.../5.json`),
  `:core:testing` (`aPhraseProgress`). Sin cambios de puertos, casos de uso públicos, UI ni librerías.
- Tests que fijan el comportamiento antiguo: `ComposeNextBatchTest`, `RegenerateActiveBatchForLevelTest`,
  `GenerateSessionTest` y, si fijan el número de nuevas, `ObtainPracticeSessionTest`, `CloseThenObtainTest` y
  tests de `:feature:session`.

## Fuera de alcance

- Cambios de UI: no se muestra qué frases están en descanso ni el registro de respuestas.
- Cambiar la espera intra-nivel, la certificación, el enfriamiento o la regla de fallo.
- Mejorar las sesiones 2 y 3 más allá de las `floor(N/5)` nuevas de la sesión 2.
- Migración que conserve el progreso existente.
