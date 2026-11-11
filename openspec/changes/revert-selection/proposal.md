# Proposal

## Why

Con la resolución al instante (ADR-0017), una autoevaluación equivocada por un toque accidental
(acierto en vez de fallo, o viceversa) queda fijada de inmediato: racha, nivel de consolidación y
`dueAtSession` cambian sin ninguna forma de corregirlo. Hoy la única opción es seguir practicando y
esperar a que el modelo de repetición espaciada lo compense con el tiempo, lo que puede tardar varias
sesiones y no corrige el dato ya guardado. El usuario pide poder revertir esa última autoevaluación,
sin interrumpir el flujo con un diálogo de confirmación en cada frase certificada.

## What Changes

- Nueva acción de UI: tras autoevaluar una frase y avanzar a la siguiente, la pantalla de práctica
  muestra un botón flotante en la esquina inferior izquierda que permite deshacer esa autoevaluación.
- Al pulsarlo: la frase vuelve a mostrarse como la frase activa (revelada, en el mismo punto en el que
  estaba antes de autoevaluarla), y su racha, nivel de consolidación y `dueAtSession` quedan como antes
  de esa autoevaluación. Si la autoevaluación revertida fue un fallo, la bajada de nivel que ese fallo
  provocó (ADR-0015, heredada por ADR-0017) también se deshace.
- Tras revertir, el botón deja de mostrarse: solo se puede deshacer la autoevaluación más reciente, y
  solo una vez. Volver a autoevaluar (la misma frase u otra) hace disponible un nuevo deshacer para esa
  nueva autoevaluación, reemplazando la posibilidad de deshacer la anterior.
- El deshacer no persiste entre aperturas de la app ni sobrevive a generar una sesión nueva: es una
  posibilidad de un solo uso, ligada a la pantalla de práctica abierta en ese momento.
- No hay diálogo de confirmación en ningún punto del flujo (ni al autoevaluar, ni al revertir, ni al
  certificar): el botón flotante es la única superficie de esta funcionalidad.

**Fuera de alcance**

- Deshacer más de una autoevaluación hacia atrás (pila de deshacer). Solo existe un "último paso".
- Deshacer una autoevaluación después de generar una sesión nueva o de cerrar y reabrir la app.
- Cualquier historial o registro persistido de autoevaluaciones revertidas.
- Un contador de errores por frase: el dominio no tiene hoy ese dato y este cambio no lo introduce;
  "revertir incluyendo los errores" se resuelve restaurando el nivel y la racha previos, que es donde
  un fallo deja su huella (ADR-0015/ADR-0017), sin añadir un campo nuevo solo para contarlos.

Este cambio no contradice ningún ADR vigente: usa el mismo modelo de resolución al instante de
ADR-0017/ADR-0018 y solo añade la posibilidad de deshacer la última resolución. No se propone ningún
ADR nuevo.

## Capabilities

### Modified Capabilities

- `spaced-repetition`: nuevo requisito que define cómo se restaura el estado de repetición espaciada
  (nivel, racha, `dueAtSession`) de una frase al revertir su última autoevaluación, y que solo la
  autoevaluación más reciente de toda la tanda puede revertirse.
- `practice-batch`: nuevo requisito que define cómo se restaura la posición de la frase en la lista
  circular de la tanda al revertir, ya que autoevaluar la movió al final de la lista.
- `practice-session`: nuevos requisitos sobre el botón flotante de deshacer (cuándo aparece, qué pasa
  al pulsarlo, cuándo deja de mostrarse) en la pantalla de práctica.

## Impact

- `:core:domain`: nuevo caso de uso para revertir la última autoevaluación (opera sobre
  `PracticeRepository`, en una transacción, igual que `AnswerPhrase`); `AnswerPhrase` pasa a devolver
  también la información necesaria para poder revertir esa autoevaluación en memoria, sin persistirla.
- `:core:data`: ninguna migración de Room ni columna nueva; la implementación de `PracticeRepository`
  ya permite leer y escribir el snapshot completo de la tanda, que es lo que hace falta para restaurar
  nivel, racha, `dueAtSession` y posición de una frase.
- `:feature:session`: nueva acción en `PracticeSessionAction`, nuevo estado en `PracticeSessionUiState`
  para controlar la visibilidad del botón, y el botón flotante (componente `Ds*` de
  `:core:designsystem`) en `PracticeSessionScreen`.
- Tests: casos de uso nuevos en `:core:domain`, ViewModel test ampliado en `:feature:session`, test de
  Compose UI instrumentado para el botón flotante.
