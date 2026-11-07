# Tasks

## 1. Dominio: `dueAtSession` y resolución al instante

- [x] 1.1 Test: `PhraseProgress` con el nuevo campo `dueAtSession: Int` (no nulo), sin la invariante
  "nivel 0 ⇔ sin próximo repaso" ni "racha > 0 exige respondida en la tanda"; `dueAtSession` negativo se
  rechaza, según `specs/spaced-repetition/spec.md` ("Invariantes de PhraseProgress con sesión de
  vencimiento"). Verificación: falla por los campos/invariantes aún no cambiados.
- [x] 1.2 Implementar el cambio de `PhraseProgress` (sustituir `nextReview: LocalDate?` por
  `dueAtSession: Int`, actualizar invariantes). Verificación: tests de 1.1 en verde.
- [x] 1.3 Test: `ReviewSchedule` (o su sustituto) devuelve el enfriamiento en sesiones por nivel (1, 2, 4, 8,
  16), según "Enfriamiento en sesiones tras certificar". Verificación: falla por la función aún no existente.
- [x] 1.4 Implementar el enfriamiento en sesiones, sustituyendo `nextReviewAfter(level, from)` por una función
  pura de solo el nivel (sin `Clock`, sin fecha). Verificación: tests de 1.3 en verde.
- [x] 1.5 Test: `PhraseProgress.record(assessment, currentSession)` resuelve al instante certificación (racha
  3 → sube de nivel, racha a 0, `dueAtSession` = sesión + enfriamiento), acierto sin certificar
  (`dueAtSession` = sesión + 1) y fallo en cualquier nivel (`dueAtSession` = sesión + 1, nivel baja si era
  ≥ 1), según "Resolución de la autoevaluación" y "Bajada instantánea de nivel ante un fallo" (delta de
  `spaced-repetition`). Verificación: falla por la firma/lógica aún no cambiada.
- [x] 1.6 Implementar `record(assessment, currentSession: Int)` con la resolución instantánea completa.
  Verificación: tests de 1.5 en verde; retirar los tests que dependían de `ResolveBatchClosure`/cierre por
  lote (se migran o se eliminan según corresponda en el grupo 3).
- [x] 1.7 Test: "Frase pendiente de repaso por sesión" — `dueAtSession <= currentSession`, sin excepción de
  nivel 0, en `PracticeBatch`/`PhraseProgress` o donde viva esta comprobación. Verificación: falla antes de
  1.8.
- [x] 1.8 Implementar `isPendingAtSession`/equivalente, sustituyendo `isPendingOn(today)`. Verificación: tests
  de 1.7 en verde.

## 2. Dominio: composición sin cola de arrastradas

- [x] 2.1 Test: `ComposeNextBatch` recibe `progress`, catálogo, CEFR, N y la sesión actual (sin
  `CarryQueue`), con cuotas 20 % nuevas / 55 % a reforzar / 25 % dominadas, según los escenarios de
  `specs/practice-batch/spec.md` ("Composición de la tanda nueva por cuotas sobre el progreso",
  "Cuotas por defecto...", "Reforzar corto...", "Dominadas corto...", "Cálculo de las cuotas con N igual a
  20"). Verificación: falla por la firma/cuotas aún no cambiadas.
- [x] 2.2 Implementar la nueva firma y cuotas de `ComposeNextBatch`, calculando "a reforzar" y "dominadas"
  directamente sobre `progress` (sin `CarryQueue`), sin inyectar `Clock` (la sesión actual llega como
  parámetro). Verificación: tests de 2.1 en verde; revisar qué tests existentes de `ComposeNextBatchTest`
  siguen aplicando bajo el nuevo modelo y cuáles hay que retirar por depender de la cola o de fechas (p. ej.
  los de orden de cola, invariantes de cola); si alguno entra en conflicto y no está claro si retirarlo,
  para y pregunta, no decidas por tu cuenta.
- [x] 2.3 Test: el filtro CEFR se extiende a "a reforzar" y "dominadas" (nunca por debajo del nivel
  elegido), y "dominadas" admite `dueAtSession` futuro mientras "a reforzar" exige vencido, según el resto de
  escenarios de "Composición de la tanda nueva por cuotas sobre el progreso" y "Frases dominadas disponibles
  para repaso ligero antes de su vencimiento". Verificación: falla antes de 2.4.
- [x] 2.4 Implementar el filtro CEFR extendido y la exclusión mutua reforzar/dominadas. Verificación: tests
  de 2.3 en verde.

## 3. Dominio: retirar `CarryQueue`/`ResolveBatchClosure`, contador de sesiones, casos de uso

- [x] 3.1 Test: `PracticeSnapshot` expone `sessionCount: Int` en vez de `carry: CarryQueue`. Verificación:
  falla por el campo aún no cambiado.
- [x] 3.2 Implementar el cambio en `PracticeSnapshot` y retirar `CarryQueue` (clase) y sus tests dedicados.
  Verificación: tests de 3.1 en verde; `./gradlew :core:domain:compileKotlin` compila salvo los llamadores
  pendientes de los grupos siguientes (previsto).
- [x] 3.3 Test: `GenerateSession` incrementa `sessionCount` en 1 y compone con ese valor como sesión actual,
  sin ninguna resolución por lote (cada frase ya quedó resuelta en su autoevaluación), según "Generar sesión
  avanza el contador y compone". Verificación: falla por ausencia del nuevo comportamiento.
- [x] 3.4 Implementar la simplificación de `GenerateSession` (vacía la tanda activa, compone con
  `sessionCount + 1`, persiste el nuevo contador). Retirar `ResolveBatchClosure` y `BatchClosureOutcome` (sin
  más llamadores tras este cambio) y sus tests dedicados. Verificación: tests de 3.3 en verde.
- [x] 3.5 Test: `RegenerateActiveBatchForLevel` recompone con el CEFR y N recibidos, sin incrementar
  `sessionCount` y sin tocar racha/nivel/`dueAtSession` de ninguna frase, según "Regeneración de la tanda
  activa por cambio de nivel CEFR sin resolución". Verificación: falla por la firma/lógica aún no cambiada
  (hoy solo recibe `cefrLevel`; revisar si también debe recibir `batchSize` para el grupo 5, o si Ajustes se
  lo pasa de otra forma — si no está claro, para y pregunta).
- [x] 3.6 Implementar la simplificación de `RegenerateActiveBatchForLevel` (vacía la tanda activa, compone con
  `sessionCount` sin incrementar). No retires todavía `ForceCloseActiveBatch`: `SettingsViewModel` (en
  `:feature:settings`) sigue inyectándolo hasta la tarea 5.2; retirarlo ahora dejaría `:feature:settings` y
  `:app` sin compilar en este commit. Se retira en 5.3, cuando ya no tiene llamadores. Verificación: tests de
  3.5 en verde; `./gradlew :core:domain:test` en verde (no ejecutes `:feature:settings`/`:app` todavía si
  dependen de `ForceCloseActiveBatch` con la firma antigua, eso se resuelve en el grupo 5).
- [x] 3.7 Test: `AnswerPhrase` pasa `sessionCount` de la tanda activa (no la fecha del reloj) a
  `PracticeBatch.answer()`/`PhraseProgress.record()`. Verificación: falla por el uso de `Clock` aún presente.
- [x] 3.8 Implementar el cambio en `AnswerPhrase`/`PracticeBatch.answer()` (sustituir `today: LocalDate` por
  `currentSession: Int`, leído de `PracticeSnapshot.sessionCount`; retirar la dependencia de `Clock` en
  `AnswerPhrase` si deja de usarse). Verificación: tests de 3.7 en verde; `./gradlew :core:domain:test` en
  verde para todo el módulo.

## 4. Datos (`:core:data`): esquema de Room

- [x] 4.1 Test de integración (`:core:data`): el contador de sesiones generadas persiste y sobrevive al
  reinicio, por defecto 0 si no hay ninguno guardado, según `specs/practice-persistence/spec.md`
  ("Persistencia del contador de sesiones generadas"). Verificación: falla por la columna/tabla aún
  inexistente.
- [x] 4.2 Añadir la columna/tabla del contador de sesiones generadas al esquema de Room; retirar la
  tabla/columna de la cola de arrastradas y la columna de `next_review` (sustituida por `due_at_session`).
  Migración destructiva para las tablas afectadas (design D7: proyecto pre-1.0.0, sin usuarios en
  producción). Verificación: tests de 4.1 en verde; `./gradlew :core:data:test` en verde.
- [x] 4.3 Test: "Posición de la tanda activa" (solo `batch_position`, sin `carry_position`) y "Composición
  atómica de la tanda siguiente"/"Composición única tras vaciar la tanda activa", según
  `specs/practice-persistence/spec.md`. Verificación: falla por el esquema/lógica aún no actualizados donde
  corresponda (puede que ya estén cubiertos por 4.1/4.2; si es así, dilo en vez de duplicar tests).
- [x] 4.4 Ajustar la implementación de persistencia (`RoomPracticeRepository`/data sources) para los cambios
  de 4.3. Verificación: tests de 4.3 en verde.

## 5. Presentación: ajustes (`feature:settings`)

- [x] 5.1 Test: `SettingsViewModel.save()` llama siempre a `RegenerateActiveBatchForLevel` (con el CEFR y N
  recién guardados) cuando algo cambió, sin distinguir N de CEFR, según los escenarios de
  `specs/settings-screen/spec.md` ("Regeneración sin tocar el progreso al cambiar los ajustes").
  Verificación: falla por la distinción `batchSizeChanged`/`cefrLevelChanged` aún presente.
- [x] 5.2 Implementar la unificación en `SettingsViewModel.save()`; retirar la inyección de
  `ForceCloseActiveBatch` de `SettingsViewModel` y de sus fixtures/tests. Verificación: tests de 5.1 en
  verde; `./gradlew :feature:settings:test` en verde.
- [x] 5.3 Retirar la clase `ForceCloseActiveBatch` y sus tests dedicados en `:core:domain`, ahora que no le
  queda ningún llamador tras 5.2. Verificación: `./gradlew :core:domain:test :feature:settings:test
  :app:assembleDebug` en verde.

## 6. Integración (`:app`) y verificación final

- [x] 6.1 Verificar que `:app` compila e inyecta correctamente los casos de uso simplificados
  (`./gradlew assembleDebug`), sin referencias a `ForceCloseActiveBatch`, `CarryQueue` ni
  `ResolveBatchClosure` en ningún módulo. Verificación: `./gradlew assembleDebug` en verde; `grep` del
  repositorio sin resultados para esos tres nombres fuera de este `tasks.md`.
- [x] 6.2 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [x] 6.3 Ejecutar `./gradlew koverVerify` y comprobar los mínimos de cobertura (dominio 90 %, presentación
  80 %, global 70 %).
- [ ] 6.4 Prueba manual en el emulador: cambiar el nivel CEFR a uno con poco catálogo y comprobar que nunca
  aparecen frases de nivel inferior; generar varias sesiones seguidas con un catálogo pequeño y comprobar que
  aparecen frases repetidas (a reforzar) en vez de solo nuevas, y que una frase certificada no reaparece hasta
  pasado su enfriamiento en sesiones.

## Workflow follow-up

- Al archivar, actualizar a mano la sección `## Purpose` de `openspec/specs/spaced-repetition/spec.md` y
  `openspec/specs/practice-batch/spec.md` (hoy dicen "intervalos de repaso en días naturales", "resolución al
  cerrar una tanda" y "la cola de frases arrastradas"), para que reflejen sesiones generadas y resolución al
  instante, sin añadir comportamiento nuevo. Los deltas no gestionan `## Purpose` de una capacidad existente.
- Ejecutar `/opsx:verify` antes de archivar, para comprobar que la implementación cumple los escenarios de
  los deltas de `spaced-repetition`, `practice-batch`, `practice-persistence` y `settings-screen`.
- Tras el `APROBADO` de `android-architect` sobre esta propuesta (incluido
  `docs/adr/0017-repeticion-por-contador-de-sesiones.md`), commitear los artefactos de OpenSpec y el ADR en
  su propio commit `docs(openspec): ...`, antes de `/opsx:apply`.
- Ejecutar `/opsx:archive` en la rama del cambio, antes de la pull request, y commitear el archivo junto con
  las specs sincronizadas.
