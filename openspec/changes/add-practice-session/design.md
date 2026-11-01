# Design

## Context

Ya existen los casos de uso de la tanda (`ObtainActiveBatch`, `AnswerPhrase`, `CloseActiveBatch`) y sus puertos (`PracticeRepository`, `CatalogRepository`, `SettingsRepository`), cableados en `:app` por `PersistenceModule` (ver `openspec/specs/practice-persistence`). `ObtainActiveBatch` devuelve solo `ActiveBatchResult` con `PracticeBatch`, que contiene ids y progreso, pero no el texto de las frases. La pantalla necesita el texto en español y en portugués de cada frase. `:feature:session` solo puede depender de `:core:domain` y `:core:designsystem` (ADR-0010; el plugin `fraseafrase.android.feature` añade esas dependencias, Compose, Hilt, lifecycle y `:core:testing` en test).

`AssetsCatalogDataSource.phrases()` parsea el JSON en cada llamada y no memoiza, aunque ADR-0013 dice "cargado en memoria". `:core:designsystem` no tiene todavía ningún componente `Ds*`. `FraseAFraseApp` es un marcador sin navegación.

## Goals / Non-Goals

**Goals:**
- Una pantalla de práctica que cumpla las specs `practice-session` sin reglas nuevas de dominio.
- Un único punto de lectura para la pantalla que devuelva la tanda con su texto, para que el ViewModel no conozca el catálogo ni la persistencia.
- Estado de presentación inmutable (ADR-0006) con las acciones de la pantalla.
- Cumplir ADR-0007: tests unitarios de dominio y ViewModel, y tests de UI con Compose sobre `PracticeSessionScreen`.

**Non-Goals:**
- Reglas de composición, cierre o racha: se reutilizan tal cual.
- Navegación entre pantallas, ajustes, progreso o TTS (ver proposal.md, Fuera de alcance).
- Componentes `Ds*` compartidos: ninguno se necesita en una segunda feature todavía.

## Decisions

### D1. Caso de uso de lectura `ObtainPracticeSession` en `:core:domain`

Un nuevo caso de uso en `core.domain.practice` (clase con `operator fun invoke`, `@Inject constructor`, como el resto), que depende de `ObtainActiveBatch` y de `CatalogRepository`:

1. Llama a `ObtainActiveBatch()`.
2. Si el resultado es `Existing` o `Created`, llama a `CatalogRepository.phrases()`. Si el resultado es `Failed`, devuelve `CatalogFailed(error)`. Si es `Loaded`, une cada `PhraseProgress` con su `CatalogPhrase` por `PhraseId`, en el orden de la tanda.
3. Si el resultado de `ObtainActiveBatch` es `NothingAvailable`, devuelve `NothingToPractice`. Si es `CatalogUnavailable`, devuelve `CatalogFailed(error)`.

Devuelve un `sealed interface PracticeSessionResult`:
- `Ready(items: List<PracticeItem>, uncertifiedCount: Int)`, donde `PracticeItem(id, source, target, streak, isKnown)` es una proyección inmutable. `isKnown` es `streak >= 3` dentro de la tanda (la "sabida" de la pantalla). No se llama `isCertified` para no confundirlo con la certificación al cerrar de ADR-0012, que es el `isCertified` de `PhraseProgress`.
- `NothingToPractice`.
- `CatalogFailed(error: CatalogLoadError)`. Se llama distinto de `ActiveBatchResult.CatalogUnavailable` a propósito, para no confundirlos en los `when`.

Una frase de la tanda que no existe en el catálogo es un dato inconsistente (los ids publicados son permanentes, ADR-0013). Se lanza `IllegalArgumentException` con el id, como ya hace `practice-batch` con "Progreso sin entrada en el catálogo". El ViewModel no la captura: es un error de programación o de datos, no un estado de la pantalla.

**Alternativa descartada:** ampliar `ActiveBatchResult` con el catálogo. Mezcla la composición con la lectura de texto.

**Alternativa descartada:** que el ViewModel llame a `CatalogRepository` directamente. Viola "los ViewModels dependen solo de casos de uso" (CLAUDE.md).

**Alternativa descartada:** leer solo `PracticeRepository.read()`. No sirve: la tanda no existe hasta que `ObtainActiveBatch` la compone.

**Coste conocido y tarea propia:** el catálogo se leería dos veces por operación. Se resuelve memoizando la carga en `AssetsCatalogDataSource` (ver D9), así que la segunda lectura no vuelve a parsear el JSON.

### D2. Recarga tras cada acción, sin copia local de la lista

Tras `AnswerPhrase` o `CloseActiveBatch`, el ViewModel vuelve a llamar a `ObtainPracticeSession` y reemplaza la lista. La siguiente frase y su orden los decide la persistencia (specs `practice-session`, "Reanudar la tanda activa" y "Cierre rechazado por cambio de tanda").

**Doble toque:** antes de llamar a `AnswerPhrase`, el ViewModel pone `busy = true` de forma síncrona en el estado y descarta cualquier `Answer` mientras `busy` sea cierto. `busy` vuelve a false al terminar la recarga. Sin esto, dos toques rápidos registrarían dos respuestas sobre la misma revelación, porque `AnswerPhrase` no falla si la frase sigue en la tanda.

### D3. Estado de presentación

`PracticeSessionUiState` (inmutable):
- `Loading`
- `Practicing(current: PracticeItem?, revealed: Boolean, busy: Boolean, closeWarning: Int?)`, donde `closeWarning` es el número de no certificadas que muestra el diálogo (null si no hay diálogo abierto). Es el único sitio donde vive ese número: el ViewModel no guarda ningún contador privado.
- `NothingToPractice`
- `CatalogError`

Acciones `PracticeSessionAction`: `Reveal`, `Answer(SelfAssessment)`, `RequestClose`, `CancelClose`, `ConfirmClose`. Un único `onAction()` en el ViewModel (ADR-0006).

`revealed` y `busy` viven en el `StateFlow` del ViewModel, en memoria. Sobreviven a la rotación, porque el ViewModel sobrevive a los cambios de configuración. No usan `SavedStateHandle`: si Android mata el proceso, la frase reaparece oculta, lo que coincide con el comportamiento al reabrir la app de la spec.

Reglas que el ViewModel aplica:
- `Answer` se ignora si `revealed` es false, si `busy` es true o si `closeWarning` no es null.
- `ConfirmClose` se ignora si `closeWarning` es null.
- `RequestClose` se ignora si `busy` es true (cerrar con una respuesta en curso no abre el diálogo, spec `practice-session`). Si no, lee la sesión con `ObtainPracticeSession` y abre el diálogo con su `uncertifiedCount`; se acepta también el cero, como exige `practice-batch`.

### D4. Cierre en dos pasos y diálogo modal

`RequestClose` (si no hay `busy`) vuelve a leer la sesión con `ObtainPracticeSession` y pone en `closeWarning` su `uncertifiedCount`. La lectura es asíncrona y no se guarda nada fuera del estado. La pantalla muestra un `AlertDialog` modal con ese número. El botón de cerrar de la pantalla se muestra deshabilitado mientras `busy` es true. `ConfirmClose` llama a `CloseActiveBatch(expectedUncertified = closeWarning)`, el mismo número que vio el usuario, y después vuelve a llamar a `ObtainPracticeSession`, que compone la tanda siguiente con `ObtainActiveBatch` (regla "Composición única tras el cierre" de `practice-persistence`). Los resultados `Stale` y `NoActiveBatch` solo recargan. `CancelClose` pone `closeWarning` a null.

### D5. Textos en `:feature:session`

Las cadenas de la pantalla van en `feature/session/src/main/res/values/strings.xml`, en español, sin literales en Compose. Los colores, dimensiones y duraciones salen de `MaterialTheme` y `AppTheme.spacing` / `AppTheme.motion`. El texto del diálogo usa plural de Android (`pluralStringResource`) para "1 frase pasará" y "N frases pasarán".

### D6. Sin componentes `Ds*` en este cambio

La tarjeta de frase y los botones son composables privados de `:feature:session`. Se promocionan a `:core:designsystem` cuando una segunda feature los necesite (regla de CLAUDE.md).

### D7. Navegación: sin librería nueva

`FraseAFraseApp` aloja `PracticeSessionRoute` como único destino inicial. No se añade `androidx.navigation`: no hay segunda pantalla. La navegación llegará con `:feature:settings`, y ahí se justificará la dependencia.

### D8. Inyección y dispatchers

`PracticeSessionViewModel` usa `@HiltViewModel` con `ObtainPracticeSession`, `AnswerPhrase` y `CloseActiveBatch`. Las lecturas y escrituras van con `IoDispatcher` dentro de `:core:data` (ya existente). El ViewModel usa `viewModelScope`, que despacha en `Dispatchers.Main.immediate`. Los tests registran `MainDispatcherExtension` de `:core:testing` (ADR-0006, ADR-0007). El plugin de feature ya añade Hilt, lifecycle y `:core:testing`: no hay dependencias nuevas.

### D9. Memoizar el catálogo en `:core:data`

`AssetsCatalogDataSource` pasa a cargar el catálogo una vez y reutilizar el resultado en las llamadas siguientes, como exige ADR-0013 ("cargado en memoria"). El resultado de la carga, sea `Loaded` o `Failed`, se guarda; la memoización es thread-safe con un `Mutex`. Sin cambios en el puerto `CatalogRepository`, así que no toca el dominio. Es un cambio de implementación con su test de contrato.

### D10. Espresso 3.7.0 para los UI tests

El BOM de Compose 2026.09.00 resuelve `espresso-core` 3.5.0 de forma transitiva. Esa versión falla al arrancar `createComposeRule` en un emulador con Android 17 (`NoSuchMethodException: android.hardware.input.InputManager.getInstance`), antes de ejecutar ningún test. Se fija `espresso-core` 3.7.0 solo en `androidTest` de `:feature:session`, con una entrada en `gradle/libs.versions.toml`. Verificado en el emulador: con 3.6.1 los 7 UI tests fallan con el mismo error (la llamada que falla está dentro de `espresso-core`), y con 3.7.0 los 7 pasan. Versión estable publicada en Google Maven. No se cambia el BOM ni otras dependencias. Alternativa descartada: Robolectric, que añadiría una librería nueva y se apartaría de ADR-0007.

## Estrategia de test

| Componente | Tipo de test | Dobles |
|---|---|---|
| `ObtainPracticeSession` | Unitario en `:core:domain` con los casos de uso reales: `ObtainActiveBatch` y `ComposeNextBatch(FixedClock)` sobre `FakePracticeRepository`, `FakeCatalogRepository`, `FakeSettingsRepository` | Fakes de `:core:testing`, `FixedClock` |
| `AssetsCatalogDataSource` (memoización) | Unitario en `:core:data`: segunda llamada no vuelve a parsear, y el error también se memoiza | Asset de prueba |
| `PracticeSessionViewModel` | Unitario en `:feature:session` con `MainDispatcherExtension`, Turbine, y los casos de uso reales sobre los fakes | Fakes de `:core:testing`, `FixedClock` |
| `PracticeSessionScreen` | Compose UI Test en `androidTest` (`ui-test-junit4`, ya en el catálogo): verifica en pantalla "no ofrece acierto ni fallo", el diálogo con su número, "no muestra su texto en portugués", la marca de sabida y el estado vacío | Estado `Practicing` construido a mano |
| Destino inicial | Unitario en `:app`: una constante `PRACTICE_START_DESTINATION` comprobada en JVM | — |
| Integración | Verificación manual con la skill `run` en el emulador | — |

Los tests de `androidTest` ejecutan con `./gradlew connectedDebugAndroidTest` y no entran en `./gradlew check`. Hace falta un emulador o dispositivo. Se justifican por ADR-0007, que exige Compose UI Test sobre `XxxScreen`. No se añade Robolectric: `ui-test-junit4` ya está en el catálogo y no hace falta una librería nueva.

Cobertura: `:core:domain` ≥ 90 %, `:feature:session` ≥ 80 % (ViewModel), `:core:data` según el umbral del módulo. `./gradlew check` y `./gradlew koverVerify` en verde antes de cerrar.

## Risks / Trade-offs

- **[Tests de UI fuera de `check`]** → Un cambio de pantalla puede romper los UI tests sin que `check` falle. Mitigación: se ejecutan en la verificación final con `connectedDebugAndroidTest`, y la tarea 5 lo exige.
- **[Frase de la tanda sin texto en el catálogo]** → Se lanza `IllegalArgumentException` (D1). Improbable por la permanencia de ids (ADR-0013). La pantalla no lo captura: el fallo es visible en desarrollo, no se oculta.
- **[Doble toque o respuesta con diálogo abierto]** → Mitigado por `busy` y las reglas de D3, con escenarios en la spec.
- **[Autoevaluación honesta]** → Igual que ADR-0012: la app no verifica la respuesta.
- **[Memoización del catálogo]** → Si el asset cambia durante la ejecución, la app no lo ve hasta reiniciar. Aceptable: los assets están empaquetados en el APK y no cambian en ejecución.

## Open Questions

Ninguna que cambie specs, enfoque o tareas.
