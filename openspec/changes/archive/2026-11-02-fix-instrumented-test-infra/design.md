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

### D1. Desactivar el componente `AndroidTest` cuando no existe `src/androidTest`

Primer intento descartado: condicionar `testInstrumentationRunner` y
`addInstrumentationRunnerDependency()` a la existencia de `src/androidTest`. Falló al
verificarlo (tarea 1.3): AGP declara `android:name="androidx.test.runner.AndroidJUnitRunner"`
en el `<instrumentation>` del manifest de test **aunque `testInstrumentationRunner` no se
asigne** (es el valor por defecto interno de AGP, no un eco de lo que ponga el build script). Al
quitar solo la dependencia, `:core:data` y `:core:platform` pasaron de "0 tests, build verde" a
fallar con `ClassNotFoundException: androidx.test.runner.AndroidJUnitRunner` al arrancar la
instrumentación. Condicionar el runner/la dependencia no evita que el manifest pida esa clase.

Mecanismo final: en `configureKotlinAndroid` (`KotlinAndroid.kt`), con la Variant API de AGP
(`AndroidComponentsExtension.beforeVariants`), desactivar el componente `AndroidTest` del
`VariantBuilder` cuando `layout.projectDirectory.dir("src/androidTest").asFile.exists()` es
`false`:

```kotlin
extensions.getByType(AndroidComponentsExtension::class.java).beforeVariants { variantBuilder ->
    (variantBuilder as? HasDeviceTestsBuilder)
        ?.deviceTests
        ?.get(DeviceTestBuilder.ANDROID_TEST_TYPE)
        ?.enable = false
}
```

`HasDeviceTestsBuilder` (implementada por `ApplicationVariantBuilder` y `LibraryVariantBuilder`,
`com.android.tools.build:gradle-api:9.3.3`) expone `deviceTests: Map<String, DeviceTestBuilder>`;
`DeviceTestBuilder.ANDROID_TEST_TYPE` es la clave del test instrumentado y `enable: Boolean` es
el interruptor soportado por AGP para no crear ese componente. Con `enable = false`, AGP no
genera ninguna tarea `connectedDebugAndroidTest` para el módulo (verificado con
`./gradlew :core:data:tasks`: no aparece esa tarea, solo los agregados `connectedAndroidTest` /
`connectedCheck`), así que no hay manifest de instrumentación, ni APK de test, ni
`ClassNotFoundException`. `testInstrumentationRunner` y la dependencia `androidx-test:runner`
vuelven a declararse sin condición en todos los módulos Android: con el componente desactivado,
son inertes para los módulos sin `src/androidTest`, así que no hace falta un segundo mecanismo
para ellos (una sola condición, un solo interruptor).

Verificado con `./gradlew connectedDebugAndroidTest` en el proyecto completo: `:core:data`,
`:core:platform`, `:core:designsystem`, `:core:domain`, `:core:testing` y `:feature:settings` no
ejecutan ninguna tarea `connectedDebugAndroidTest`; `:app` (8 tests) y `:feature:session`
(2 tests) siguen pasando. `BUILD SUCCESSFUL`.

**Alternativas consideradas:**
- Condicionar solo `testInstrumentationRunner` y la dependencia del runner (intento inicial):
  descartada por el `ClassNotFoundException` de arriba.
- Mantener el runner global y desactivar la tarea `connectedDebugAndroidTest` con un `onlyIf`:
  sigue construyendo e instalando el APK de test vacío antes de que `onlyIf` salte (el coste que
  D1 quiere evitar), y es un mecanismo de Gradle por fuera de la Variant API cuando AGP ya ofrece
  uno dedicado. Descartada.
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
- [Un módulo nuevo con `src/androidTest` pero sin tests reales (cero clases `@Test`)] → D1 solo
  mira si el directorio existe, no si contiene tests, así que ese módulo mantendría el
  componente `AndroidTest` activo y volvería a construir e instalar un APK que reporta
  "0 tests" sin fallar (el mismo comportamiento verificado hoy para los módulos actuales antes
  de D1); no es un problema nuevo, solo queda sin resolver para ese caso límite.

## Migration Plan

1. `KotlinAndroid.kt`: desactivar el componente `AndroidTest` con la Variant API cuando no hay
   `src/androidTest` (D1).
2. `Quality.kt`: ampliar `source` de `detekt` (D4).
3. Corregir el `MaxLineLength` ya encontrado en `PersistenceWiringTest.kt` (hallazgo "Corregir"
   de la exploración), para que `detekt` siga en verde al activarse D4.
4. `.github/workflows/ci.yml`: nuevo job de emulador (D2, D3).
5. `docs/deuda-tecnica.md`: cerrar las entradas correspondientes.

Sin pasos de rollback de datos: es infraestructura de build y CI, sin estado persistente que
migrar.
