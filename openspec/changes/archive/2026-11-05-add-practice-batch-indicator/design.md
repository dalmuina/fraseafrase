# Design

## Context

`PracticeSessionResult.Ready` ya devuelve `items: List<PracticeItem>` en el orden circular de la
tanda (ADR-0012: cada frase respondida se mueve al final, nada desaparece), pero
`PracticeSessionViewModel.toScreenState()` solo conserva `items.first()`
(`PracticeSessionViewModel.kt:137-142`): descarta el tamaño de la tanda y cualquier noción de
posición. `PracticeItem` (`core/domain/.../practice/PracticeSessionResult.kt:35-48`) no lleva el
`CefrLevel` de la frase, aunque `CatalogPhrase.level` ya lo tiene. El botón de escuchar
(`PracticeSessionScreen.kt:124-129`) es un `TextButton` con solo texto.

Como la tanda es una lista circular donde cada respuesta mueve la frase actual al final, mostrar
la frase siempre es ver `items.first()`: no hay un índice estable por id que avance de forma
natural. Ver `proposal.md` para la motivación completa.

## Goals / Non-Goals

**Goals:**
- Mostrar "X de Y" donde Y es el tamaño de la tanda activa y X es cuántas frases se han visto en la
  vuelta actual a la tanda (1 al abrir o al empezar una tanda nueva, avanza en cada autoevaluación,
  vuelve a 1 al completar una vuelta).
- Mostrar el nivel CEFR de la frase actual.
- Añadir un icono al control de escuchar.

**Non-Goals:**
- No se persiste el contador de posición ni se añade ninguna tabla/columna a Room: es un dato de
  pantalla, no de dominio (igual que `revealed`/`busy`/`closeWarning` hoy).
- No se cambia el modelo de tanda circular ni su composición (eso es la hoja de ruta P2-P4 del plan
  de ajustes/progreso, fuera de este cambio).
- No se introduce ningún componente `Ds*` nuevo en `:core:designsystem` (ver Decisión 3).

## Decisions

### D1: El contador "X de Y" vive en `PracticeSessionUiState.Practicing`, no en el dominio

`Practicing` gana `index: Int` (1-based, posición dentro de la vuelta actual) y `total: Int`
(tamaño de la tanda, `items.size`). El `ViewModel` los calcula así:
- Carga inicial (`init`) y cualquier recarga que no sea consecuencia de una autoevaluación (cierre
  confirmado, cierre obsoleto/sin tanda activa) usan la ruta ya existente de `toScreenState()`:
  `index = 1`, `total = items.size`.
- Tras una autoevaluación (`answer()`), la recarga usa una ruta nueva que toma el `index`/`total`
  del estado anterior y calcula `index = if (index == total) 1 else index + 1`; `total` se
  recalcula igual por si la tanda cambiara de tamaño (no debería, pero evita que quede obsoleto).

**Alternativa descartada**: guardar un contador de "vuelta" persistido junto a la tanda en Room.
Se descarta porque añadiría una columna/estado nuevo al agregado de persistencia de ADR-0013 solo
para un dato cosmético; perderlo al matar el proceso es un degradado aceptable, igual que ya pasa
hoy con `revealed`/`busy` (design D3 de `practice-session`).

**Riesgo**: si en el futuro una recarga cambia el tamaño de la tanda sin pasar por el cierre (hoy
no ocurre; `answer()` solo registra una respuesta), el índice podría quedar fuera de rango. Se
mitiga con `index.coerceAtMost(total)` al construir el estado tras una autoevaluación.

### D2: `CefrLevel` se añade a `PracticeItem`, no a `PracticeSessionUiState`

`PracticeItem` gana `cefrLevel: CefrLevel`, poblado en `ObtainPracticeSession.items()` desde
`CatalogPhrase.level` (ya presente en la frase unida por id). La pantalla lee
`state.current.cefrLevel` directamente, igual que ya hace con `state.current.streak`. No se
duplica el dato en `PracticeSessionUiState`.

### D3: Icono del control de escuchar: `Icons.Default.PlayArrow`, sin dependencia nueva

`material3` trae `material-icons-core` de forma transitiva en versiones recientes de Compose, que
incluye `PlayArrow`; hoy no hay ningún artefacto `material-icons-*` declarado en
`gradle/libs.versions.toml` ni ningún `Icons.*` en uso, así que esto debe comprobarse con un build
real antes de completar la tarea 3.6, no asumirse. Si `Icons.Default.PlayArrow` no resuelve sin
cambios, añadir `androidx.compose.material:material-icons-core` (no `-extended`, que son varios
cientos de iconos adicionales innecesarios) al catálogo de versiones, con esta misma
justificación. En cualquier caso se evita `material-icons-extended` solo por un icono de
altavoz/boca hablando más literal.

**Resultado de la comprobación real**: `Icons.Default.PlayArrow` NO resuelve solo con `material3`
(`Unresolved reference 'icons'` al compilar `:feature:session`); se añadió
`androidx.compose.material:material-icons-core` (sin versión propia, gestionado por el
`compose-bom` ya declarado) a `gradle/libs.versions.toml` y como `implementation` junto a
`material3` en `AndroidCompose.kt` (build-logic), para que todas las features que ya dependen de
Compose la tengan disponible sin declarar nada por su cuenta.

El `TextButton` se sustituye por un `Row` con `Icon(Icons.Default.PlayArrow, contentDescription =
null)` + `Text(...)` dentro del mismo `TextButton`, manteniendo su `onClick`/`enabled` actuales.
El icono es decorativo (la etiqueta de texto ya describe la acción), así que
`contentDescription = null` evita duplicar el anuncio en lectores de pantalla.

### D4: Sin componente `Ds*` nuevo

El indicador de posición y el chip de nivel CEFR se implementan con `Text`/`Row` de Material 3
directamente en `PracticeSessionScreen.kt`, usando `AppTheme.spacing`. Solo `:feature:session` los
usa hoy; si `:feature:progress` u otra feature necesitara el mismo chip de nivel, se promocionaría
entonces a `:core:designsystem` como `DsLevelBadge`, según la convención del proyecto (un
componente se promociona cuando lo necesita una segunda feature).

## Risks / Trade-offs

- [El índice de pantalla no sobrevive a la muerte del proceso] → Aceptable: vuelve a 1 al recargar,
  sin pérdida de datos de dominio (racha, nivel, catálogo no se ven afectados).
- [Confusión si el usuario entiende "X de Y" como progreso lineal hacia "terminar" la tanda, cuando
  en realidad es circular y nunca termina sola] → Fuera de alcance de este cambio (el propio
  usuario ya señaló que quiere revisar el concepto de cierre de tanda en un cambio futuro); no se
  toca el texto ni el comportamiento del cierre aquí.
