# Tasks

## 1. Dominio: mensajes de `require` en inglés

- [x] 1.1 Traducir el mensaje de `require` de `Level.kt` (`core/domain/.../spacedrepetition/Level.kt`)
  del español al inglés, sin cambiar la condición. Verificar con
  `./gradlew :core:domain:test --tests "*LevelTest*"` en verde (ningún test afirma sobre el texto
  del mensaje, así que no debe fallar ninguno).
- [x] 1.2 Traducir los tres mensajes de `require` de `PhraseProgress.kt`
  (`core/domain/.../spacedrepetition/PhraseProgress.kt`) del español al inglés, sin cambiar las
  condiciones. Verificar con `./gradlew :core:domain:test --tests "*PhraseProgressTest*"` en
  verde.

## 2. Fixture de test: `aPhraseProgress` con value classes (D2, D3)

- [x] 2.1 Cambiar la firma de `aPhraseProgress` (`core/testing/.../PhraseProgressBuilders.kt`) de
  `level: Int = 0, streak: Int = 0` a `level: Level = Level(0), streak: Streak = Streak(0)`,
  eliminando el envoltorio `Level(level)`/`Streak(streak)` interno que ya no hace falta. Verificar
  compilando `:core:testing` (`./gradlew :core:testing:compileTestKotlin` o el `test` del módulo,
  que no tiene tests propios pero sí debe compilar).
- [x] 2.2 Actualizar todas las llamadas existentes a `aPhraseProgress(...)` (localizadas con
  `grep -rl "aPhraseProgress(" --include=*.kt .`, excluyendo el propio builder; unos 15 ficheros,
  con muchas más invocaciones que ficheros) para envolver los argumentos `level`/`streak` en
  `Level(...)`/`Streak(...)` donde se pasen enteros literales. Verificar con `./gradlew test` en
  verde en todos los módulos afectados (`:core:domain`, `:core:data`, `:feature:session`,
  `:feature:progress`).
- [x] 2.3 Eliminar el `progressWith` local de `RecordSelfAssessmentTest.kt` y sustituir sus dos usos
  por `aPhraseProgress(streak = Streak(n), answeredInBatch = ...)`. Verificar con
  `./gradlew :core:domain:test --tests "*RecordSelfAssessmentTest*"` en verde, mismos escenarios
  cubiertos que antes del cambio.
- [x] 2.4 Eliminar el `progressWith` local de `ResolveBatchClosureTest.kt` y sustituir sus usos por
  `aPhraseProgress(level = Level(n), streak = Streak(n), answeredInBatch = ..., nextReview = ...)`.
  Verificar con `./gradlew :core:domain:test --tests "*ResolveBatchClosureTest*"` en verde, mismos
  escenarios cubiertos que antes del cambio.

## 3. Dominio: parametrizar `ReviewScheduleTest` (D6)

- [x] 3.1 Sustituir los seis tests de nivel 0 a 5 (misma fecha de partida, 10-03-2026) por un único
  `@ParameterizedTest` con `@CsvSource` (nivel, fecha esperada o vacío para nivel 0), manteniendo
  el nombre de escenario de cada caso en un comentario sobre la fuente de datos. Dejar los tres
  casos límite (cambio de año en nivel 1, cambio de año en nivel 5, año bisiesto) como `@Test`
  independientes, sin tocarlos. Verificar con
  `./gradlew :core:domain:test --tests "*ReviewScheduleTest*"` en verde, con el mismo número de
  invocaciones de test que antes (nueve) visible en el reporte de Gradle.

## 4. Datos: mover `inMemoryPracticeDatabase()` a `src/test` (D4)

- [x] 4.1 Mover la función `inMemoryPracticeDatabase()` de
  `core/data/src/main/.../practice/PracticeSchema.kt` a un fichero nuevo
  `core/data/src/test/.../practice/PracticeTestDatabase.kt`, misma visibilidad `internal`. Ajustar
  los imports de los cinco tests que la usan (`PracticeSchemaTest`, `PracticeTriggersTest`,
  `RoomPracticeCycleTest`, `RoomPracticeDataSourceReadTest`, `RoomPracticeDataSourceWriteTest`) si
  el nuevo fichero cambia de paquete. Verificar con `./gradlew :core:data:test` en verde.
- [x] 4.2 Confirmar que `core/data/src/main` ya no referencia la función (ninguna clase de
  producción la usaba) y que `./gradlew :app:assembleDebug` sigue compilando sin cambios en el
  grafo de Hilt de producción.

## 5. Presentación: helpers del ViewModel de sesión (D1)

- [x] 5.1 Añadir `currentPracticing()` y `updatePracticing { ... }` a `PracticeSessionViewModel`
  (`feature/session/.../PracticeSessionViewModel.kt`) según D1 del diseño.
- [x] 5.2 Reescribir `reveal()` y `cancelClose()` con `updatePracticing { ... }`. Verificar con
  `./gradlew :feature:session:test --tests "*PracticeSessionViewModelTest*"` en verde, mismos
  escenarios de D3 cubiertos (revelar se ignora con `busy` o aviso de cierre abierto).
- [x] 5.3 Reescribir `requestClose()`, `confirmClose()`, `answer()` y `playAudio()` usando
  `currentPracticing()` en vez del cast repetido, sin cambiar su lógica asíncrona. Verificar con
  `./gradlew :feature:session:test --tests "*PracticeSessionViewModelTest*,*PracticeSessionCloseTest*"`
  en verde, mismos escenarios de D3/D4 cubiertos (cierre, autoevaluación, audio).

## 6. Verificación final

- [ ] 6.1 Ejecutar `./gradlew check` (tests + detekt + lint) en verde sobre todo el proyecto.
- [ ] 6.2 Ejecutar `./gradlew koverVerify` en verde (ningún cambio de este grupo debería mover la
  cobertura por debajo de los mínimos, ya que no se añade código de producción nuevo sin test).

## Workflow follow-up

- Revisión de `android-architect` (cambio ligero: configuración/infraestructura con diff pequeño y
  sin lógica de dominio nueva, por lo que basta una ronda) antes de `/opsx:apply`.
- Revisión de `code-reviewer` (una ronda) tras la implementación.
- `/opsx:verify` y `/opsx:archive` en la rama del cambio, antes de la PR a `develop`.
