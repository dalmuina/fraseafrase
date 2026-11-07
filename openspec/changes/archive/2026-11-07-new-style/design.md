# Design

## Context

`:core:designsystem` tiene hoy una paleta y tipografía Material 3 por defecto, explícitamente
marcadas como provisionales en el código (`Color.kt`, `Type.kt`). No hay `Shapes` propio: `AppTheme`
no se lo pasa a `MaterialTheme`, así que usa las esquinas por defecto de Material 3 (poco
redondeadas frente a los mocks). `DsLevelBadge` usa iconos de `material-icons-extended`
(`MilitaryTech`, `WorkspacePremium`, `Diamond`) teñidos con colores del `ColorScheme`. Ver
`proposal.md - Why` para la motivación completa.

El usuario ha entregado 6 vectores ya coloreados (`ic_rango_sin_rango`, `ic_rango_bronce`,
`ic_rango_plata`, `ic_rango_oro`, `ic_rango_platino`, `ic_rango_diamante`) en
`android_drawable/*.xml`, listos para copiar a `res/drawable/`.

## Goals / Non-Goals

**Goals:**
- Paleta y tipografía definitivas, en claro y oscuro, coherentes con los 4 mocks.
- Esquinas muy redondeadas (`Shapes`) coherentes con los mocks, token nuevo en `AppTheme`.
- `DsLevelBadge` usando los 6 vectores entregados, con el nivel 0 ahora visible.
- Barra de navegación inferior con icono por destino.
- Pantalla de Progreso con la tarjeta de total disponible + nivel CEFR en vez de certificadas.

**Non-Goals:**
- Elegir una librería de iconos nueva: los iconos de navegación salen de
  `material-icons-extended`, ya presente en el version catalog.
- Tocar `:core:data`, `:core:platform` o el dominio: este cambio es de presentación pura.
- Definir una paleta de "colores semánticos" genérica más allá de lo que usan estos 4 mocks.

## Decisions

### D1. Paleta de color fija en `ColorScheme` de Material 3, sin tokens propios nuevos

Se rellenan `LightColors`/`DarkColors` en `Color.kt` con `lightColorScheme(...)` /
`darkColorScheme(...)` explícitos, en vez de introducir un sistema de tokens de color paralelo.
Alternativa descartada: un `object AppColors` propio (como `Spacing`/`Motion`) — se descarta porque
Material 3 ya tiene roles (`primary`, `error`, `tertiary`...) que cubren todo lo que piden los
mocks, y duplicarlos complicaría el tema sin necesidad.

Mapeo de roles (valores aproximados, muestreados de los PNG; el detalle fino de contraste se ajusta
en implementación):

| Rol Material 3 | Valor claro | Uso en los mocks |
|---|---|---|
| `primary` | `#3346C8` | CTA principal, chip CEFR seleccionado, barra de progreso, tarjeta "Frases disponibles" |
| `onPrimary` | `#FFFFFF` | Texto/icono sobre `primary` |
| `primaryContainer` | `#E6E9FB` | Tarjeta de traducción revelada |
| `onPrimaryContainer` | `#1B2559` | Texto sobre `primaryContainer` |
| `tertiary` | `#1B6B44` | Botón "Acierto" |
| `onTertiary` | `#FFFFFF` | Texto sobre `tertiary` |
| `error` | Rojo Material 3 por defecto (ya definido) | Borde y texto del botón "Fallo" |
| `background` | `#F5F6FA` | Fondo de pantalla |
| `surface` | `#FFFFFF` | Tarjetas |
| `onSurface` | `#14171F` | Texto principal |

La paleta oscura se deriva de los mismos tonos (mismo matiz, luminosidad invertida/ajustada para
contraste AA), sin mock de referencia porque ninguno de los 4 lo muestra (decisión del usuario:
derivarla igualmente).

Las insignias de rango (`DsLevelBadge`) NO usan estos roles: sus colores vienen ya fijados en los
vectores entregados por el usuario (ver D3), para no perder la identidad visual de cada rango.

### D2. `Shapes` nuevo en `theme/Shape.kt`, aplicado vía `AppTheme`

Se añade un fichero `theme/Shape.kt` con un `Shapes(...)` de Material 3 con radios mayores que el
valor por defecto (tarjetas ~24dp, chips/botones con esquina muy redondeada o totalmente circular
en los pill-shaped como el indicador "Frase N de M"). `AppTheme.kt` pasa `shapes = AppShapes` a
`MaterialTheme`. Las features siguen sin declarar `RoundedCornerShape` literal: usan
`MaterialTheme.shapes.*` (regla de diseño del proyecto).

### D3. `DsLevelBadge` pinta un recurso vectorial por nivel, sin tinte

Se copian los 6 XML entregados a `core/designsystem/src/main/res/drawable/` (primer uso de `res/`
en este módulo). `DsLevelBadge` pasa de `Icon(imageVector = ..., tint = spec.tint)` a
`Icon(painter = painterResource(spec.drawableRes), tint = Color.Unspecified)`, para respetar el
color ya definido en cada vector. `levelBadgeLabel` gana la entrada `0 -> "Sin rango"`; el nivel 0
deja de devolver `null` en `levelBadgeSpec`. `material-icons-extended` dejará de usarse solo para
estos 3 iconos (`MilitaryTech`/`WorkspacePremium`/`Diamond`); se mantiene la dependencia por si
otra parte del design system la necesita, no se retira en este cambio (no hay otro uso detectado,
pero retirar la dependencia no es necesario para cumplir la spec y se deja fuera para no ampliar
el diff).

### D4. Tipografía: fuentes estáticas empaquetadas, no "downloadable fonts"

Los mocks usan una tipografía redondeada de trazo grueso en títulos, distinta de Roboto. Se
proponen dos Google Fonts con licencia OFL, de look similar:
- **Baloo 2** (pesos 600/700) para `displayLarge`/`headlineLarge`/`titleLarge` (títulos de pantalla
  tipo "Practicar", "Progreso", "Ajustes", y el número grande "120").
- **Plus Jakarta Sans** (pesos 400/500/600) para el resto de la escala (`bodyLarge`, `labelLarge`,
  etc.), por su buen contraste con Baloo 2 y su legibilidad en textos largos (la frase a traducir).

Se empaquetan como ficheros `.ttf` estáticos en `core/designsystem/src/main/res/font/`, declarados
con `FontFamily(Font(R.font.xxx, weight = ...))`, en vez de usar la API de *downloadable fonts* de
Jetpack Compose (que resuelve la fuente contra Google Play Services en tiempo de ejecución).
Alternativa descartada: *downloadable fonts* — requiere red en el primer uso y depende de Google
Play Services, lo que contradice ADR-0004 (offline-first, sin red en tiempo de ejecución).

Los binarios `.ttf` no están en este repositorio ni se pueden generar por texto: la tarea
correspondiente de `tasks.md` incluye descargarlos de Google Fonts (licencia OFL) y añadirlos a
`res/font/` como parte de la implementación.

### D5. Iconos de la barra de navegación: Material Icons existentes, sin nuevos recursos

Los tres destinos usan iconos ya disponibles en `material-icons-extended`, sin necesidad de vectores
nuevos: `Icons.Default.AutoStories` (práctica), `Icons.Default.BarChart` (progreso),
`Icons.Default.Tune` (ajustes). Viven en `:app` (`FraseAFraseApp.kt`), que ya es el único módulo que
compone las tres features (ADR-0010). `material-icons-extended` está declarada como
`implementation` en `core/designsystem/build.gradle.kts`, así que no llega transitivamente a
`:app`: hay que añadirla también como `implementation(libs.androidx.compose.material.icons.extended)`
en `app/build.gradle.kts`.

### D6. `ProgressUiState` sustituye `certifiedInBatch` por `totalAvailable` + `cefrLevel` (ADR-0018)

`ProgressUiState.Content` cambia sus campos: se quita `certifiedInBatch: CertifiedCount?` (y el tipo
`CertifiedCount`, que deja de tener otro uso) y se añaden `totalAvailable: Int` (suma de `byLevel`)
y `cefrLevel: CefrLevel`. El ViewModel de progreso pasa a inyectar también `ObtainUserSettings`
(`:core:domain`, ya existe) para leer el nivel CEFR vigente, además del caso de uso que ya usa para
`byLevel`. No se crea ningún caso de uso ni puerto nuevo: ambos datos salen de código ya existente
en `:core:domain`.

## Risks / Trade-offs

- [Los colores muestreados de PNG pueden no ser exactamente los valores de diseño originales] →
  Se usan como punto de partida; el `android-architect`/el usuario pueden ajustar valores exactos
  en la revisión sin que cambie la spec (es detalle de implementación).
- [Fuentes no incluidas en el repo bloquean la tarea hasta que alguien las descargue] → Se deja
  como tarea explícita y aislada en `tasks.md`, al principio del grupo de design system, para que
  el resto de tareas no dependa de tener ya el binario si se hace en paralelo.
- [Quitar `certifiedInBatch` es un cambio de comportamiento observable, no solo visual, y
  contradice el punto 6 de "Alcance del MVP" vigente] → Resuelto con **ADR-0018**, que sustituye ese
  punto del alcance (ver `docs/adr/0018-progreso-basico-sin-certificadas-de-la-tanda-activa.md`) y
  queda reflejado como REMOVED/MODIFIED en los deltas de `progress-overview`.

## Migration Plan

No aplica dato persistido: `certifiedInBatch` se calculaba en memoria a partir de la tanda activa,
no se guardaba tal cual en Room. No hay migración de base de datos en este cambio.
