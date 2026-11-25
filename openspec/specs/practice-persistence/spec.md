# practice-persistence Specification

## Purpose

Define cómo se guarda y se recupera el estado del usuario entre sesiones: el progreso por frase, la tanda activa con su orden y su capacidad, y el contador de sesiones generadas. Fija la composición de la tanda siguiente como operación atómica y la coherencia de posiciones, según ADR-0012, ADR-0013 y ADR-0017. El comportamiento de la tanda y de la resolución de cada frase se especifica en `practice-batch` y `spaced-repetition`; esta especificación solo cubre su persistencia.

## Requirements

### Requirement: Persistencia del progreso por frase

El sistema MUST persistir una fila por cada frase con progreso o con posición en la tanda activa de
un idioma destino. El progreso de una frase MUST ser específico de su idioma destino: la misma frase
puede tener progreso distinto en dos idiomas destino. Tras reiniciar la app, el progreso recuperado
de cada idioma destino MUST ser igual al último guardado para ese idioma destino.

#### Scenario: Progreso sobrevive al reinicio

- **GIVEN** una frase de nivel 2 con `dueAtSession` igual a 13 guardada tras generar sesión en el
  idioma destino `pt-PT`
- **WHEN** la app se reinicia y se consulta el progreso de `pt-PT`
- **THEN** la frase tiene nivel 2 y `dueAtSession` igual a 13

#### Scenario: El progreso de dos idiomas destino para la misma frase no se mezcla

- **GIVEN** la frase `a1-saludos-001` con nivel 3 en el idioma destino `pt-PT` y nivel 0 en `en-GB`
- **WHEN** se consulta el progreso de esa frase en cada idioma destino
- **THEN** `pt-PT` devuelve nivel 3 y `en-GB` devuelve nivel 0

### Requirement: Posición de la tanda activa

Cada frase de la tanda activa de un idioma destino MUST ocupar una posición única (`batch_position`)
**dentro de ese idioma destino**. Una frase fuera de la tanda activa de su idioma destino MUST NOT
tener posición. La misma frase MUST poder ocupar posiciones distintas, o ninguna, en tandas activas
de idiomas destino distintos.

#### Scenario: Posición repetida se rechaza

- **GIVEN** dos frases distintas del mismo idioma destino con la misma posición de tanda
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

#### Scenario: La misma posición es válida en idiomas destino distintos

- **GIVEN** una frase de `pt-PT` en la posición 0 de su tanda activa
- **WHEN** se guarda una frase de `en-GB` en la posición 0 de su propia tanda activa
- **THEN** el guardado se completa sin error

### Requirement: Tanda activa única

Cada idioma destino MUST tener como máximo una tanda activa. Su existencia MUST indicarse por la
presencia de una única fila de la tabla `active_batch` para ese idioma destino. Su capacidad MUST
estar entre 10 y 30 inclusive. La tanda activa de un idioma destino MUST ser independiente de la
tanda activa de cualquier otro idioma destino.

#### Scenario: Sin tanda activa

- **GIVEN** que la tabla `active_batch` no tiene ninguna fila para el idioma destino `pt-PT`
- **WHEN** se consulta la tanda activa de `pt-PT`
- **THEN** el resultado indica que no hay tanda activa

#### Scenario: Capacidad fuera de rango se rechaza

- **GIVEN** una tanda activa con capacidad 31 para un idioma destino
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

#### Scenario: La tanda activa de un idioma destino no afecta a la de otro

- **GIVEN** una tanda activa guardada para `pt-PT`
- **WHEN** se consulta si hay tanda activa para `en-GB`
- **THEN** el resultado indica que no hay tanda activa para `en-GB`

### Requirement: Orden de la tanda activa

La tanda activa de un idioma destino MUST recuperarse en el mismo orden en que se guardó, según
`batch_position`. Responder una frase MUST cambiar su posición al final de la lista de su idioma
destino, sin afectar al orden de la tanda activa de ningún otro idioma destino.

#### Scenario: Orden tras responder

- **GIVEN** una tanda activa del idioma destino `pt-PT` con las frases A, B y C en ese orden
- **WHEN** se responde A y se guarda la tanda
- **THEN** al recuperarla el orden es B, C, A

### Requirement: Persistencia del contador de sesiones generadas

El sistema MUST persistir un contador de sesiones generadas por idioma destino, en 0 si no hay
ninguno guardado para ese idioma destino. Generar sesión de un idioma destino MUST incrementar su
contador en 1 y persistir el nuevo valor en la misma transacción que la tanda siguiente de ese mismo
idioma destino, sin modificar el contador de ningún otro idioma destino. Recomponer la tanda activa
por cambio de ajustes MUST NOT modificarlo.

#### Scenario: El contador sobrevive al reinicio

- **GIVEN** un contador de sesiones generadas igual a 7 guardado para el idioma destino `pt-PT`
- **WHEN** la app se reinicia y se consulta el contador de `pt-PT`
- **THEN** su valor es 7

#### Scenario: Sin contador guardado el valor por defecto es 0

- **GIVEN** que no hay ningún contador guardado para el idioma destino `en-GB`
- **WHEN** se consulta el contador de sesiones generadas de `en-GB`
- **THEN** su valor es 0

#### Scenario: Generar sesión en un idioma destino no incrementa el contador de otro

- **GIVEN** el contador de sesiones generadas en 7 para `pt-PT` y en 3 para `en-GB`
- **WHEN** se genera sesión para `pt-PT`
- **THEN** el contador de `pt-PT` queda en 8 y el de `en-GB` sigue en 3

### Requirement: Composición atómica de la tanda siguiente

Generar una sesión o recomponer la tanda activa por cambio de ajustes, para un idioma destino, MUST
persistir en una única transacción el progreso, el contador de sesiones (si cambia) y la nueva tanda
activa de ese idioma destino, o bien no persistir nada. Si cualquier paso falla, el estado de ese
idioma destino MUST quedar exactamente como antes del intento, sin afectar al estado de ningún otro
idioma destino.

#### Scenario: Fallo a mitad de la operación no deja estado parcial

- **GIVEN** una tanda activa con tres frases y un fallo de escritura al persistir la tanda siguiente
  del idioma destino `pt-PT`
- **WHEN** se intenta generar sesión para `pt-PT`
- **THEN** la tanda activa, el progreso y el contador de sesiones de `pt-PT` quedan iguales a los de
  antes del intento

#### Scenario: Generar sesión persiste todo a la vez

- **GIVEN** una tanda activa completa de un idioma destino
- **WHEN** se genera sesión para ese idioma destino
- **THEN** el contador de sesiones y la nueva tanda activa de ese idioma destino quedan persistidos
  en la misma operación

### Requirement: Composición única tras vaciar la tanda activa

Tras vaciar la tanda activa de un idioma destino, `ObtainActiveBatch` de ese idioma destino MUST
componer la tanda siguiente una sola vez y dentro de la misma transacción de escritura que la
guarda, solo cuando no hay tanda activa para ese idioma destino. Las llamadas siguientes de ese mismo
idioma destino MUST devolver la tanda ya compuesta sin componer otra.

#### Scenario: Vaciar la tanda activa no compone la tanda siguiente por sí solo

- **GIVEN** una tanda con composición posible para la tanda siguiente de un idioma destino
- **WHEN** se vacía la tanda activa de ese idioma destino sin componer
- **THEN** no hay tanda activa para ese idioma destino y el progreso no contiene frases nuevas
  añadidas por esa operación

#### Scenario: Obtener dos veces tras vaciar compone una sola vez

- **GIVEN** la tanda activa de un idioma destino vaciada, con composición posible para la tanda
  siguiente
- **WHEN** se obtiene la tanda activa de ese idioma destino dos veces seguidas
- **THEN** la primera llamada devuelve `Created` con la tanda compuesta, la segunda devuelve
  `Existing` con la misma tanda, y no se compone otra

### Requirement: Recomposición de la tanda activa al detectar ajustes aplicados distintos

Al obtener la tanda activa de un idioma destino que ya tiene una, el sistema MUST compararla contra
sus ajustes aplicados registrados (`user-settings`) y recomponerla de inmediato si difieren de los
ajustes guardados vigentes, sin tocar progreso ni el contador de sesiones. Esta comparación MUST
repetirse en cada obtención, sobreviva o no la app de por medio. Si no hay ningún ajuste aplicado
registrado todavía, el sistema MUST registrar los ajustes guardados vigentes como sus ajustes
aplicados en esa misma obtención, sin recomponer la tanda existente.

#### Scenario: Ajustes aplicados distintos recomponen sin tocar el progreso

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR A1 y N igual a 20,
  registrados como sus ajustes aplicados, y una frase de esa tanda con racha 2
- **WHEN** los ajustes guardados de ese idioma destino pasan a tener el nivel CEFR B1 y se obtiene su
  tanda activa
- **THEN** la tanda activa se recompone con frases del nivel CEFR B1, sin incrementar el contador de
  sesiones generadas y conservando la racha, el nivel de consolidación y el `dueAtSession` de la frase
  de racha 2 sin cambios

#### Scenario: Ajustes aplicados iguales no recomponen

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR B1 y N igual a 20,
  registrados como sus ajustes aplicados, que siguen siendo los ajustes guardados vigentes de ese
  idioma destino
- **WHEN** se obtiene la tanda activa de ese idioma destino
- **THEN** se devuelve la misma tanda activa, sin recomponerla

#### Scenario: Sin ajustes aplicados registrados todavía no se recompone

- **GIVEN** una tanda activa de un idioma destino sin ningún ajuste aplicado registrado para ese
  idioma destino, con el nivel CEFR B1 y N igual a 15 guardados
- **WHEN** se obtiene la tanda activa de ese idioma destino
- **THEN** se devuelve la tanda activa existente tal cual, sin recomponerla, y los ajustes aplicados
  registrados de ese idioma destino quedan en el nivel CEFR B1 y N igual a 15

#### Scenario: Tras registrarse, un cambio posterior de ajustes ya recompone

- **GIVEN** una tanda activa de un idioma destino sin ajustes aplicados registrados, obtenida una vez
  con el nivel CEFR A1 y N igual a 20 guardados (lo que registra ese nivel y esa N como sus ajustes
  aplicados)
- **WHEN** los ajustes guardados de ese idioma destino cambian al nivel CEFR B1 y se obtiene su tanda
  activa de nuevo
- **THEN** la tanda activa se recompone con frases del nivel CEFR B1

#### Scenario: El cambio sobrevive al reinicio de la app

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR A1, registrado como su
  ajuste aplicado, y un cambio posterior de los ajustes guardados de ese idioma destino al nivel CEFR
  B1 hecho sin volver a obtener su tanda activa
- **WHEN** la app se reinicia y se obtiene la tanda activa de ese idioma destino
- **THEN** la tanda activa se recompone con frases del nivel CEFR B1

### Requirement: Registro de los ajustes aplicados al componer

Toda composición de la tanda activa de un idioma destino (su primer uso, generar sesión, o la
recomposición al detectar ajustes aplicados distintos) MUST registrar sus ajustes guardados vigentes
como los ajustes aplicados de ese idioma destino, en la misma operación.

#### Scenario: El primer uso registra sus ajustes aplicados

- **GIVEN** un idioma destino sin tanda activa ni ajustes aplicados registrados, con el nivel CEFR A1
  y N igual a 20 guardados
- **WHEN** se obtiene la tanda activa de ese idioma destino
- **THEN** los ajustes aplicados registrados de ese idioma destino quedan en el nivel CEFR A1 y N
  igual a 20

#### Scenario: Generar sesión registra sus ajustes aplicados

- **GIVEN** un idioma destino con el nivel CEFR B1 y N igual a 15 guardados
- **WHEN** se genera sesión para ese idioma destino
- **THEN** los ajustes aplicados registrados de ese idioma destino quedan en el nivel CEFR B1 y N
  igual a 15

### Requirement: Lectura coherente del estado

La lectura del estado de un idioma destino MUST devolver un único estado consistente: el progreso,
la tanda activa y el contador de sesiones de ese idioma destino corresponden a la misma versión
persistida.

#### Scenario: Lectura durante una composición

- **GIVEN** una composición de la tanda siguiente en curso para un idioma destino
- **WHEN** se lee el estado de ese idioma destino
- **THEN** la lectura devuelve el estado anterior o el posterior a la composición, nunca una mezcla
  de ambos

### Requirement: Primer uso

Si la base de datos no tiene progreso ni tanda para un idioma destino, `ObtainActiveBatch` de ese
idioma destino MUST componer la primera tanda de ese idioma destino con las frases nuevas del nivel
elegido para ese idioma destino.

#### Scenario: Primer uso crea la primera tanda

- **GIVEN** una base de datos sin progreso ni tanda para el idioma destino `pt-PT`, un catálogo de
  `pt-PT` con quince frases A1 y el nivel elegido A1 con N igual a 20 para `pt-PT`
- **WHEN** se obtiene la tanda activa de `pt-PT`
- **THEN** el resultado es `Created` con las quince frases en orden de catálogo, todas de nivel 0
  (la primera tanda se llena hasta N o hasta agotar las frases del nivel, ADR-0026)

#### Scenario: El primer uso de un idioma destino no crea tanda para otro

- **GIVEN** `pt-PT` con progreso y tanda activa ya existentes, y `en-GB` sin progreso ni tanda
- **WHEN** se obtiene la tanda activa de `en-GB`
- **THEN** se compone la primera tanda de `en-GB` y la tanda activa de `pt-PT` no se modifica

### Requirement: Existe una única instancia de `PracticeDatabase` en el grafo de la aplicación

En el grafo de la aplicación, toda obtención de `PracticeDatabase` MUST devolver la misma instancia.

#### Scenario: Dos obtenciones de la base de datos desde el grafo de producción devuelven la misma instancia

- **GIVEN** la aplicación con su grafo de Hilt de producción (`PersistenceModule`)
- **WHEN** se obtiene `PracticeDatabase` dos veces
- **THEN** ambas referencias son la misma instancia
