# Proposal

## Why

ADR-0015 fija cuándo y cuánto sube o baja el nivel de consolidación (0-5) de una frase, dejando
desbloqueado el punto 4 de la hoja de ruta MVP v2: mostrar ese nivel como una insignia reconocible
(bronce/plata/oro/platino/diamante) en vez de solo como un número, en Practicar y en Progreso. El
nivel ya es correcto y estable (P1-P3 hechos); falta solo la representación visual.

## What Changes

- Se define la insignia de nivel de consolidación como concepto propio: nivel 0 sin insignia
  ("sin rango"), nivel 1 Bronce, nivel 2 Plata, nivel 3 Oro, nivel 4 Platino, nivel 5 Diamante.
  Cada insignia usa un icono Material placeholder (sin diseño a medida, que queda para un cambio
  posterior) y un color de `MaterialTheme`.
- Nuevo componente `DsLevelBadge` en `:core:designsystem` que recibe el nivel de consolidación y
  pinta su icono, color y etiqueta de texto (o nada, en nivel 0).
- Práctica (`:feature:session`): la frase que se muestra incluye su insignia de nivel de
  consolidación junto al indicador de nivel CEFR ya existente. Esto requiere exponer el nivel de
  consolidación de la frase actual, que hoy `PracticeItem` no lleva.
- Progreso (`:feature:progress`): cada fila de nivel (0 a 5) de la pantalla de progreso muestra su
  insignia junto al número de frases de ese nivel.

## Capabilities

### New Capabilities

- `level-badges`: define la insignia visual de cada nivel de consolidación (0 a 5) - icono, color
  y etiqueta - como concepto reutilizable, independiente de la pantalla donde se muestre.

### Modified Capabilities

- `practice-session`: la frase mostrada en Practicar también muestra la insignia de su nivel de
  consolidación.
- `progress-overview`: cada fila de nivel de la pantalla de Progreso muestra la insignia de ese
  nivel junto al número de frases.

## Fuera de alcance

- Rediseño visual a medida de las insignias (se usan iconos Material estándar como placeholder).
- Cualquier cambio a las reglas de subida o bajada de nivel: ya fijadas en ADR-0012 y ADR-0015, sin
  tocar.
- Animaciones o notificaciones al conseguir una insignia nueva (por ejemplo, al cerrar una tanda).
- Mostrar la insignia de frases que no son la actual en Practicar (por ejemplo, en una lista de toda
  la tanda): la tanda no se ve como lista en el MVP, solo frase a frase.

## Impact

- `:core:designsystem`: nuevo componente `DsLevelBadge`, tokens de color para las cinco insignias y
  una dependencia nueva, `material-icons-extended`, justificada en `design.md` (D2).
- `:core:domain`: `PracticeItem` (en `practice/PracticeSessionResult.kt`) gana el nivel de
  consolidación de la frase actual; el caso de uso que lo construye debe leerlo de `PhraseProgress`.
- `:feature:session`: `PracticeSessionUiState`/`PracticeSessionScreen` pintan la insignia nueva.
- `:feature:progress`: `ProgressUiState`/`ProgressScreen` pintan la insignia nueva en cada fila de
  nivel.
- Sin cambios de persistencia, de ADR ni de las reglas de ADR-0012/ADR-0015.
