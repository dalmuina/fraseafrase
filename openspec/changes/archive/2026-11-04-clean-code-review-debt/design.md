# Design

## Context

Seis limpiezas de `docs/deuda-tecnica.md`, de distinto origen (revisión de `add-practice-session`,
revisión de tests de repetición espaciada, revisión de fakes y builders), sin relación funcional
entre sí pero homogéneas en naturaleza: legibilidad y mantenibilidad interna, sin cambio de
comportamiento. Ver `proposal.md` - Why.

Puntos verificados en el código actual que cambian el alcance respecto al texto original de la
deuda:
- `requireNotNull` en `AnswerPhrase` ya está resuelto (no es tarea de este cambio).
- `PracticeSessionUiState.Practicing(current = null)` sigue existiendo: `ObtainPracticeSession`
  garantiza que `Ready.items` nunca está vacío (una tanda vacía produce `NothingToPractice`, no
  `Ready`), así que la rama `current == null` de `PracticingContent` (`PracticeSessionScreen.kt`)
  es código muerto en producción, pero sigue ahí.
- `inMemoryPracticeDatabase()` (`core/data/src/main/.../PracticeSchema.kt:81`) solo la usan cinco
  tests JVM dentro del propio módulo `:core:data` (`PracticeSchemaTest`, `PracticeTriggersTest`,
  `RoomPracticeCycleTest`, `RoomPracticeDataSourceReadTest`, `RoomPracticeDataSourceWriteTest`).
  No hace falta `testFixtures` ni tocar `:core:testing`: el problema es solo que vive en
  `src/main` sin usarla producción, no que cruce módulos.
- `progressWith` duplicado en `RecordSelfAssessmentTest` y `ResolveBatchClosureTest` tiene firmas
  distintas entre sí (`Streak`/`Int` para los mismos campos) y además duplica `aPhraseProgress`,
  que ya existe en `:core:testing` y ya es `testImplementation` de `:core:domain`.
- Los únicos builders de `:core:testing` con `Int` donde el dominio usa value class son
  `level`/`streak` de `aPhraseProgress` (`PhraseProgressBuilders.kt`); `CatalogPhraseBuilders.kt`
  ya envuelve sus parámetros primitivos en los value classes del dominio.

## Goals / Non-Goals

**Goals:**
- Dejar cada limpieza en el estado que su revisión original pedía, sin ampliarla.
- No introducir mecanismos nuevos (plugins de Gradle, dependencias) para resolver puntos cuyo
  alcance real no los necesita.

**Non-Goals:**
- No se añade `java-test-fixtures` ni se reorganiza `:core:testing`: innecesario para
  `inMemoryPracticeDatabase()` (ver Context).
- No se toca `CatalogPhraseBuilders.kt` ni otros builders: ya usan value classes donde corresponde.
- No se resuelven los puntos de infraestructura/ADR de la misma lista (Konsist, detekt 2.0,
  protección de ramas): quedan en `docs/deuda-tecnica.md` para su propio cambio, como ya indica
  `proposal.md`.

## Decisions

### D1. Helper del ViewModel para los estados `Practicing`

Dos helpers, no uno, porque las seis acciones de `PracticeSessionViewModel` no comparten la misma
forma: `reveal` y `cancelClose` son transformaciones puras del estado actual; `requestClose`,
`confirmClose`, `answer` y `playAudio` necesitan leer el estado antes **y** lanzar una corrutina.

- `currentPracticing(): PracticeSessionUiState.Practicing?` sustituye el cast
  `_uiState.value as? Practicing` repetido en los casos asíncronos.
- `updatePracticing(transform: PracticeSessionUiState.Practicing.() -> PracticeSessionUiState.Practicing)`
  sustituye el cast + asignación en los casos síncronos (`reveal`, `cancelClose`); un guard que no
  aplica devuelve `this` sin cambios (mismo efecto que el `return` actual, sin reemisión porque
  `StateFlow` no emite valores iguales).

Alternativa descartada: un único helper que envuelva también las ramas asíncronas (p. ej. que
reciba una `suspend` lambda). Oculta el control de flujo de `requestClose`/`confirmClose` (guard
de `busy` antes de la llamada a `obtainPracticeSession()`, reentrada tras el `await`) detrás de una
abstracción que no aporta legibilidad y complica seguir D3/D4 en el código.

### D2. Unificar `progressWith` con `aPhraseProgress`

En vez de mover los dos `progressWith` locales a un tercer fixture compartido, se eliminan y se
sustituyen por `aPhraseProgress` (`:core:testing`), ya disponible como `testImplementation` de
`:core:domain`. Requiere primero D3 (`aPhraseProgress` con value classes) para que las llamadas
con `Streak`/`Level` de `RecordSelfAssessmentTest` no pierdan expresividad.

Alternativa descartada: extraer un nuevo `progressWith` a `:core:testing` junto a `aPhraseProgress`.
Mantendría dos builders equivalentes para la misma entidad.

### D3. `aPhraseProgress` con `Level`/`Streak` en vez de `Int`

Cambia la firma de `level: Int = 0, streak: Int = 0` a `level: Level = Level(0), streak: Streak = Streak(0)`.
Afecta a los ficheros que llaman a `aPhraseProgress` (`grep -rl "aPhraseProgress("` localiza unos
15 ficheros en `:core:domain`, `:core:data`, `:feature:session` y `:feature:progress`, con muchas
más invocaciones que ficheros: algunos, como `ComposeNextBatchTest`, la llaman varias decenas de
veces). Todas pasan enteros literales y pasan a envolverlos (`Level(1)`, `Streak(2)`) o a usar
variables ya tipadas como value class cuando el test ya las tiene (como en `ResolveBatchClosureTest`,
cuyo `progressWith` local desaparece con D2). Mecánico, sin lógica nueva: el compilador obliga a
corregir cada sitio antes de que el módulo compile.

Alternativa descartada: dejar `Int` y añadir una sobrecarga con value classes. Mantiene la
inconsistencia que señaló la revisión y duplica la API del builder.

### D4. Mover `inMemoryPracticeDatabase()` dentro de `:core:data`

De `src/main/.../practice/PracticeSchema.kt` a un fichero nuevo en
`src/test/.../practice/PracticeTestDatabase.kt`, mismo módulo. Sigue `internal`; el `sourceSet`
de test de Kotlin ve las declaraciones `internal` de `src/main` igual que antes, y ahora además
los cinco tests que la usan están en el mismo `sourceSet` que la función.

### D5. Mensajes de `require` al inglés

`Level.kt` y `PhraseProgress.kt`: traducir los mensajes de `require` sin cambiar el texto más allá
de la traducción literal. Ningún test afirma sobre el texto del mensaje (`shouldThrow<IllegalArgumentException>`
sin `.message`), así que no hay riesgo de romper aserciones existentes.

### D6. Parametrizar `ReviewScheduleTest`

Los seis primeros tests (`nivel 0` a `nivel 5`, misma fecha de partida) comparten forma exacta:
`Level(n)` + fecha fija → fecha esperada. Se sustituyen por un `@ParameterizedTest` con
`@CsvSource` (nivel, fecha esperada). Los tres casos restantes (cambio de año en nivel 1 y 5, año
bisiesto) son casos límite distintos entre sí y se quedan como `@Test` independientes: forzarlos
al mismo `@ParameterizedTest` mezclaría dos preocupaciones (la tabla de intervalos por nivel y los
casos límite de fecha) en una sola fuente de datos.

## Risks / Trade-offs

- [D1 cambia el cuerpo de las seis acciones del ViewModel] → mitigado por los tests ya existentes
  de `PracticeSessionViewModelTest`/`PracticeSessionCloseTest`, que cubren cada escenario de D3/D4
  y deben seguir en verde sin modificarse (si hiciera falta modificarlos, sería señal de que el
  refactor cambió comportamiento, no solo forma).
- [D3 toca 16 ficheros de test] → cambio mecánico (envolver enteros en `Level(...)`/`Streak(...)`),
  sin lógica nueva; se verifica con `./gradlew test` en verde, sin necesidad de revisar cada
  aserción una por una.
- [D4 asume que `src/test` de Kotlin ve lo `internal` de `src/main` del mismo módulo] → es el
  comportamiento estándar de Kotlin/Gradle para un único módulo con sourceSets `main`/`test`; se
  confirma con la compilación de los cinco tests que la usan.

## Open Questions

(ninguna)
