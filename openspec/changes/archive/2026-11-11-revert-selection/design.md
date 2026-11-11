# Design

## Context

Ver `proposal.md` - Why. Estado actual relevante:

- `AnswerPhrase` (`:core:domain`, `practice/AnswerPhrase.kt`) resuelve la autoevaluación dentro de
  `PracticeRepository.transact`: lee el `PracticeSnapshot`, llama a `PracticeBatch.answer` (que
  delega en `PhraseProgress.record`, ADR-0015/ADR-0017) y persiste el snapshot resultante en la
  misma transacción. Solo devuelve el `Level` resultante; no conserva el estado anterior en ningún
  sitio.
- `PracticeBatch.answer` siempre actúa sobre la frase que el usuario tiene delante
  (`session.items.first()` en `ObtainPracticeSession`) y la mueve al final de `entries`. Por tanto,
  la frase recién autoevaluada está siempre en la última posición de la tanda justo después de
  responder, y su posición anterior era siempre la primera.
- `PracticeSessionViewModel.answer()` guarda `levelBefore` como variable local, solo para calcular
  `badgeChange`; se pierde al terminar la función. No hay ningún buffer de "última respuesta".
- No existe ningún contador de errores en `PhraseProgress`/`PhraseProgressEntity`: el único rastro
  de un fallo es la bajada de nivel (ADR-0015) y el reinicio de racha.

## Goals / Non-Goals

**Goals:**
- Permitir deshacer la autoevaluación más reciente de toda la tanda activa, restaurando nivel,
  racha, `dueAtSession` y posición en la lista circular de la frase afectada.
- Que el deshacer sea de un solo uso por autoevaluación, sin pila de deshacer y sin persistencia.

**Non-Goals:**
- Persistir el deshacer entre aperturas de la app o sesiones generadas (proposal.md - Fuera de
  alcance).
- Añadir un contador de errores al dominio: no aporta nada que restaurar nivel/racha/`dueAtSession`
  no cubra ya.
- Cambiar `PracticeRepository`, el esquema de Room o el `PracticeDao`: `transact` ya permite leer y
  escribir el snapshot completo, que es todo lo que hace falta para revertir.

## Decisions

### D1: Un value object de dominio transporta lo necesario para revertir, sin persistirlo

`AnswerPhrase.invoke` pasa a devolver `AnswerOutcome(resultingLevel: Level, undo: UndoableAnswer)`
en vez de solo `Level`. `UndoableAnswer(val phraseId: PhraseId, val progressBefore: PhraseProgress)`
vive en `:core:domain`, junto a `AnswerPhrase`. Es un dato puro, nunca persistido: la UI lo guarda en
memoria (D3) y lo pasa de vuelta al caso de uso que revierte.

Alternativa descartada: añadir una tabla o columna de "última autoevaluación" en Room. Se descarta
porque el deshacer es de un solo uso y no debe sobrevivir ni a cerrar la app ni a generar sesión
(proposal.md): no es progreso de usuario, así que no encaja en ADR-0013 y evita cualquier migración.

### D2: Nuevo caso de uso `RevertLastAnswer` en `:core:domain`, simétrico a `AnswerPhrase`

`RevertLastAnswer(undo: UndoableAnswer)` (inject constructor, depende solo de `PracticeRepository`)
hace `transact` sobre el snapshot: exige que la frase en la última posición de la tanda activa sea
`undo.phraseId` (si no lo es, es un error de programación - alguien intentó revertir algo que ya no
es lo último - y lanza `IllegalArgumentException`, igual que el resto de invariantes de la tanda) y
sustituye esa entrada por `undo.progressBefore` en primera posición, igual que estaba antes de
autoevaluarla (ver Contexto: la frase respondida sale siempre de la primera posición).

Se añade `PracticeBatch.revertLastAnswer(expectedId: PhraseId, progressBefore: PhraseProgress)` para
mantener esa invariante y esa recolocación encapsuladas en `PracticeBatch`, igual que `answer()`.

Alternativa descartada: un único caso de uso `AnswerPhrase` con un parámetro de modo
"answer/revert". Se descarta porque mezclaría dos contratos distintos (uno recibe una autoevaluación,
el otro un `UndoableAnswer` ya resuelto) en la misma clase, y porque ambos casos de uso ya siguen el
patrón de una responsabilidad por clase del resto del dominio (`AnswerPhrase`, `GenerateSession`).

### D3: El buffer de deshacer vive en el ViewModel, no en `PracticeSessionUiState`

`PracticeSessionViewModel` guarda un campo privado `pendingUndo: PendingUndo?` (análogo a
`loadedBatchIds`, que ya sigue este mismo patrón para `Resume`), con
`PendingUndo(val answer: UndoableAnswer, val indexBefore: Int)`: `indexBefore` es el `index` ("X de
Y") que tenía la pantalla justo antes de la autoevaluación, necesario para restaurar el indicador de
posición al revertir, ya que ese dato es de presentación y no vive en el dominio.

`PracticeSessionUiState.Practicing` expone solo `canRevertLastAnswer: Boolean` (si `pendingUndo` no
es `null`), no el propio `UndoableAnswer`: la pantalla solo necesita saber si debe mostrar el botón,
y mantener el dato de dominio fuera del `UiState` evita acoplar la presentación a un tipo que solo
tiene sentido como token de un solo uso.

Igual que el resto del estado en memoria de este ViewModel (design D3 de un cambio previo, comentario
en la clase), `pendingUndo` sobrevive a la rotación pero no a la muerte del proceso: aceptable, porque
es una posibilidad de deshacer efímera, no progreso de usuario.

`pendingUndo` se fija tras cada autoevaluación (reemplazando cualquier valor anterior, nunca se
acumula) y se limpia a `null`: al revertir, al pedir generar sesión, cada vez que una autoevaluación
nueva lo reemplaza, y al reconstruir el `Practicing` por una recarga que no es continuación directa
de una autoevaluación (`reload()` sin `previous`, usado en la carga inicial y en `Resume` cuando la
tanda leída cambió): en ese camino no hay ninguna autoevaluación de la que `pendingUndo` sea
reflejo, así que mantenerlo apuntaría a una frase que puede ya no estar en la última posición de la
tanda nueva.

### D4: El botón se oculta en cuanto se autoevalúa, no solo durante la transición de insignia

El requisito (spec: practice-session, "Botón flotante para revertir la última autoevaluación") dice
que el botón MUST NOT mostrarse mientras *cualquier* autoevaluación esté en curso (`busy`), no solo
durante la transición de insignia. Por eso `answer()` fija `canRevertLastAnswer = false` en el mismo
`copy()` donde ya fija `busy = true`, de forma síncrona, antes de lanzar la corrutina que llama a
`answerPhrase(...)` - igual que ya hace con `busy` (comentario existente: "un segundo toque antes de
terminar se descarta"). Así el botón desaparece de inmediato al autoevaluar la frase siguiente,
aunque el botón visible correspondiera a la autoevaluación anterior (spec: practice-session, "El
botón no aparece mientras se registra una nueva autoevaluación").

`canRevertLastAnswer` solo vuelve a `true` al construir el siguiente `Practicing` ya resuelto (sea
por `reload(previous = state)` directo, sea por `consumeBadgeChange()` tras la transición de
insignia), y solo si `pendingUndo != null` en ese momento. Mientras `badgeChange` esté activo,
`canRevertLastAnswer` sigue en `false` por el mismo motivo: sigue siendo una autoevaluación en curso
desde el punto de vista del usuario, aunque `busy` ya esté fijado a `true` desde antes.

### D5: Revertir reconstruye el `Practicing` a mano, sin pasar por `reload()`, con el mismo guard síncrono que `answer()`

`reload()` asume que siempre se avanza (su `nextIndex()` solo incrementa o vuelve a 1). Revertir va
hacia atrás, así que el manejador de `PracticeSessionAction.RevertLastAnswer` construye el
`Practicing` siguiente directamente a partir de `obtainPracticeSession()` (para leer la tanda ya
revertida) y de `pendingUndo.indexBefore`, con `revealed = true` (la frase vuelve mostrándose
revelada, como estaba justo antes de autoevaluarla) y `canRevertLastAnswer = false`. No se añade un
parámetro nuevo a `reload()` para este caso, para no acoplar su lógica de avance (D1 de un cambio
anterior) con la de retroceso, que solo ocurre aquí.

Revertir comparte con `answer()` el mismo riesgo de doble toque y de carrera con una autoevaluación
nueva: si el usuario pulsa el botón y, casi a la vez, se registra la autoevaluación de la frase
siguiente, las dos corrutinas llamarían a `PracticeRepository.transact` sobre la misma tanda, y
`RevertLastAnswer` podría encontrar que la frase de `pendingUndo` ya no está en la última posición
(invariante de D2) y lanzar `IllegalArgumentException` sin capturar. Por eso el manejador de
`RevertLastAnswer` aplica el mismo guard síncrono que `answer()`: ignora la acción si `state.busy`
es `true`, y si no lo es, fija `busy = true` y `pendingUndo = null` de inmediato, antes de lanzar la
corrutina que llama a `revertLastAnswer(...)`. Un segundo toque sobre el botón, o una autoevaluación
que llegara justo después, encuentran `busy = true` o `pendingUndo = null` y se ignoran, sin que la
corrutina de la reversión llegue a competir con otra transacción (spec: practice-session, "Doble
toque en revertir solo revierte una vez").

### D6: Botón flotante como composable propio de `:feature:session`, no un componente `Ds*` nuevo

El botón es un único `FloatingActionButton` de Material 3 posicionado en la esquina inferior
izquierda de `PracticeSessionScreen`, construido con `AppTheme.spacing`/`AppTheme.motion` y sin
colores, `dp` ni duraciones literales. No se promueve a `:core:designsystem` porque ninguna otra
feature lo necesita todavía (regla del proyecto: se promociona cuando lo necesita una segunda
feature).

## Risks / Trade-offs

- [Revertir después de que algo externo cambie la tanda (p. ej. un ajuste que recompone la tanda
  activa mientras el botón está visible) dejaría `pendingUndo` apuntando a una frase que ya no está
  en la última posición] → Mitigación: `RevertLastAnswer` valida esa invariante y lanza
  `IllegalArgumentException` como error de programación; en la UI, `reload()` sin `previous` (D3) ya
  limpia `pendingUndo` a la vez que se reconstruye el `Practicing` fuera del propio flujo de
  autoevaluación, igual que ya hace al generar sesión.
- [Un doble toque en el botón, o pulsarlo justo cuando se registra la autoevaluación de la frase
  siguiente, lanzarían dos corrutinas que compiten por la misma transacción de `transact` y podrían
  hacer que `RevertLastAnswer` encuentre una tanda ya distinta a la esperada] → Mitigación: el mismo
  guard síncrono que ya usa `answer()` (D5): `revertLast()` ignora la acción si `busy` es `true`, y
  fija `busy = true`/`pendingUndo = null` antes de lanzar su corrutina.
- [Guardar `indexBefore` en el ViewModel añade un segundo lugar (junto al propio dominio) del que
  depende el deshacer] → Mitigación: es un único `Int` de presentación, sin reglas de negocio;
  mantenerlo fuera del dominio es más simple que inventar un concepto de "posición" en
  `PracticeBatch` solo para este caso.
- [No hay contador de errores que mostrar al usuario al revertir, como sugería la petición original]
  → Mitigación: ya registrada como decisión en `proposal.md` - Fuera de alcance; revertir restaura el
  nivel y la racha, que es donde un fallo deja su huella real (ADR-0015/ADR-0017).

## Migration Plan

No aplica: no hay migración de Room ni cambio de esquema. El cambio es aditivo en `:core:domain` (un
caso de uso y un value object nuevos, un cambio de tipo de retorno en `AnswerPhrase`) y en
`:feature:session` (una acción, un campo de estado y un botón nuevos).
