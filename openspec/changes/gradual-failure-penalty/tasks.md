# Tasks

Escenarios: `specs/<capacidad>/spec.md` de este cambio. Decisiones: `design.md` (D1–D3). ADR-0024 ya está
redactado en `docs/adr/0024-un-fallo-resta-un-acierto.md` (Propuesto).

## 1. Regla de fallo en el dominio (`:core:domain`, más un test de `:feature:session`)

> Nota: `PracticeSessionViewModelTest` usa el dominio real con `FakePracticeRepository`, así que el cambio de 1.4
> pone en rojo `given a level 2 phrase with streak 2 when answering with a miss then it shows 3 remaining hits for
> Plata`. El hook ejecuta `./gradlew check` global al commitear, por eso 1.6 adapta ese test en este mismo grupo
> (design D3). Solo cambian tests en `:feature:session`, no código de producción.

- [x] 1.1 Test: `StreakTest` - `given a streak of 2 when a miss is recorded then the streak becomes 1` (sustituye
  al de "→ 0"; `spaced-repetition` "Fallo resta uno a la racha"), `given a streak of 1 when a miss is recorded then
  the streak becomes 0` ("Fallo reinicia la racha") y `given a streak of 0 when a miss is recorded then the streak
  stays 0` (suelo en 0; "Fallo en nivel 0 no cambia el nivel pero sí fija la sesión siguiente"). Debe fallar por la
  aserción del primero.
- [x] 1.2 Implementación: `Streak.afterMiss()` = `Streak(maxOf(value - 1, 0))` y su KDoc (design D1).
  Verificación: 1.1 en verde.
- [x] 1.3 Test: `PhraseProgressTest` - un test por escenario de fallo de `spaced-repetition` "Resolución de la
  autoevaluación": racha 0 en nivel 3 → nivel 2 racha 2; racha 0 en nivel 1 → nivel 0 racha 2; racha 0 en nivel 5
  → nivel 4 racha 2; nivel 3 racha 2 → nivel 3 racha 1; nivel 0 racha 0 → sin cambios; nivel 0 racha 2 → racha 1;
  todos con `dueAtSession` 11 en la sesión 10. Además, de "Racha persistente entre sesiones": nivel 2 racha 2 →
  nivel 2 racha 1 ("Fallo resta uno a la racha") y nivel 2 racha 1 → nivel 2 racha 0 ("Fallo reinicia la racha").
  En total, 8 tests. Los tres tests de fallo actuales se renombran y fijan la racha de
  partida. Debe fallar por la racha resultante tras bajar de nivel y por el nivel con racha > 0.
- [x] 1.4 Implementación: `PhraseProgress.recordMiss` según design D1 (racha `CERTIFICATION_STREAK - 1` al bajar
  de nivel) y KDoc de `record` con la regla de ADR-0024. Verificación: 1.3 en verde.
- [x] 1.5 Tests existentes de `:core:domain` que fijan el resultado de un fallo: `AnswerPhraseTest` (líneas de los
  tests de miss), `RevertLastAnswerTest`, `PracticeBatchTest` (`...misses it then its level drops to 1`). Los que
  parten de racha 0 siguen valiendo: solo se actualiza el comentario de escenario/ADR; los que parten de racha > 0
  se ajustan al resultado nuevo. `AnswerPhraseTest` `...undo exposes the progress before the miss` cubre
  `spaced-repetition` "Revertir un fallo restaura el nivel, la racha y el vencimiento anteriores" con nivel 3
  racha 0 → nivel 2 racha 2. Verificación: `./gradlew :core:domain:check`.
- [x] 1.6 `PracticeSessionViewModelTest`: sustituir el test del contador tras un fallo por
  `given a level 2 phrase with streak 2 when answering with a miss then it shows 2 remaining hits for Oro` y
  añadir `given a level 2 phrase with streak 1 when answering with a miss then it shows 3 remaining hits for Oro` y
  `given a level 2 phrase with streak 0 when answering with a miss then it shows 1 remaining hit for Plata`
  (`practice-session`: "Fallo con racha mayor que 0 suma un acierto al contador", "Fallo reinicia el contador",
  "Fallo con racha 0 deja la frase a un acierto del nivel que tenía"). Si se reutiliza el test actual (líneas
  ~448-486), quitar el `ConsumeBadgeChange` tras el fallo cuando el nivel no cambia y su comentario ADR-0015.
  Actualizar a ADR-0024 los comentarios de los tests de fallo en nivel 3 y nivel 0. Adaptar también el
  instrumentado `PracticeSessionScreenTest` (`givenPhraseWithLevel2WhenMissedAndShownAgainThenTheCounterShowsThreeForSilver`,
  ~líneas 529-560) → `givenPhraseWithLevel2AndStreak1WhenMissedAndShownAgainThenTheCounterShowsThreeForGold`:
  estado de partida 2 aciertos para "Oro", tras el fallo 3 para "Oro", comentario ADR-0024. Verificación:
  `./gradlew :feature:session:check` y el instrumentado filtrado por clase
  (`-Pandroid.testInstrumentationRunnerArguments.class=com.dalmuina.fraseafrase.feature.session.PracticeSessionScreenTest`);
  si no hay dispositivo disponible, decirlo en la tarea.

## 2. Insignia e indicador con fallo sin bajada de nivel (`:feature:session`)

> Nota: la regla vive en el dominio (grupo 1) y el ViewModel ya deriva `badgeChange` e insignia del nivel
> resultante, así que estos tests cubren escenarios nuevos de la spec y pasan en verde al escribirlos. Para
> comprobar que detectan el fallo, ejecutarlos una vez con `recordMiss` en su versión anterior (mutación) y
> anotar el resultado en esta tarea.

- [x] 2.1 Test: `PracticeSessionViewModelTest` - `given a level 3 phrase with streak 2 when answering with a miss
  then badgeChange is null` (`practice-session`: "Un fallo con racha mayor que 0 no muestra el indicador") y
  `given a level 3 phrase with streak 1 when answering with a miss then the badge stays Oro` ("Un fallo con racha
  mayor que 0 mantiene la insignia"). Verificación: `./gradlew :feature:session:check` y evidencia de mutación. Evidencia: con `recordMiss` mutado a
  "todo fallo baja el nivel" (comportamiento previo a ADR-0024), los 2 tests fallan; revertida la mutación, pasan.
  Tests en `PracticeSessionFailureBadgeTest` (nuevo fichero, por el límite de LargeClass de `PracticeSessionViewModelTest`).

## 3. Cierre y verificación (sesión principal)

- [x] 3.1 Líneas de estado (ADR-0001): ADR-0024 → `Aceptado`; ADR-0017 → `Sustituido por ADR-0018 (alcance del
  MVP) y ADR-0024 (regla de fallo)`; añadir 0024 (`Aceptado`) a la tabla de `docs/adr/README.md` y actualizar la
  fila de 0017.
- [x] 3.2 `./gradlew check` y `./gradlew koverVerify` en verde (tests, detekt/ktlint, lint y cobertura: dominio
  90 %, presentación 80 %, global 70 %).
- [ ] 3.3 Instalar la app de depuración y comprobar a mano: una frase en Plata con racha 0 que se falla pasa a
  Bronce con "1 para Plata"; una frase con racha 2 que se falla mantiene su insignia y muestra un acierto más
  pendiente.

## Workflow follow-up

- `code-reviewer` (hasta dos rondas), `/opsx:verify`, `/opsx:archive` y commit de archive (abre la PR por hook).
