# Design

## Context

Ver `proposal.md` - Why. Puntos de partida relevantes del código actual (confirmados por
inspección, no por los specs):

- `:core:designsystem` solo tiene dos componentes `Ds*`: `DsLevelBadge(level: Int)` y
  `DsLevelBadgeChange`. Todo lo demás en `feature:session`, `feature:progress` y
  `feature:settings` usa Material3 sin envoltorio propio (`Card`, `Button`, `FilterChip`,
  `LinearProgressIndicator`, `NavigationBar`).
- `AppTheme` ya expone `AppTheme.spacing`, `AppTheme.motion`, `AppShapes`, `AppTypography` y los
  esquemas de color `LightColors`/`DarkColors`. Las fuentes (Baloo 2, Plus Jakarta Sans) están
  empaquetadas en el proyecto, no se cargan por red.
- El dominio persiste el progreso por frase (`PhraseProgress`: nivel, racha, `dueAtSession`) y la
  tanda activa (`PracticeBatch`) a través de `PracticeRepository`, con una única fuente Room
  (`RoomPracticeDataSource`, ADR-0010). No existe hoy ningún contador agregado de la tanda en
  curso: `AnswerPhrase` y `RevertLastAnswer` solo tocan el estado por frase.
- `ObtainProgressOverview`/`ProgressOverviewResult` todavía devuelven `BatchProgress(certified,
  total)` (pre-ADR-0018) en vez de la forma que ya exige el spec `progress-overview` vigente.

## Goals / Non-Goals

**Goals:**
- Dar a `:core:designsystem` los tokens y componentes que hacen falta para que Practicar, Progreso,
  Ajustes y Sesión completada compartan una misma identidad visual, reutilizable por más de una
  pantalla donde corresponda.
- Calcular y persistir, por tanda activa, cuántas autoevaluaciones fueron acierto, cuántas fallo y
  cuántas frases subieron de nivel de consolidación en algún momento de esa tanda, y exponerlo en el
  mensaje de tanda completada.
- Corregir `ObtainProgressOverview`/`ProgressOverviewResult` para que coincidan con el spec
  `progress-overview` vigente.

**Non-Goals:**
- No se cambia ninguna regla de composición de tanda, resolución de nivel ni repetición espaciada
  (ADR-0015/0016/0017): el contador de la tanda solo observa resultados que esas reglas ya producen.
- No se introduce ninguna fuente tipográfica ni asset remoto nuevo: las fuentes "Space Grotesk"/
  "Inter" del mock se traducen a los pesos y tamaños más cercanos de Baloo 2 y Plus Jakarta Sans, ya
  empaquetadas, para no romper el offline-first (ADR-0013) ni añadir una dependencia nueva.
- No se cambia la navegación de `:app` (`FraseAFraseApp.kt`): sigue habiendo tres destinos
  (Practicar, Progreso, Ajustes) en la misma bottom bar; el nuevo enlace "Ver mi progreso" navega al
  destino Progreso que ya existe.

## Decisions

### Tokens de `:core:designsystem`

- `theme/Color.kt`: se actualiza `primary`/`primaryContainer` al índigo del mock (sustituye
  `#3346C8` por un tono equivalente a `#5B3DF5`, en los esquemas claro y oscuro), y se revisan
  `tertiary` (éxito/"Acierto") y `error` (fallo) para que coincidan con los verdes/rojos del mock,
  conservando el contraste 4.5:1 exigido por las reglas de diseño del proyecto. Los colores de cada
  insignia de rango (bronce/plata/oro/platino/diamante) de `DsLevelBadge` no cambian: son
  representacionales (metal/gema), no tokens de tema, y el mock los mantiene.
- `theme/AppShapes.kt`: se amplía el radio de las tarjetas grandes a ~24dp para igualar el mock
  (hoy el radio por defecto es menor); el radio de botones/controles pequeños se revisa igual.
- `theme/Type.kt`: no se añade tipografía nueva. Se ajustan tamaño/peso/`letterSpacing` de
  `displayLarge`/`headlineLarge`/`titleLarge` (Baloo 2) y `titleMedium`/`bodyLarge` (Plus Jakarta
  Sans) para acercarse al ritmo visual de Space Grotesk/Inter del mock, sin cambiar de familia.

### Componentes `Ds*` nuevos

Solo se promueven a `:core:designsystem` los dos patrones que reutiliza más de una feature; el
resto queda como composables locales de su feature, según la regla del proyecto de no promocionar
antes de que haga falta:

- **`DsStatTile(value: String, label: String, size: DsStatTileSize = DsStatTileSize.Compact, modifier: Modifier = Modifier)`**:
  tarjeta de recuento con número grande y etiqueta, sin estado ni tipos del dominio (recibe
  `String` ya formateado, no `Int` ni tipos del dominio). `modifier` va primero entre los opcionales,
  como exige la convención de componentes `Ds*` del proyecto. La usan `feature:session` (los tres
  contadores de Sesión completada, variante `Compact`) y `feature:progress` (la tarjeta destacada de
  frases disponibles, variante `Large`); `DsStatTileSize` es un enum sin lógica de negocio, solo
  controla tamaño de fuente y relleno.
- **`DsProgressBar(progress: Float, modifier: Modifier = Modifier)`**: envuelve
  `LinearProgressIndicator` con la forma y el color del tema del proyecto (extremos redondeados,
  color `{{accent}}` equivalente a `primary`). La usan `feature:session` (indicador de posición en
  la tanda, cabecera de `PracticeSessionScreen`) y `feature:progress` (barra proporcional de cada
  fila de rango). No sustituye a `DsLevelBadge`; las filas de Progreso siguen usando `DsLevelBadge`
  para la insignia y añaden `DsProgressBar` para la proporción.

El resto de piezas del mock (pastilla de posición "X de Y", etiqueta de nivel CEFR en la cabecera de
la tarjeta de práctica, selector segmentado de nivel CEFR en Ajustes, fila de rango completa de
Progreso) se implementan como composables privados de su propia feature, construidos sobre
`AppTheme`/`DsLevelBadge`/`DsProgressBar`; si una segunda feature los necesita más adelante, se
promocionan entonces, no antes.

### Contador de la tanda completada (`:core:domain`)

- Nuevo tipo `BatchOutcome(hits: Int, misses: Int, levelUps: Int)` en el paquete `practice` de
  `:core:domain`, inmutable, sin anotaciones de framework.
- Se añade como parte del estado persistido de la tanda activa, detrás del puerto
  `PracticeRepository` que ya existe (ADR-0010): sigue habiendo una única fuente
  (`RoomPracticeDataSource`), así que no se introduce ningún `Repository` de estrategia nuevo, solo
  se amplían sus columnas. El único precedente real de cambio de esquema en `PracticeDatabase`
  (v1→v2, cambio `repeat-by-session-counter`) usa `fallbackToDestructiveMigration()` a propósito,
  documentado con "proyecto pre-1.0.0, sin usuarios en producción: el progreso existente se pierde,
  igual que una instalación nueva". Esta tarea sigue ese mismo precedente: se sube la versión de
  `PracticeDatabase`, se añaden las columnas de `BatchOutcome` y se mantiene
  `fallbackToDestructiveMigration()`, sin escribir una `Migration` real ni añadir `room3-testing`/
  `MigrationTestHelper` (no está en el version catalog y no hay nada que justifique esa dependencia
  nueva para datos que, por ese mismo precedente, no se preservan entre versiones de esquema).
- `AnswerPhrase` incrementa `hits` o `misses` según la autoevaluación, y además incrementa
  `levelUps` cuando el nivel de consolidación resultante de esa respuesta es estrictamente mayor que
  el nivel de la frase inmediatamente antes de esa misma respuesta (igual que ya hace para decidir la
  transición de insignia). `levelUps` no vuelve a bajar si una respuesta posterior de la misma tanda
  hace bajar el nivel de esa frase: cuenta subidas ocurridas, no el nivel final.
- `RevertLastAnswer` deshace exactamente el último incremento aplicado (resta 1 a `hits` o `misses`,
  y resta 1 a `levelUps` si esa respuesta había contado como subida), simétrico a cómo ya deshace la
  racha, el nivel y el `dueAtSession` de la frase afectada.
- `ComposeNextBatch`/`GenerateSession` reinicia `BatchOutcome` a cero al componer la tanda siguiente.
- `PracticeSessionResult.Completed` (o el tipo equivalente que ya representa el mensaje de tanda
  completada) pasa a incluir el `BatchOutcome` de la tanda que se completó, para que
  `PracticeSessionViewModel` lo traduzca a `PracticeSessionUiState`.

### Corrección de `progress-overview`

- `ProgressOverviewResult.Available` sustituye `batchProgress: BatchProgress?` por los dos datos que
  exige el spec vigente: el total disponible del nivel CEFR vigente y ese mismo nivel CEFR (el
  recuento por nivel `byLevel` ya existe y no cambia de forma). `ObtainProgressOverview` deja de leer
  la tanda activa para este dato: el total es la suma de `byLevel`, ya acotado al nivel CEFR vigente
  por el repositorio de catálogo + progreso existente.
- Es una corrección de implementación sobre un spec ya aprobado (ADR-0018), no un requisito nuevo:
  no necesita delta de spec (`proposal.md` ya lo explica).

## Risks / Trade-offs

- [Subir la versión de esquema de `PracticeDatabase` con `fallbackToDestructiveMigration()` pierde
  cualquier tanda activa guardada al actualizar la app] → Aceptado a propósito, siguiendo el único
  precedente real del proyecto para esta misma base de datos (pre-1.0.0, sin usuarios en producción);
  no se introduce una `Migration` real ni la dependencia `room3-testing` para preservar datos que el
  proyecto ya decidió no preservar entre versiones de esquema.
- [Re-tema global de color/tipografía puede afectar al contraste accesible en estados ya probados
  (p. ej. `FilterChip` seleccionado, textos sobre `primaryContainer`)] → Revisar contraste 4.5:1 de
  cada combinación de color tocada como parte de la tarea de tokens, antes de reestilizar las
  pantallas que los consumen.
- [El selector segmentado de nivel CEFR en Ajustes sustituye a `FilterChip`, que ya tiene semántica
  accesible de selección única por defecto] → Implementarlo con `Modifier.selectableGroup()` y rol de
  radio por botón, para no perder esa semántica.

## Estrategia de test

- `BatchOutcome` y su acumulación/reversión en `AnswerPhrase`/`RevertLastAnswer`: tests unitarios en
  `:core:domain` (JUnit 6 + Kotest), con los fakes de `:core:testing` ampliados para
  `PracticeRepository`.
- `ObtainProgressOverview` corregido: tests unitarios existentes actualizados a la nueva forma de
  `ProgressOverviewResult`.
- `PracticeSessionViewModel`: tests con `MainDispatcherExtension` y Turbine verificando que el
  `UiState` expone el desglose y que revertir lo recalcula.
- `DsStatTile`/`DsProgressBar`: Compose UI tests de renderizado (valores/etiquetas visibles) y
  `@PreviewLightDark` en ambos temas, como el resto de componentes `Ds*`.
- Pantallas (`PracticeSessionScreen`, `ProgressScreen`, `SettingsScreen`, pantalla de sesión
  completada): Compose UI tests existentes ampliados para los nuevos elementos visibles (desglose,
  barra por fila, selector segmentado), sin test nuevo de las reglas de negocio que no cambian.
