# practice-persistence Specification

## Purpose

Define cómo se guarda y se recupera el estado del usuario entre sesiones: el progreso por frase, la tanda activa con su orden y su capacidad, y el contador de sesiones generadas. Fija la composición de la tanda siguiente como operación atómica y la coherencia de posiciones, según ADR-0012, ADR-0013 y ADR-0017. El comportamiento de la tanda y de la resolución de cada frase se especifica en `practice-batch` y `spaced-repetition`; esta especificación solo cubre su persistencia.

## Requirements

### Requirement: Persistencia del progreso por frase

El sistema MUST persistir una fila por cada frase con progreso o con posición en la tanda activa. Tras
reiniciar la app, el progreso recuperado MUST ser igual al último guardado.

#### Scenario: Progreso sobrevive al reinicio

- **GIVEN** una frase de nivel 2 con `dueAtSession` igual a 13 guardada tras generar sesión
- **WHEN** la app se reinicia y se consulta el progreso
- **THEN** la frase tiene nivel 2 y `dueAtSession` igual a 13

### Requirement: Posición de la tanda activa

Cada frase de la tanda activa MUST ocupar una posición única (`batch_position`). Una frase fuera de la tanda
activa MUST NOT tener posición.

#### Scenario: Posición repetida se rechaza

- **GIVEN** dos frases distintas con la misma posición de tanda
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

### Requirement: Tanda activa única

Solo puede existir una tanda activa. Su existencia MUST indicarse por la presencia de una única fila de la tabla `active_batch`. Su capacidad MUST estar entre 10 y 30 inclusive.

#### Scenario: Sin tanda activa

- **GIVEN** que la tabla `active_batch` no tiene filas
- **WHEN** se consulta la tanda activa
- **THEN** el resultado indica que no hay tanda activa

#### Scenario: Capacidad fuera de rango se rechaza

- **GIVEN** una tanda activa con capacidad 31
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

### Requirement: Orden de la tanda activa

La tanda activa MUST recuperarse en el mismo orden en que se guardó, según `batch_position`. Responder una frase MUST cambiar su posición al final de la lista.

#### Scenario: Orden tras responder

- **GIVEN** una tanda activa con las frases A, B y C en ese orden
- **WHEN** se responde A y se guarda la tanda
- **THEN** al recuperarla el orden es B, C, A

### Requirement: Persistencia del contador de sesiones generadas

El sistema MUST persistir un contador de sesiones generadas, en 0 si no hay ninguno guardado. Generar sesión
MUST incrementarlo en 1 y persistir el nuevo valor en la misma transacción que la tanda siguiente. Recomponer
la tanda activa por cambio de ajustes MUST NOT modificarlo.

#### Scenario: El contador sobrevive al reinicio

- **GIVEN** un contador de sesiones generadas igual a 7 guardado
- **WHEN** la app se reinicia y se consulta el contador
- **THEN** su valor es 7

#### Scenario: Sin contador guardado el valor por defecto es 0

- **GIVEN** que no hay ningún contador guardado
- **WHEN** se consulta el contador de sesiones generadas
- **THEN** su valor es 0

### Requirement: Composición atómica de la tanda siguiente

Generar una sesión o recomponer la tanda activa por cambio de ajustes MUST persistir en una única transacción
el progreso, el contador de sesiones (si cambia) y la nueva tanda activa, o bien no persistir nada. Si
cualquier paso falla, el estado MUST quedar exactamente como antes del intento.

#### Scenario: Fallo a mitad de la operación no deja estado parcial

- **GIVEN** una tanda activa con tres frases y un fallo de escritura al persistir la tanda siguiente
- **WHEN** se intenta generar sesión
- **THEN** la tanda activa, el progreso y el contador de sesiones quedan iguales a los de antes del intento

#### Scenario: Generar sesión persiste todo a la vez

- **GIVEN** una tanda activa completa
- **WHEN** se genera sesión
- **THEN** el contador de sesiones y la nueva tanda activa quedan persistidos en la misma operación

### Requirement: Composición única tras vaciar la tanda activa

Tras vaciar la tanda activa, `ObtainActiveBatch` MUST componer la tanda siguiente una sola vez y dentro de la
misma transacción de escritura que la guarda, solo cuando no hay tanda activa. Las llamadas siguientes MUST
devolver la tanda ya compuesta sin componer otra.

#### Scenario: Vaciar la tanda activa no compone la tanda siguiente por sí solo

- **GIVEN** una tanda con composición posible para la tanda siguiente
- **WHEN** se vacía la tanda activa sin componer
- **THEN** no hay tanda activa y el progreso no contiene frases nuevas añadidas por esa operación

#### Scenario: Obtener dos veces tras vaciar compone una sola vez

- **GIVEN** la tanda activa vaciada, con composición posible para la tanda siguiente
- **WHEN** se obtiene la tanda activa dos veces seguidas
- **THEN** la primera llamada devuelve `Created` con la tanda compuesta, la segunda devuelve `Existing` con la
  misma tanda, y no se compone otra

### Requirement: Lectura coherente del estado

La lectura del estado MUST devolver un único estado consistente: el progreso, la tanda activa y el contador de sesiones corresponden a la misma versión persistida.

#### Scenario: Lectura durante una composición

- **GIVEN** una composición de la tanda siguiente en curso
- **WHEN** se lee el estado
- **THEN** la lectura devuelve el estado anterior o el posterior a la composición, nunca una mezcla de ambos

### Requirement: Primer uso

Si la base de datos no tiene progreso ni tanda, `ObtainActiveBatch` MUST componer la primera tanda con las frases nuevas del nivel elegido.

#### Scenario: Primer uso crea la primera tanda

- **GIVEN** una base de datos vacía, un catálogo de quince frases A1 y el nivel elegido A1 con N igual a 20
- **WHEN** se obtiene la tanda activa
- **THEN** el resultado es `Created` con quince frases en orden de catálogo, todas de nivel 0

### Requirement: Existe una única instancia de `PracticeDatabase` en el grafo de la aplicación

En el grafo de la aplicación, toda obtención de `PracticeDatabase` MUST devolver la misma instancia.

#### Scenario: Dos obtenciones de la base de datos desde el grafo de producción devuelven la misma instancia

- **GIVEN** la aplicación con su grafo de Hilt de producción (`PersistenceModule`)
- **WHEN** se obtiene `PracticeDatabase` dos veces
- **THEN** ambas referencias son la misma instancia
