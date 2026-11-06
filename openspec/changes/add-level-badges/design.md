# Design

## Context

`PracticeBatch.entries` ya es una lista de `PhraseProgress` (`:core:domain`), que lleva `level: Level`
(0 a 5, ADR-0012/ADR-0015). `ObtainPracticeSession.items()` construye cada `PracticeItem` a partir de
esa entrada y de la frase del catálogo, pero hoy no copia `level`. En Progreso, `ObtainProgressOverview`
ya agrupa el catálogo `byLevel: Map<Level, Int>`, así que la insignia solo hace falta en la capa de
presentación, por fila. Ver `proposal.md` para el por qué.

## Goals / Non-Goals

**Goals:**
- Representación visual única (`DsLevelBadge`) para los niveles 1 a 5, reutilizada en Practicar y
  Progreso.
- `PracticeItem` expone el nivel de consolidación de la frase actual sin tocar las reglas de
  ADR-0012/ADR-0015 ni la persistencia.

**Non-Goals:**
- Diseño visual a medida de las insignias (iconos Material estándar, placeholder).
- Mostrar insignias de frases distintas a la actual en Practicar.
- Cualquier lógica nueva de cálculo de nivel: `DsLevelBadge` solo pinta el `Level` que recibe.

## Decisions

### D1: `DsLevelBadge` recibe un `Int` (0-5), no el `Level` del dominio

Recibe `level: Level` y pinta icono + etiqueta de texto, o nada en nivel 0. `Level` ya vive en
`:core:domain`, así que `:core:designsystem` pasaría a depender de él - pero la regla del proyecto
es que `:core:designsystem` "no depende de ningún módulo del proyecto" (CLAUDE.md, ADR-0010).
Para no romperla, `DsLevelBadge` recibe un `Int` (0 a 5) en vez de `Level`; cada feature traduce su
`Level`/nivel de dominio a ese `Int` antes de llamarlo. Alternativa descartada: que
`:core:designsystem` dependa de `:core:domain` solo por este enum - contradice una regla explícita
del proyecto para ahorrar una conversión de una línea.

### D2: Mapeo nivel → insignia como tabla fija dentro de `DsLevelBadge`, con `material-icons-extended`

El catálogo de versiones solo declara `material-icons-core` (`androidx-compose-material-icons-core`
en `gradle/libs.versions.toml`), que no incluye `military_tech`, `workspace_premium` ni `diamond`.
Se añade `androidx-compose-material-icons-extended` (mismo grupo y versión que
`material-icons-core`, del version catalog de Compose ya usado) como dependencia nueva de
`:core:designsystem`, justificada porque el alcance de P4 pide iconos Material reconocibles por
rango y el set core no los tiene; usar solo formas genéricas (círculo, estrella) del set core
contradice el pedido explícito de insignias bronce/plata/oro/platino/diamante. Alternativa
descartada: dibujar los iconos a mano con `Canvas`/vectores propios - es el rediseño visual a medida
que `proposal.md` deja fuera de alcance.

Icono y color por nivel, definidos en el propio componente: nivel 1 `military_tech` (bronce), 2
`military_tech` con tinte distinto (plata), 3 `workspace_premium` (oro), 4 `workspace_premium` con
tinte distinto (platino), 5 `diamond` (diamante). Los colores usan tokens de
`MaterialTheme.colorScheme` (ningún color literal), elegidos por contraste y no por intentar imitar
el metal/piedra real. Nivel 0 no pinta nada (`Box` vacío o `null`), para no reservar espacio
distinto entre tener y no tener insignia en Progreso, donde las seis filas deben alinearse.

### D3: `PracticeItem` gana `level: Level`, copiado de `PhraseProgress.level`

`ObtainPracticeSession.items()` ya tiene `entry: PhraseProgress` con `entry.level` disponible; solo
añade `level = entry.level` al construir cada `PracticeItem`. Sin nuevo acceso a datos ni cambio de
`PhraseRepository`/`ProgressRepository`.

### D4: Progreso no cambia su modelo de datos, solo su presentación

`ProgressOverviewResult.Available.byLevel` y `ProgressUiState.Content.byLevel` ya son
`Map<Level, Int>`; `ContentState` en `ProgressScreen.kt` solo añade `DsLevelBadge(level.value)` junto
al texto existente de cada fila, sin tocar `ProgressViewModel` ni el dominio.

## Risks / Trade-offs

- [`material-icons-extended` añade un paquete de iconos grande a `:core:designsystem`, usado por
  todas las features] → aceptable: solo aumenta el tamaño del APK con los iconos que R8/minify
  acaben necesitando (shrinking ya activo en release); no añade lógica ni superficie de API nueva.
- [Pasar `Int` en vez de `Level` a `DsLevelBadge` pierde el tipo de dominio en el límite de
  `:core:designsystem`] → aceptable: es la misma regla que ya aplican los demás componentes `Ds*`,
  que no reciben tipos del dominio.
