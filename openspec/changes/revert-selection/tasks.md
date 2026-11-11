# Tasks

## 1. Dominio: value object y caso de uso para revertir (`:core:domain`)

- [ ] 1.1 Test: `PracticeBatchTest` - dada una tanda A, B, C donde A se autoevaluó (orden B, C, A),
  `revertLastAnswer(PhraseId("A"), progressBefore)` devuelve la tanda en el orden A, B, C con el
  `progressBefore` de A restaurado (spec: practice-batch, "Revertir un acierto devuelve la frase a
  su posición original"). Ejecutar y comprobar que falla porque `revertLastAnswer` no existe
  todavía.
- [ ] 1.2 Implementar `PracticeBatch.revertLastAnswer(expectedId: PhraseId, progressBefore:
  PhraseProgress): PracticeBatch`: exige `entries.last().id == expectedId` (si no,
  `IllegalArgumentException`, spec: practice-batch "No se puede revertir la posición tras una
  segunda autoevaluación") y antepone `progressBefore` al resto de `entries` sin la última posición.
  Verificar: pasa 1.1 y el test de la invariante.
- [ ] 1.3 Test: añadir a `PracticeBatchTest` el caso en que B se autoevaluó después de A y se pide
  revertir A - debe lanzar `IllegalArgumentException` (mismo escenario que 1.2, cubre también el
  caso simétrico de `spaced-repetition`, "Solo la autoevaluación más reciente..."). Verificar: pasa
  en verde con 1.2 ya implementado.
- [ ] 1.4 Andamiaje sin lógica (no es una excepción a rojo→verde: solo declara los tipos que 1.5
  necesita para compilar): crear `UndoableAnswer(val phraseId: PhraseId, val progressBefore:
  PhraseProgress)` y `AnswerOutcome(val resultingLevel: Level, val undo: UndoableAnswer)` en
  `core/domain/.../practice/AnswerPhrase.kt` (o fichero propio en el mismo paquete).
- [ ] 1.5 Test: actualizar `AnswerPhraseTest` para que cada aserción sobre el nivel devuelto use
  `result.resultingLevel` en vez de `result` (p. ej. `given a hit that certifies the phrase...`,
  `given a miss on a phrase with level 1 or higher...`), y añadir un test nuevo: dada una frase de
  nivel 3 racha 1 `dueAtSession` 5 en sesión 10 que falla, `AnswerPhrase` devuelve en `undo` el
  `phraseId` de la frase y su `progressBefore` con nivel 3, racha 1 y `dueAtSession` 5 (spec:
  spaced-repetition, "Revertir un fallo restaura el nivel, la racha y el vencimiento anteriores").
  Ejecutar y comprobar que el test nuevo falla porque `AnswerPhrase` todavía devuelve solo `Level`.
- [ ] 1.6 Cambiar `AnswerPhrase.invoke` para devolver `AnswerOutcome`, capturando el `PhraseProgress`
  de la frase antes de llamar a `batch.answer(...)`. Verificar: `AnswerPhraseTest` en verde.
- [ ] 1.7 Test: `RevertLastAnswerTest` (archivo nuevo) - dado un `UndoableAnswer` de una frase que
  está en la última posición de la tanda activa, `RevertLastAnswer` restaura su nivel, racha y
  `dueAtSession` y la devuelve a la primera posición (spec: spaced-repetition, "Revertir un acierto
  que certificó restaura el nivel y la racha anteriores"; spec: practice-batch, "Revertir un acierto
  devuelve la frase a su posición original"). Ejecutar y comprobar que falla porque la clase no
  existe.
- [ ] 1.8 Implementar `RevertLastAnswer` (`@Inject constructor(private val practiceRepository:
  PracticeRepository)`, `suspend operator fun invoke(undo: UndoableAnswer)`): `transact` sobre el
  snapshot, llama a `batch.revertLastAnswer(undo.phraseId, undo.progressBefore)` y persiste con
  `snapshot.storing(...)`. Verificar: 1.7 en verde.
- [ ] 1.9 Test: añadir a `RevertLastAnswerTest` el caso en que la frase a revertir ya no está en la
  última posición (porque se autoevaluó otra después) - debe lanzar `IllegalArgumentException`
  (spec: spaced-repetition, "La reversión no se acumula"). Verificar: pasa en verde con 1.8 ya
  implementado.

## 2. Presentación: acción, estado y lógica del ViewModel (`:feature:session`)

- [ ] 2.1 Test: en `PracticeSessionViewModelTest`, dada una tanda A, B con A revelada, al autoevaluar
  A con acierto y avanzar a B, `uiState` expone `canRevertLastAnswer = true` (spec: practice-session,
  "El botón aparece tras autoevaluar"). Ejecutar y comprobar que falla porque `Practicing` no tiene
  ese campo todavía.
- [ ] 2.2 Añadir `canRevertLastAnswer: Boolean` a `PracticeSessionUiState.Practicing` (por defecto
  `false`) y `RevertLastAnswer : PracticeSessionAction` (data object) en `PracticeSessionAction`.
  Añadir el campo privado `pendingUndo: PendingUndo?` al ViewModel (`PendingUndo(val answer:
  UndoableAnswer, val indexBefore: Int)`, design D3) y la dependencia inyectada `RevertLastAnswer`.
  En `answer()`, capturar `undo` del `AnswerOutcome` devuelto por `answerPhrase(...)` junto con
  `indexBefore = state.index` antes de autoevaluar, y fijar `canRevertLastAnswer = pendingUndo !=
  null` al construir el `Practicing` siguiente (tanto en `reload(previous = state)` como en
  `consumeBadgeChange()`); en `reload()` sin `previous`, limpiar también `pendingUndo = null` (design
  D3, riesgo de `pendingUndo` obsoleto tras una recarga que no continúa una autoevaluación).
  Verificar: 2.1 en verde.
- [ ] 2.3 Test: dado un acierto que certifica y dispara `badgeChange`, mientras la transición está en
  curso (antes de `ConsumeBadgeChange`) `canRevertLastAnswer` es `false`; al consumirla, pasa a
  `true` (spec: practice-session, "El botón no aparece durante la transición de cambio de
  insignia"). Ejecutar y comprobar que falla si 2.2 no distingue ambos momentos.
- [ ] 2.4 Ajustar `answer()` para que, al fijar `badgeChange`, `canRevertLastAnswer` quede en `false`
  explícitamente (no heredado de `copy()`). Verificar: 2.3 en verde.
- [ ] 2.5 Test: dada la pantalla recién abierta sin ninguna autoevaluación todavía, `uiState` expone
  `canRevertLastAnswer = false` (spec: practice-session, "El botón no aparece antes de la primera
  autoevaluación"). Verificar junto con 2.2 (ya debería pasar; si no, ajustar `reload()` sin
  `previous` para fijar siempre `false`).
- [ ] 2.6 Test: dada una tanda A, B, C con A ya autoevaluada (pantalla en B, `canRevertLastAnswer =
  true`, apuntando al deshacer de A), al autoevaluar B, `canRevertLastAnswer` pasa a `false` de
  inmediato (de forma síncrona, antes de que termine la corrutina de esa segunda autoevaluación), no
  solo cuando la pantalla termine de avanzar a C (spec: practice-session, "El botón no aparece
  mientras se registra una nueva autoevaluación"). Ejecutar y comprobar que falla porque el `copy()`
  que fija `busy = true` en `answer()` todavía conserva el `canRevertLastAnswer` anterior.
- [ ] 2.7 Ajustar `answer()` para que el primer `copy()` síncrono (el que ya fija `busy = true` antes
  de lanzar la corrutina) fije también `canRevertLastAnswer = false`, simétrico a como ya se trata en
  2.4 para el camino de `badgeChange`. Verificar: 2.6 en verde.
- [ ] 2.8 Test: dada una tanda A, B, C con A revelada, autoevaluada con acierto (pantalla en B,
  `canRevertLastAnswer = true`), al enviar `RevertLastAnswer`, `uiState` vuelve a mostrar A revelada,
  en la posición `indexBefore`, con su racha e insignia anteriores, y `canRevertLastAnswer = false`
  (spec: practice-session, "Revertir un acierto vuelve a mostrar la frase anterior"). Ejecutar y
  comprobar que falla porque `onAction` no maneja `RevertLastAnswer` todavía.
- [ ] 2.9 Test: con el botón de revertir visible, enviar `RevertLastAnswer` dos veces seguidas solo
  revierte una vez (spec: practice-session, "Doble toque en revertir solo revierte una vez"); y, por
  separado, enviar `RevertLastAnswer` mientras `busy` ya es `true` por otra operación en curso (una
  autoevaluación o una reversión previa sin terminar) no tiene ningún efecto ni lanza una excepción
  (cubre el riesgo de carrera de design D5: dos corrutinas compitiendo por la misma transacción de
  `PracticeRepository.transact`). Ejecutar y comprobar que ambos casos fallan porque `revertLast()`
  no tiene ningún guard de `busy` todavía.
- [ ] 2.10 Implementar el manejo de `RevertLastAnswer` en `onAction`/un método privado
  `revertLast()`: si `state.busy` es `true` o `pendingUndo` es `null`, ignorar sin lanzar nada; si
  no, fijar `busy = true` de forma síncrona y capturar/limpiar `pendingUndo` a `null` antes de
  lanzar la corrutina (mismo guard que ya usa `answer()`, design D5), llamar a
  `revertLastAnswer(undo.answer)`, leer de nuevo con `obtainPracticeSession()` y construir el
  `Practicing` final con `revealed = true`, `busy = false`, `index = undo.indexBefore`,
  `canRevertLastAnswer = false`, `badgeChange = null`. Verificar: 2.8 y 2.9 en verde.
- [ ] 2.11 Test: tras revertir, autoevaluar A de nuevo y avanzar vuelve a exponer
  `canRevertLastAnswer = true` (spec: practice-session, "Autoevaluar de nuevo tras revertir vuelve a
  ofrecer el botón"). Verificar: ya debería pasar con 2.2/2.10; si no, ajustar dónde se fija
  `pendingUndo`.
- [ ] 2.12 Test: tras la última autoevaluación de la tanda (mensaje de tanda completada con
  `canRevertLastAnswer = true`), al enviar `GenerateSession`, la tanda siguiente se expone con
  `canRevertLastAnswer = false` (spec: practice-session, "Generar sesión hace desaparecer el
  botón"). Ejecutar y comprobar que falla si `requestGenerateSession()` no limpia `pendingUndo`.
- [ ] 2.13 Limpiar `pendingUndo = null` al inicio de `requestGenerateSession()`. Verificar: 2.12 en
  verde.
- [ ] 2.14 Test: dado `pendingUndo` activo tras autoevaluar A (botón visible apuntando a A), cuando
  `Resume` detecta que la tanda activa cambió (p. ej. por un ajuste que la recompuso) y `reload()`
  sin `previous` reconstruye el `Practicing`, enviar `RevertLastAnswer` después no tiene ningún
  efecto ni lanza una excepción (design D3, limpieza de `pendingUndo` fuera del flujo de
  autoevaluación). Ejecutar y comprobar que falla si 2.2 no limpia `pendingUndo` en ese camino.
  Verificar: ya debería pasar si 2.2 ya incluyó esa limpieza; si no, añadir
  `pendingUndo = null` en `reload()` sin `previous`.

## 3. UI: botón flotante en la pantalla de práctica (`:feature:session`)

- [ ] 3.1 Test instrumentado en `PracticeSessionScreenTest`: con `canRevertLastAnswer = true` en el
  `Practicing` de la previsualización/fake, el botón de revertir es visible y pulsable en la esquina
  inferior izquierda; con `canRevertLastAnswer = false`, no existe en el árbol semántico (spec:
  practice-session, escenarios de aparición y desaparición del botón). Nombrar con el patrón
  `givenX_whenY_thenZ` (ADR-0007). Ejecutar y comprobar que falla porque el botón no existe en
  `PracticeSessionScreen`.
- [ ] 3.2 Añadir el `FloatingActionButton` de Material 3 en `PracticeSessionScreen`, posicionado en
  la esquina inferior izquierda con `AppTheme.spacing`, visible solo cuando
  `uiState.canRevertLastAnswer` es `true`, que emite `PracticeSessionAction.RevertLastAnswer` al
  pulsarlo. Verificar: 3.1 en verde.
- [ ] 3.3 Test instrumentado: pulsar el botón dos veces seguidas solo envía una reversión efectiva en
  el ViewModel (reutiliza la cobertura de doble toque de la tarea 2.9 a nivel de UI). Verificar junto
  con 3.2.
- [ ] 3.4 Actualizar `PracticeSessionScreenPreviews.kt` para incluir una previsualización con el
  botón visible (`canRevertLastAnswer = true`), siguiendo el patrón `@PreviewLightDark` existente.

## 4. Verificación final

- [ ] 4.1 Ejecutar `./gradlew test`, `./gradlew detekt` y `./gradlew koverVerify` en verde.
- [ ] 4.2 Ejecutar `./gradlew check` en verde antes de dar la tarea por terminada.

## Workflow follow-up

- Ejecutar `/opsx:verify` para comprobar la implementación contra las specs de este cambio.
- Ejecutar `/opsx:archive` en la rama del cambio, antes de la pull request.
