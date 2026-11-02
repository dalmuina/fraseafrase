# Proposal

## Why

`connectedDebugAndroidTest` fuerza el `testInstrumentationRunner` en todos los módulos Android
(`KotlinAndroid.kt`), incluidos `:core:data` y `:core:platform`, que no tienen ningún test
instrumentado (`src/androidTest`). Instalar y arrancar la instrumentación con cero clases de
test es un modo de fallo distinto y documentado de `AndroidJUnitRunner` ("no tests found" /
"empty test suite"), separado del `ClassNotFoundException` ya resuelto en
`1.0.0/fix-instrumented-test-runner`. Además, los tests instrumentados solo se ejecutan en
local: la CI (`.github/workflows/ci.yml`) no tiene ningún job de emulador, así que un fallo de
cableado de Hilt o de UI solo se detecta si alguien ejecuta `connectedDebugAndroidTest` antes de
integrar. Por último, `detekt` no analiza `src/androidTest`: una infracción allí pasa
desapercibida (comprobado con una infracción real de `MaxLineLength` en
`PersistenceWiringTest.kt`).

## What Changes

- El `testInstrumentationRunner` y la dependencia `androidx-test-runner` solo se declaran para
  módulos Android que tengan `src/androidTest` (deja de forzarse en todos los módulos Android).
- Nuevo job de CI que ejecuta `connectedDebugAndroidTest` del proyecto completo en un emulador
  (acción `reactivecircus/android-emulator-runner`, API 34), como paso informativo (no bloquea
  el merge por ahora, a falta de protección de ramas en GitHub).
- La tarea `detekt` existente de cada módulo amplía su `source` para incluir
  `src/androidTest/kotlin` cuando ese directorio existe, en vez de crear una tarea separada.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: es un cambio de infraestructura de build y CI, sin comportamiento de producto
observable por el usuario ni cambios de requisito en las specs vigentes. `skip_specs: true` en
`.openspec.yaml`.

## Impact

- `build-logic/convention/src/main/kotlin/com/dalmuina/fraseafrase/buildlogic/KotlinAndroid.kt`:
  runner e instrumentación condicionales a la existencia de `src/androidTest`.
- `build-logic/convention/src/main/kotlin/com/dalmuina/fraseafrase/buildlogic/Quality.kt`:
  `source` de la tarea `detekt` ampliado a `src/androidTest/kotlin`.
- `.github/workflows/ci.yml`: nuevo job de emulador.
- `docs/deuda-tecnica.md`: cierra las entradas "Job de tests instrumentados en la CI" y
  "`src/androidTest` fuera de detekt" (ambas del cambio `add-hilt-wiring-tests`), y actualiza el
  "Pendiente" de la sección "Runner de tests instrumentados".
- No se toca código de dominio, presentación ni datos de producción.

ADR afectados: ninguno contradice lo propuesto. ADR-0007 (tests y cobertura) ya exige detekt sin
errores; este cambio hace que `src/androidTest` entre en esa misma puerta. No se necesita un ADR
nuevo.

## Fuera de alcance

- El test de arranque de `FraseAFraseApp` (se hará en el cambio de navegación, según
  `docs/deuda-tecnica.md`).
- Proteger `develop`/`master` en GitHub (deuda separada, fuera del repositorio).
- Cambiar la API de compilación/lint o separar `detekt` en una tarea distinta para
  `androidTest`.
