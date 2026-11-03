# Design

## Context

Ver `proposal.md` - Why. Hoy `:app` navega entre práctica y ajustes con un *back stack* de
Navigation 3 en memoria (`FraseAFraseApp.kt`, `Routes.kt`): `Practice` es la raíz, y `Settings` se
empuja desde una acción de la práctica y se descarta al volver. El progreso se calcula a partir de
dos puertos de dominio que ya existen y no cambian: `CatalogRepository.phrases()` (catálogo
completo) y `PracticeRepository.read()` (progreso persistido y tanda activa, `PracticeSnapshot`).

## Goals / Non-Goals

**Goals:**
- Calcular certificadas de la tanda activa y total por nivel con los puertos existentes, sin nuevo
  almacenamiento.
- Sustituir el *back stack* práctica↔ajustes por una barra de navegación inferior de tres destinos.

**Non-Goals:**
- Estadísticas históricas, gráficas o tendencia en el tiempo (fuera del ADR-0012).
- Guardar qué pestaña estaba seleccionada entre reinicios: cada apertura empieza en práctica, igual
  que hoy.
- Tocar el esquema de persistencia (ADR-0013) o los puertos `CatalogRepository`/`PracticeRepository`.

## Decisions

### Caso de uso de progreso en `:core:domain`

`ObtainProgressOverview` (paquete `com.dalmuina.fraseafrase.core.domain.progress`), con
`@Inject constructor` e `operator fun invoke`, combina `CatalogRepository.phrases()` y
`PracticeRepository.read()`. Devuelve un tipo sellado `ProgressOverviewResult`:

- `Available(batchProgress: BatchProgress?, byLevel: Map<Level, Int>)`, donde `batchProgress` es
  `null` sin tanda activa y, si existe, tiene `certified: Int` y `total: Int`.
- `CatalogUnavailable(error: CatalogLoadError)` cuando `CatalogRepository.phrases()` devuelve
  `Failed` (reutiliza `CatalogLoadError` de `phrase-catalog`, sin un tipo de error nuevo).

`byLevel` se construye indexando `snapshot.progress` por `PhraseId` y, para cada frase del catálogo,
tomando su nivel si tiene progreso o `Level(0)` si no; por eso `byLevel` suma siempre el tamaño del
catálogo (spec `progress-overview`, "Total de frases por nivel"). Es una función pura sobre los
datos ya leídos: no hace E/S ni usa el reloj.

**Alternativa descartada**: un puerto `ProgressRepository` nuevo. Innecesario: ninguna de las dos
fuentes de datos cambia ni se añade una tercera; `ObtainProgressOverview` solo combina lecturas que
ya existen, igual que `ObtainPracticeSession` combina catálogo y progreso para la pantalla de
práctica.

### Nuevo módulo `:feature:progress`

Mismo patrón que `:feature:settings` (ADR-0006, ADR-0010): `ProgressRoute` + `ProgressScreen` sin
estado, `ProgressViewModel` con `StateFlow<ProgressUiState>` y `ProgressAction` sellado, inyectando
solo `ObtainProgressOverview`. Se añade a `settings.gradle.kts` con el plugin
`fraseafrase.android.feature`, que ya añade las dependencias a `:core:domain` y
`:core:designsystem` (ningún `build.gradle.kts` de feature declara dependencias de proyecto).

`ProgressUiState` modela los estados de la spec `progress-overview` con tres variantes selladas:
`Loading`, `Content(certifiedInBatch: CertifiedCount?, byLevel: Map<Level, Int>)` y `CatalogError`.
`Content` cubre tanto "con tanda activa" (`certifiedInBatch` no nulo) como "sin tanda activa"
(`certifiedInBatch` nulo) con el mismo nullable que ya usa `ProgressOverviewResult.Available.
batchProgress`: no hay una variante `NoActiveBatch` aparte, porque sería una segunda forma de
representar el mismo caso que `Content(certifiedInBatch = null, ...)` y el `ViewModel` no tendría
una regla única para elegir entre ambas.

### Barra de navegación inferior en `:app`

`FraseAFraseApp.kt` sustituye el `NavDisplay` de *back stack* por un `Scaffold` con
`NavigationBar` (Material 3) y tres `NavigationBarItem`, uno por destino (`Routes.kt` pasa de
`Practice`/`Settings` a un enum o `sealed interface BottomDestination` con `Practice`, `Progress`,
`Settings`). El contenido de cada pestaña es la pantalla de esa feature, sin empujar ni retroceder
en una pila: cambiar de pestaña selecciona qué `Route` se compone, igual que hoy selecciona qué
entrada de `NavDisplay` mostrar. `:feature:session` y `:feature:settings` pierden el parámetro
`onOpenSettings`/`onBack` de sus `Route`, porque esa navegación ya no la decide la feature (ADR-0010:
solo `:app` conoce todas las implementaciones).

**Alternativa descartada**: mantener el *back stack* de Navigation 3 y añadir `Progress` como una
tercera entrada empujable. Rechazada porque la pregunta de UX ya fijó una barra de navegación
inferior con los tres destinos, no una pila; usar Navigation 3 solo para resolver qué pestaña
compone evita una dependencia de *back stack* que no se necesita para tres destinos de igual nivel.

### Reutilización de `CatalogLoadError`

`progress-overview` reutiliza el tipo sellado `CatalogLoadError` de `phrase-catalog` en lugar de
definir uno propio, porque el motivo de error es el mismo catálogo y duplicarlo rompería que un
error esperable se modele una sola vez (regla del proyecto, "Modelar errores esperables como tipos
sellados").

## Risks / Trade-offs

- [`ObtainProgressOverview` recorre todo el catálogo en cada apertura de la pantalla] → Catálogo de
  cientos de frases, no miles (ADR-0012: niveles A1–C1 de un idioma); un `Map` por `PhraseId`
  construido una vez por llamada es suficiente, sin necesidad de índice persistido.
- [Quitar `onOpenSettings`/`onBack` de las `Route` de `:feature:session` y `:feature:settings` es un
  cambio de firma pública de esos módulos] → Solo los usa `:app`, que se actualiza en el mismo
  cambio; no hay otro consumidor.

## Tests

- `ObtainProgressOverview`: unitario en `:core:domain`, con fakes de `:core:testing` para
  `CatalogRepository` y `PracticeRepository`; cubre los cuatro escenarios de `progress-overview`
  (con tanda, sin tanda, catálogo vacío de progreso, catálogo no disponible).
- `ProgressViewModel`: unitario con `MainDispatcherExtension`, Turbine sobre el `StateFlow`, caso de
  uso falso.
- `ProgressScreen`: Compose UI Test por estado (`Content` con `certifiedInBatch`, `Content` sin
  `certifiedInBatch`, `CatalogError`), siguiendo el patrón de `SettingsScreenTest`.
- `FraseAFraseApp` (barra de navegación): instrumentado, verifica que seleccionar cada
  `NavigationBarItem` muestra la pantalla correspondiente y la marca seleccionada (spec
  `app-navigation`); sustituye el uso que haga falta de `FraseAFraseAppTest.kt`.
