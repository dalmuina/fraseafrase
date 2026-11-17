# Proposal

## Why

ADR-0017 extendió el filtro CEFR de "a reforzar" y "dominadas" para exigir nivel CEFR **igual o
superior** al elegido en Ajustes, en vez de exigir el nivel exacto (como ya hacía el bloque de
nuevas). El objetivo era permitir un "repaso ligero" de frases ya dominadas de niveles más altos
aunque el usuario bajara el nivel CEFR elegido.

En uso real esto resulta frustrante: si el usuario empezó a practicar en un nivel alto (por
ejemplo B1) y baja a uno más bajo (A1) porque le resultaba demasiado difícil, el algoritmo le
sigue mostrando esas frases de B1 ya iniciadas, en vez de darle solo frases de A1. El usuario
esperaba dejar atrás el nivel alto, no seguir repasándolo.

El caso de subir de nivel (de A1 a A2) ya debería quedar cubierto por la regla actual, porque A1 es
inferior a A2 y el filtro ya excluye niveles inferiores; esta propuesta lo deja confirmado con
nuevos escenarios además de corregir el caso de bajar.

## What Changes

- **BREAKING** (comportamiento, no de datos): el filtro CEFR de "a reforzar" y "dominadas" pasa a
  exigir el nivel CEFR **exactamente igual** al elegido en Ajustes, igual que ya exige el bloque de
  nuevas. Deja de admitir niveles superiores al elegido.
- El progreso (nivel de consolidación, racha, `dueAtSession`) de una frase de un nivel CEFR distinto
  al elegido **se conserva sin cambios**: no se filtra como pendiente ni dominada mientras el nivel
  elegido sea otro, pero vuelve a ofrecerse tal cual si el usuario vuelve a elegir ese nivel CEFR
  más adelante. No se añade ningún campo ni migración nueva: esto ya es el comportamiento actual,
  porque el progreso no se borra al cambiar de nivel CEFR; solo cambia qué frases entran en la
  tanda compuesta.
- Nuevo ADR (ADR-0021) que sustituye, solo en este punto, la regla de ADR-0017 sobre el alcance del
  filtro CEFR en "a reforzar" y "dominadas".

## Capabilities

### Modified Capabilities

- `practice-batch`: el requisito "Composición de la tanda nueva por cuotas sobre el progreso" (el
  filtro CEFR de los bloques a reforzar y dominadas) cambia de "igual o superior al nivel elegido" a
  "igual al nivel elegido".

## Impact

- `:core:domain`: `ComposeNextBatch` — las condiciones `catalogById.getValue(it.id).level >= cefrLevel`
  de `reinforceCandidates` y `masteredCandidates` pasan a comparar por igualdad (`== cefrLevel`).
- Tests existentes de `ComposeNextBatchTest` que cubren "igual o superior" (frases de nivel CEFR
  superior entrando en a reforzar/dominadas) deben actualizarse para reflejar la exclusión.
- No afecta a `:core:data`, `:core:platform`, `:feature:settings` ni a la persistencia de ajustes
  (`user-settings`): el cambio es puramente de criterio de selección en memoria dentro de
  `ComposeNextBatch`.
- Respeta el alcance del ADR-0012 (no añade modos de práctica ni capacidades fuera del MVP).

## Fuera de alcance

- No cambia el filtro del bloque de nuevas (ya exige nivel exacto).
- No añade ninguna forma de ver o gestionar el progreso "oculto" de otros niveles CEFR desde la
  pantalla de progreso ni la de ajustes; sigue estando disponible solo al volver a elegir ese nivel.
- No cambia la regeneración de la tanda activa al cambiar de nivel CEFR (`RegenerateActiveBatchForLevel`),
  solo qué frases resultan candidatas dentro de esa recomposición.
- No cambia las cuotas, el tamaño N, ni ninguna regla de racha, certificación o enfriamiento
  (`spaced-repetition`).
