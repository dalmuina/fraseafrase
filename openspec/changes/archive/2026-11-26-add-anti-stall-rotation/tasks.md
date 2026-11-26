# Tasks

## 1. Registro de respuestas al autoevaluar (`:core:domain`)

> Nota: 1.2 toca también `:core:testing` (builder `anAnswerHistory`), porque los tests de 1.3
> y siguientes los necesitan para compilar. Sin cambios de comportamiento en la composición.

Escenarios: `specs/spaced-repetition/spec.md` → "Invariantes del registro de respuestas" (salvo "Sesiones seguidas
sin sesión respondida se rechazan", que va al grupo 3), "Registro de respuestas por frase", "Componer una tanda no
cambia el registro de respuestas", "Reversión de la última autoevaluación"; `specs/practice-batch/spec.md` →
"Cambiar el nivel CEFR no toca el registro de respuestas".
Ficheros: `spacedrepetition/AnswerHistory.kt` (nuevo) / `AnswerHistoryTest.kt` (nuevo),
`spacedrepetition/PhraseProgress.kt` / `PhraseProgressTest.kt`, `core/testing/.../PhraseProgressBuilders.kt`.
Design D1, D5.

- [x] 1.1 Test rojo en `AnswerHistoryTest`: "Sesión respondida negativa se rechaza", "Sesión respondida con cero
  sesiones seguidas se rechaza", "Sesión fallada posterior a la respondida se rechaza" y "Registro coherente se
  acepta". Verificación: no compila o falla porque `AnswerHistory` no existe.
- [x] 1.2 Implementar `AnswerHistory` con sus `require` y añadir `answerHistory: AnswerHistory? = null` como último
  parámetro de `PhraseProgress`; en `:core:testing`, builder `anAnswerHistory`. `aPhraseProgress` no gana parámetro
  (un sexto parámetro incumple `LongParameterList` de detekt): los tests usan `.copy(answerHistory = ...)`.
  Verificación: 1.1 pasa y `./gradlew :core:domain:check :core:testing:check` en verde.
- [x] 1.3 Tests rojos en `PhraseProgressTest` para los siete escenarios de "Registro de respuestas por frase" (primera
  respuesta, sesión siguiente, misma sesión, hueco, fallo, acierto que conserva el fallo, acierto en repaso
  adelantado). Verificación: fallan porque `record` no actualiza el registro.
- [x] 1.4 Implementar `AnswerHistory?.afterAnswer(session, missed)` y llamarla desde las tres ramas de
  `PhraseProgress.record`; actualizar el KDoc con ADR-0027. Verificación: 1.3 pasa.
- [x] 1.5 Tests de caracterización: "Revertir un fallo restaura el registro de respuestas" (`PracticeBatchTest` o
  `RevertLastAnswerTest`), "Generar sesión no cambia el registro" (`GenerateSessionTest`) y "Cambiar el nivel CEFR
  no toca el registro de respuestas" (`RegenerateActiveBatchForLevelTest`). Verificación: pasan sin más cambios de
  producción y `./gradlew :core:domain:check` en verde.

## 2. Composición por tramos (`:core:domain`)

> Nota: 2.5 puede tocar `:feature:session`, `:core:data` o `:app` solo en datos de test: el nuevo límite de nuevas
> rompe en el mismo commit los tests que fijaban el cálculo antiguo, y el hook exige `check` global en verde.

Escenarios: `specs/practice-batch/spec.md` → "Composición de la tanda por tramos", "Frases repetidas y falladas por
volver", "Frases en descanso", "Prioridad de las pendientes", "Orden del repaso adelantado", "Límite de
frases nuevas por tanda", "Candidatas a la tanda nueva" ("Una frase de nivel alto sin vencer no entra"), "Reparto
de las nuevas en la tanda", "Regeneración de la tanda activa por cambio de nivel CEFR sin resolución" ("La tanda
siguiente se compone por cuotas con el nuevo nivel"); `specs/practice-session/spec.md` → "Generar sesión muestra la
tanda siguiente con sus arrastradas primero", "Generar sesión con todas certificadas deja la tanda siguiente sin
arrastradas".
Ficheros: `spacedrepetition/BatchTranches.kt` (nuevo), `spacedrepetition/ComposeNextBatch.kt` /
`ComposeNextBatchTest.kt`, `practice/RegenerateActiveBatchForLevelTest.kt`. Design D2, D3.

- [x] 2.1 Tests rojos en `ComposeNextBatchTest` para "Límite de frases nuevas por tanda" (ocho escenarios) y
  reescritura de los tests del requisito retirado "Número de frases nuevas según el aprendizaje" y de "Una frase de
  nivel alto sin vencer no entra". Verificación: fallan por el número de nuevas.
- [x] 2.2 Tests rojos en `ComposeNextBatchTest` para los seis escenarios de "Composición de la tanda por tramos" y
  los tres de "Frases repetidas y falladas por volver" (la recomposición en la misma sesión se prueba componiendo con
  la sesión actual igual a la última sesión respondida). Los escenarios vigentes de "Prioridad de las pendientes" y
  "Orden del repaso adelantado" deben seguir en verde. Verificación: los nuevos fallan por el contenido o el orden
  de la tanda.
- [x] 2.3 Tests rojos en `ComposeNextBatchTest` para los cinco escenarios de "Frases en descanso" y en
  `RegenerateActiveBatchForLevelTest` para "La tanda siguiente se compone por cuotas con el nuevo nivel" (12
  pendientes y 8 nuevas). Verificación: fallan con el compositor actual.
- [x] 2.4 Implementar `BatchTranches` (tramos 1 a 7, límite de nuevas, definiciones de design D3) y simplificar
  `ComposeNextBatch` (sin techo ni hueco), con KDoc que cite ADR-0027. Verificación: 2.1–2.3 pasan, los tests de
  reparto y de repaso adelantado siguen en verde y `./gradlew :core:domain:check` en verde (sin desactivar reglas de
  detekt).
- [x] 2.5 Ajustar solo los datos de los tests de `:core:domain`, `:feature:session`, `:core:data` o `:app` que
  fijen el cálculo antiguo de nuevas, sin cambiar lo que prueban; comprobar que los dos escenarios de
  `practice-session` del encabezado siguen en verde con su resultado actual. Verificación: `./gradlew
  :core:domain:check :feature:session:check :core:data:check` en verde.

## 3. Persistencia del registro y esquema v5 (`:core:data`)

Escenarios: `specs/practice-persistence/spec.md` → "El registro de respuestas sobrevive al reinicio", "Una base
anterior se abre vacía"; `specs/spaced-repetition/spec.md` → "Sesiones seguidas sin sesión respondida se rechazan".
Ficheros: `practice/PhraseProgressEntity.kt`, `practice/PracticeMapping.kt`, `practice/PracticeDatabase.kt`,
`schemas/com.dalmuina.fraseafrase.core.data.practice.PracticeDatabase/5.json` (generado) / `PracticeRestartTest.kt`,
`RoomPracticeDataSourceReadTest.kt`, `PracticeMigrationTest.kt`. Design D4.

- [x] 3.1 Test rojo en `PracticeRestartTest`: "El registro de respuestas sobrevive al reinicio" (una frase con
  registro y otra sin él). Verificación: falla porque el registro no se persiste.
- [x] 3.2 Test rojo en `RoomPracticeDataSourceReadTest`: una fila con `consecutive_sessions` mayor que 0, u otra con
  `last_miss_session`, sin `last_answered_session` se rechaza al leerla. Verificación: falla porque las columnas no
  existen.
- [x] 3.3 Test rojo en `PracticeMigrationTest`: "Una base anterior se abre vacía" (v4 con una fila de `pt-PT`,
  `identity_hash` de `4.json`, `PRAGMA user_version = 4`). Verificación: falla porque las columnas nuevas no
  existen.
- [x] 3.4 Implementar las tres columnas con valores por defecto en `PhraseProgressEntity`, el mapeo en ambos
  sentidos en `PracticeMapping`, `PracticeDatabase.version = 5` con el KDoc del precedente, y commitear `5.json`
  generado por la build. Verificación: 3.1–3.3 pasan y `./gradlew :core:data:check` en verde.

## 4. Cierre y verificación (sesión principal)

- [x] 4.1 Líneas de estado (ADR-0001): cabeceras ADR-0027 → `Aceptado`, ADR-0026 → `Sustituido por ADR-0027` y
  ADR-0025 → `Sustituido por ADR-0026 y ADR-0027`; en la columna Estado de `docs/adr/README.md`, 0027 → `Aceptado`,
  0026 → `Sustituido por 0027` y 0025 → `Sustituido por 0026 y 0027`. Verificación: el README y las cabeceras
  coinciden.
- [x] 4.2 `CLAUDE.md`, regla de oro 4: citar ADR-0027 para la composición de la tanda y el registro de respuestas.
  Verificación: la regla no remite a ADR-0026 como composición vigente.
- [x] 4.3 `./gradlew check` y `./gradlew koverVerify` en verde (dominio 90 %, presentación 80 %, global 70 %).
- [x] 4.4 Comprobación manual con `assembleDebug`, N = 10 y 5 o 6 sesiones: la app arranca sin progreso, la sesión
  2 tiene 2 nuevas y una frase fallada vuelve en la sesión siguiente. Si no hay dispositivo, indicarlo en la tarea.

## Workflow follow-up

- `code-reviewer` (hasta dos rondas), `/opsx:verify`, `/opsx:archive` y commit de archive (abre la PR por hook).
