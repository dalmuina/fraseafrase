# Design

## Context

Ver `proposal.md` (Why) y ADR-0027. Estado actual relevante:

- `PhraseProgress` (`:core:domain`, `spacedrepetition`) guarda nivel, racha, `answeredInBatch` y `dueAtSession`, y
  resuelve la autoevaluación en `record(assessment, currentSession)`, que ya recibe la sesión actual.
- `ComposeNextBatch` es una función pura que recibe el progreso, el catálogo, el nivel CEFR, N y la sesión actual.
  La usan `GenerateSession`, `ObtainActiveBatch` y `RegenerateActiveBatchForLevel` mediante `composeNextBatchInto`;
  ninguno cambia.
- La reversión guarda el `PhraseProgress` completo anterior (`UndoableAnswer.progressBefore`) y lo repone
  (`PracticeBatch.revertLastAnswer`): un campo nuevo dentro de `PhraseProgress` se restaura sin código adicional.
- `PracticeDatabase` está en la versión 4 con `fallbackToDestructiveMigration()` (`PracticeSchema`). Las
  versiones 2, 3 y 4 ya se subieron así, con un test de caracterización (`PracticeMigrationTest`).
- La primera sesión es la 0: `ObtainActiveBatch` compone con el contador de sesiones a 0.

## Goals / Non-Goals

**Goals:**

- Guardar el registro de respuestas como un único valor de dominio, actualizado solo al autoevaluar.
- Implementar los siete tramos de ADR-0027 sin superar los umbrales de complejidad de detekt.

**Non-Goals:**

- Cambiar puertos (`PracticeRepository`), casos de uso públicos, ViewModels o UI.
- Añadir triggers de SQLite para las invariantes del registro.

## Decisions

### D1. `AnswerHistory`: valor de dominio opcional en `PhraseProgress`

Nuevo `data class AnswerHistory(lastAnsweredSession: Int, consecutiveSessions: Int, lastMissSession: Int?)` en
`:core:domain` (`spacedrepetition/AnswerHistory.kt`). `PhraseProgress` gana
`answerHistory: AnswerHistory? = null` como último parámetro: `null` significa "nunca respondida". Las
invariantes del spec ("Invariantes del registro de respuestas") van en el `init` de `AnswerHistory` con `require`,
como el resto de entidades: `lastAnsweredSession ≥ 0`, `consecutiveSessions ≥ 1`, `lastMissSession` en
`0..lastAnsweredSession`. "Sesiones seguidas sin sesión respondida" es imposible por construcción (no hay
`AnswerHistory`), y el escenario se cubre en el mapeo de Room (D4).

Una función pura `AnswerHistory?.afterAnswer(session: Int, missed: Boolean): AnswerHistory` aplica la regla de
ADR-0027. `PhraseProgress.record` la llama en las tres ramas (acierto pendiente, acierto en repaso adelantado y
fallo), así que el registro se actualiza cualquiera que sea el resultado.

- **Alternativa descartada:** tres campos sueltos en `PhraseProgress`. Permiten estados incoherentes (sesiones
  seguidas sin sesión respondida) y alargan un constructor con 172 llamadas en el código.
- **Por qué con valor por defecto:** las llamadas existentes (incluida `aPhraseProgress`) y las frases nuevas
  compuestas siguen compilando sin cambios; solo los tests que prueban el registro lo asignan.

### D2. Composición por tramos en una clase interna aparte

`ComposeNextBatch` mantiene su firma, las precondiciones, la selección de nuevas por catálogo, el reparto
(`interleave`) y el resultado `NothingAvailable`. El relleno de no nuevas pasa a una clase `internal` nueva
`BatchTranches` (`spacedrepetition/BatchTranches.kt`), construida con el progreso del nivel CEFR elegido, la sesión
actual, N y el orden de catálogo. Expone:

- `newCount(available: Int, firstBatchOfLevel: Boolean): Int`: límite de nuevas de los tramos 1 a 4
  ("Límite de frases nuevas por tanda").
- `fill(newCount: Int, available: Int): Fill`, con `Fill(notNew: List<PhraseProgress>, extraNew: Int)`: recorre los
  tramos 1 a 7 y devuelve las no nuevas en orden de tramo y cuántas nuevas añade el tramo 6. Los tramos 1 a 4 paran
  cuando las no nuevas llegan a `N − newCount`; los 5 a 7, cuando las no nuevas más `newCount` más las nuevas del
  tramo 6 llegan a N (el tramo 5 nunca desplaza a las nuevas del límite).

Cada tramo es una función privada corta que añade a un acumulador común (lista ordenada, ids ya tomados y
contador de repetidas). Constantes en el `companion object`: `MAX_CONSECUTIVE_SESSIONS = 6`, fracciones `1/5`
(nuevas), `1/4` redondeado hacia arriba (mínimo de repetidas, `(N + 3) / 4`) y `2/5` (máximo), con aritmética
entera como hoy. La prioridad `nivel + (dueAtSession − sesión)` con desempate por catálogo se mueve tal cual.

`ComposeNextBatch` ya no calcula techo ni hueco: elimina `newCountOf` y sus cuatro constantes.

- **Alternativa descartada:** todo en `ComposeNextBatch`. Con siete tramos supera `LongMethod` y
  `CyclomaticComplexMethod` de detekt, y no se pueden desactivar reglas (CLAUDE.md).
- **Por qué `internal` y no un caso de uso:** no tiene E/S ni otro consumidor; se prueba a través de
  `ComposeNextBatch`, cuyos tests siguen los escenarios del spec. Un test propio de `BatchTranches` solo si un
  tramo no se puede aislar desde la composición.

### D3. Definiciones evaluadas en la composición

Con `s` la sesión actual y `h` el `answerHistory` de la frase:

- Repetida: `h?.lastAnsweredSession == s − 1`.
- En descanso: repetida y `h.consecutiveSessions ≥ 6`.
- Fallada por volver: `h?.lastMissSession == h?.lastAnsweredSession` (ambos no nulos) y
  `h.lastAnsweredSession ≥ s − 2`. Incluye el caso de una recomposición en la misma sesión (`lastAnsweredSession
  == s`), en el que la frase fallada vuelve a entrar en la tanda recompuesta (spec "Frases repetidas y falladas por
  volver", ADR-0027).
- Primera tanda del nivel: ninguna frase del nivel CEFR elegido tiene progreso.
- Frases de nivel 0: progreso del nivel CEFR elegido con `level == 0`, estén o no pendientes (como hoy).

Las definiciones viven como funciones privadas de `BatchTranches`; no se exponen en `PhraseProgress` porque solo
tienen sentido para componer.

### D4. Persistencia: tres columnas y versión 5 sin `Migration`

`PhraseProgressEntity` (`:core:data`) gana `last_answered_session INTEGER NULL`, `consecutive_sessions INTEGER NOT
NULL` y `last_miss_session INTEGER NULL`, con valores por defecto en Kotlin (0 en `consecutive_sessions`) para no
tocar las llamadas existentes. `PracticeMapping`:

- `toDomain()`: `last_answered_session` nulo → `answerHistory = null`; si no, `AnswerHistory(...)`. Una fila con
  `consecutive_sessions > 0`, o con `last_miss_session` no nulo, sin sesión respondida lanza
  `IllegalArgumentException` (escenario "Sesiones seguidas sin sesión respondida se rechazan" e invariante de la
  sesión fallada sin sesión respondida), igual que hoy un nivel fuera de rango.
- `toEntity()`: escribe los tres valores, o `NULL`, `0`, `NULL` sin registro.

`PracticeDatabase.version` pasa de 4 a 5 y se exporta `schemas/.../PracticeDatabase/5.json` (lo genera la build con
`exportSchema = true`). No se añade `Migration`: actúa `fallbackToDestructiveMigration()` (decisión del usuario en el
explore). El escenario "Una base anterior se abre vacía" se cubre con un test nuevo en `PracticeMigrationTest`,
copiando el patrón v3→v4 con el esquema y el `identity_hash` de `4.json`.

- **Fuente de datos:** sigue siendo una (`RoomPracticeDataSource : PracticeRepository`, ADR-0010); no cambia el
  puerto.
- **Sin triggers nuevos:** las invariantes se validan al construir el dominio; los triggers actuales protegen la
  tanda activa, no el progreso.

### D5. Estrategia de test

| Componente | Tipo | Tests |
|---|---|---|
| `AnswerHistory`, `PhraseProgress.record` | Unitario JUnit 6 | `AnswerHistoryTest` (nuevo), `PhraseProgressTest` |
| Reversión | Unitario | `PracticeBatchTest` o `RevertLastAnswerTest` (caracterización: pasa sin código nuevo) |
| `ComposeNextBatch` + `BatchTranches` | Unitario | `ComposeNextBatchTest` (reescritura de los escenarios de nuevas) |
| Recomposición sin tocar el registro | Unitario con `FakePracticeRepository` | `RegenerateActiveBatchForLevelTest`, `GenerateSessionTest` |
| Mapeo y reinicio | Unitario con Room en fichero (`BundledSQLiteDriver`) | `PracticeRestartTest`, `RoomPracticeDataSourceReadTest`/`WriteTest` |
| Esquema v5 | Unitario con Room | `PracticeMigrationTest` |

Fakes: `FakePracticeRepository` no cambia. `:core:testing` gana el builder
`anAnswerHistory(lastAnsweredSession, consecutiveSessions = 1, lastMissSession = null)`. `aPhraseProgress` no gana
parámetro (un sexto parámetro incumple `LongParameterList` de detekt): los tests usan
`aPhraseProgress(...).copy(answerHistory = anAnswerHistory(...))`.

## Risks / Trade-offs

- [Se pierde el progreso guardado al actualizar] → Aceptado por el usuario; la app no tiene usuarios reales y es
  el precedente de las versiones 2 a 4. Queda en ADR-0027 y en el proposal como **BREAKING**.
- [Muchos tests de composición fijan el número de nuevas antiguo] → El grupo de composición reescribe primero
  `ComposeNextBatchTest` en rojo; los demás tests que se rompan solo ajustan datos (sin cambiar lo que prueban) en el
  mismo grupo, para que el hook de `check` global siga en verde.
- [Escenarios con frases sin registro] → Una frase sin registro nunca es repetida, fallada por volver ni en descanso:
  con datos antiguos de tests la composición se reduce a pendientes, repaso adelantado y nuevas con el límite nuevo.
- [Complejidad de `BatchTranches`] → Funciones por tramo y constantes con nombre; los escenarios del spec cubren cada
  tramo por separado.
- [El límite de nuevas se supera en el tramo 6] → Documentado en ADR-0027 (2 de 180 simulaciones); preferible a
  repetir una frase una séptima sesión seguida.

## Migration Plan

Despliegue con la subida a la versión 5: al abrir, Room recrea las tablas vacías y la app se comporta como en el
primer uso. Sin rollback: volver a la versión 4 exigiría desinstalar (Room no baja de versión).
