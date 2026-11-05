# Tasks

## 1. Dominio: bajada instantánea de nivel en `PhraseProgress.record()`

- [x] 1.1 Rojo: en `PhraseProgressTest`, añadir los casos de "Bajada instantánea de nivel ante un fallo"
      (fallo en nivel 3 con repaso 20-03-2026 → nivel 2 con repaso 13-03-2026 al fallar el 10-03-2026;
      fallo en nivel 1 → nivel 0 sin repaso; fallo en nivel 0 no cambia nivel ni repaso; un acierto no
      cambia nivel ni repaso; dos fallos encadenados en nivel 4 con repaso 20-03-2026, ambos el
      10-03-2026, dejan la frase en nivel 2 con repaso 13-03-2026, mediante dos llamadas sucesivas a
      `record(MISS, today)`) llamando a `record(assessment, today)`. Deben fallar porque `record()`
      todavía no acepta `today` ni aplica la bajada.
- [x] 1.2 Verde: cambiar `PhraseProgress.record()` a `record(assessment: SelfAssessment, today: LocalDate)`
      e implementar D3 (diseño): en `MISS` con `level.value >= 1`, bajar a `Level(level.value - 1)` y
      recalcular `nextReview` con `ReviewSchedule.nextReviewAfter(newLevel, today)`; en `MISS` con nivel 0
      o en `HIT`, sin cambios de nivel/repaso. Ejecutar `PhraseProgressTest` en verde.

## 2. Dominio: propagar `today` a `PracticeBatch.answer()` y `RecordSelfAssessment`

- [x] 2.1 Rojo: en `PracticeBatchTest`, pasar `today` explícito a las llamadas existentes a `answer()` y
      añadir un caso donde un fallo en una frase de nivel ≥ 1 dentro de la tanda baja su nivel (verifica
      que `answer()` propaga `today` a `PhraseProgress.record()`). En `RecordSelfAssessmentTest`, pasar
      `today` a las llamadas existentes y añadir el caso equivalente de bajada de nivel. Deben fallar por
      falta del parámetro `today` en ambas firmas.
- [x] 2.2 Verde: cambiar `PracticeBatch.answer(id, assessment, today: LocalDate)` para pasar `today` a
      `record()`; cambiar `RecordSelfAssessment` para inyectar `Clock` y llamar a
      `progress.record(assessment, clock.today())`. Ejecutar `PracticeBatchTest` y
      `RecordSelfAssessmentTest` en verde.

## 3. Caso de uso real: `AnswerPhrase` inyecta `Clock`

- [x] 3.1 Rojo: en `AnswerPhraseTest`, construir `AnswerPhrase` con un `FixedClock` (`:core:testing`) y
      añadir un test que, tras un fallo en una frase de nivel ≥ 1, compruebe a través del repositorio
      fake que su nivel bajó y su próximo repaso se recalculó con la fecha del `FixedClock`. Debe fallar
      porque `AnswerPhrase` no inyecta `Clock` ni pasa fecha a `batch.answer()`.
- [x] 3.2 Verde: `AnswerPhrase` inyecta `Clock` y llama a `batch.answer(id, assessment, clock.today())`
      dentro de `practiceRepository.transact`. Ejecutar `AnswerPhraseTest` en verde (incluidos los tests
      ya existentes, ahora con `FixedClock`).
- [x] 3.3 (Alcance añadido tras detectarse en 3.2, confirmado por el usuario): adaptar los dos sitios de
      `:feature:session` que construyen `AnswerPhrase(practice)` manualmente y dejaron de compilar al
      exigir `Clock` en su constructor: `feature/session/src/test/kotlin/com/dalmuina/fraseafrase/feature/session/PracticeSessionFixtures.kt:97`
      y `feature/session/src/androidTest/kotlin/com/dalmuina/fraseafrase/feature/session/PracticeSessionRouteTest.kt:172`.
      En ambos sitios ya hay un `clock` en scope (usado para `ResolveBatchClosure(clock)`); pasar ese
      mismo `clock` a `AnswerPhrase(practice, clock)`. Cambio mecánico sin lógica nueva: verificar
      ejecutando `./gradlew :feature:session:testDebugUnitTest` y comprobando que `PracticeSessionFixtures`
      y los tests que la usan compilan y pasan en verde (el test instrumentado de `PracticeSessionRouteTest`
      se verifica por compilación, sin emulador en esta sesión).

## 4. Cierre de tanda: `ResolveBatchClosure` ya no fuerza nivel 0

- [x] 4.1 Rojo: en `ResolveBatchClosureTest`, reescribir los tres escenarios de "respondida no
      certificada" (spec: Resolución de frase respondida no certificada al cerrar la tanda) para partir
      de un `PhraseProgress` que ya bajó de nivel durante la práctica (resultado de un `record()` previo
      con fallo) y esperar que el cierre conserve ese nivel y ese próximo repaso, en vez de forzar nivel 0.
      Deben fallar con el `carried()` actual.
- [x] 4.2 Verde: simplificar `ResolveBatchClosure.carried()` a
      `progress.copy(streak = Streak(0), answeredInBatch = false)` (D4), eliminando la rama que forzaba
      nivel 0. Ejecutar `ResolveBatchClosureTest` en verde.

## 5. Verificación final

- [x] 5.1 Ejecutar `./gradlew :core:domain:test` y verificar que todos los tests del dominio, incluidos
      los de los grupos 1 a 4, pasan en verde.
- [x] 5.2 Ejecutar `./gradlew check` y verificar que tests, ktlint y detekt pasan sin errores.
- [x] 5.3 Ejecutar `./gradlew koverVerify` y verificar que se mantiene la cobertura mínima de dominio
      (90 %).

## Workflow follow-up

- Revisión de `android-architect` sobre esta propuesta y su diseño antes de `/opsx:apply`.
- Revisión de `code-reviewer` sobre el código implementado antes de `/opsx:verify`.
- `/opsx:verify` para comprobar el cambio contra sus especificaciones.
- `/opsx:archive` en la rama del cambio, antes de abrir la pull request a `develop`.
