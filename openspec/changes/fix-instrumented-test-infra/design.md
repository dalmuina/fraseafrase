# Design

## Context

Ver `proposal.md` - Why (no es un fallo de build con el AGP actual, sino trabajo de más).
`KotlinAndroid.kt` (`build-logic/convention`) fija
`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` y la dependencia
`androidx-test-runner` para todos los módulos a los que se aplica `fraseafrase.android.library`
o `fraseafrase.android.application`, sin mirar si el módulo tiene `src/androidTest`. Hoy solo
`:app` y `:feature:session` tienen tests instrumentados; `:core:data`, `:core:platform` y
`:core:designsystem` no tienen ninguno. `Quality.kt` aplica `detekt` con `buildUponDefaultConfig`
pero no toca el `source` de la tarea, que por defecto es solo `src/main`. `.github/workflows/ci.yml`
no tiene ningún job con emulador: solo `detekt`, `check`, `koverVerify` y `assembleDebug` en
`ubuntu-latest`.

## Goals / Non-Goals

**Goals:**
- Que `connectedDebugAndroidTest` del proyecto completo no construya ni instale un APK de test
  vacío en módulos sin tests instrumentados (hoy no falla con el AGP de este proyecto, pero sí
  hace trabajo de más: ver D1).
- Que la CI ejecute esos tests en un emulador, sin bloquear el merge todavía.
- Que `detekt` analice `src/androidTest` con la misma puerta que ya usa `src/main`.

**Non-Goals:**
- No se añade el test de arranque de `FraseAFraseApp` ni ningún test instrumentado nuevo.
- No se activa protección de ramas en GitHub ni se hace el job bloqueante.
- No se cambia la API/imagen de lint o de `assembleDebug`, ni el `compileSdk`/`targetSdk` del
  proyecto.

## Decisions

### D1. Runner e instrumentación condicionales a la existencia de `src/androidTest`

En `configureKotlinAndroid` (`KotlinAndroid.kt`), comprobar
`layout.projectDirectory.dir("src/androidTest").asFile.exists()` antes de fijar
`testInstrumentationRunner` y de llamar a `addInstrumentationRunnerDependency()`. Si el
directorio no existe, el módulo no declara runner ni dependencia de `androidx-test:runner`, y
Gradle no genera una tarea `connectedDebugAndroidTest` que compile, empaquete e instale un APK
de test vacío.

Verificado que el estado actual (runner forzado en todos los módulos) no hace fallar el build:
`./gradlew connectedDebugAndroidTest` en el proyecto completo termina en
`Starting 0 tests... Finished 0 tests... BUILD SUCCESSFUL` para `:core:data` y
`:core:platform`. D1 no corrige un fallo, sino trabajo innecesario (build, empaquetado e
instalación de un APK vacío) y una dependencia sin uso.

**Alternativas consideradas:**
- Mantener el runner global y desactivar la tarea `connectedDebugAndroidTest` con un `onlyIf`
  cuando no hay `androidTest`: dos mecanismos (runner forzado + tarea apagada) para un problema
  que D1 resuelve con uno. Descartada.
- Añadir un test instrumentado trivial a `:core:data` y `:core:platform`: test sin valor que
  oculta que esos módulos no necesitan instrumentación. Descartada.

### D2. CI ejecuta `connectedDebugAndroidTest` del proyecto completo, no una lista de módulos

Un job nuevo en `.github/workflows/ci.yml` ejecuta `./gradlew connectedDebugAndroidTest` en un
emulador, sobre todo el proyecto (igual que `check` o `detekt`, sin acotar manualmente a `:app`
y `:feature:session`). Con D1 aplicado, los módulos sin `src/androidTest` no ejecutan nada: no
hace falta mantener una lista de módulos a mano cuando se añada un test instrumentado nuevo en
otro módulo.

**Alternativa considerada:** job con lista explícita de módulos (`:app:connectedDebugAndroidTest
:feature:session:connectedDebugAndroidTest`). Funciona sin D1, pero queda obsoleta en silencio
si se añade `src/androidTest` a otro módulo y nadie actualiza la lista. Descartada.

### D3. Emulador: `reactivecircus/android-emulator-runner`, API 34, job informativo

El job usa la acción `reactivecircus/android-emulator-runner` (ya nombrada como candidata en
`docs/deuda-tecnica.md`) con una imagen API 34 sin Google APIs, en `ubuntu-latest` (acepta
KVM). API 34 en vez del `compileSdk`/`targetSdk` actual (37) porque es una imagen más asentada
y rápida de arrancar en este runner de GitHub Actions; el `minSdk` real de la app es 26, así que
no se pierde cobertura real de compatibilidad por no usar la versión más nueva.

El job se ejecuta en paralelo a `detekt`/`check`, pero **no bloquea el merge**: no hay
protección de ramas configurada en GitHub todavía (deuda aparte, "Proteger develop y main"), y
un emulador en CI es más inestable que los tests JVM; marcarlo como requerido ahora generaría
fricción sin la protección de ramas detrás para que tenga sentido. Se revisa junto con esa
deuda cuando se configure la protección.

**Alternativa considerada:** hacerlo bloqueante desde el principio, igual que `check`. Más
rigor, pero sin protección de ramas un job rojo no impide nada y añade ruido. Descartada por
ahora.

### D4. `detekt` analiza `src/androidTest` ampliando el `source` de la tarea existente

En `Quality.kt`, tras aplicar el plugin, añadir a `tasks.withType<Detekt>().configureEach` (solo
a la tarea llamada `detekt`, no a `detektBaseline`/`detektGenerateConfig`) el directorio
`src/androidTest/kotlin` del proyecto cuando existe, con `source = files(source, androidTestDir)`.
Un único `detekt` por módulo sigue cubriendo `src/main` y `src/androidTest`; el paso de CI
(`./gradlew detekt -PwarningsAsErrors=true`) no cambia.

Verificado con una prueba de Gradle init script (sin tocar el repo): con esa ampliación,
`:app:detekt` encuentra la infracción real de `MaxLineLength` en
`PersistenceWiringTest.kt:58` que hoy pasa inadvertida.

**Alternativa considerada:** tarea `detektAndroidTest` separada, con su propio paso de CI. Más
control fino, pero un paso más que mantener y una puerta de calidad duplicada. Descartada.

## Risks / Trade-offs

- [El emulador en CI es más lento e inestable que los tests JVM] → mitigado al no bloquear el
  merge (D3); se revisa cuando se mida su estabilidad real en varias ejecuciones.
- [API 34 en CI diverge del `compileSdk`/`targetSdk` 37 del proyecto] → aceptable porque el
  `minSdk` real es 26 y la instrumentación no depende de comportamiento específico de API 37;
  si en el futuro aparece una diferencia real, se puede alinear sin tocar D1/D2/D4.
- [Un módulo nuevo con `src/androidTest` pero sin tests reales (cero clases `@Test`) volvería a
  fallar con "no tests found"] → D1 solo mira si el directorio existe, no si contiene tests;
  queda igual que hoy para ese caso límite, pero deja de ser un problema de los módulos actuales.

## Migration Plan

1. `KotlinAndroid.kt`: condicionar runner y dependencia (D1).
2. `Quality.kt`: ampliar `source` de `detekt` (D4).
3. Corregir el `MaxLineLength` ya encontrado en `PersistenceWiringTest.kt` (hallazgo "Corregir"
   de la exploración), para que `detekt` siga en verde al activarse D4.
4. `.github/workflows/ci.yml`: nuevo job de emulador (D2, D3).
5. `docs/deuda-tecnica.md`: cerrar las entradas correspondientes.

Sin pasos de rollback de datos: es infraestructura de build y CI, sin estado persistente que
migrar.
