# Spec Delta

## Purpose

Define cómo se guarda y se recupera el estado del usuario entre sesiones: el progreso por frase, la tanda activa con su orden y su capacidad, y la cola de arrastradas. Fija el cierre de tanda como operación atómica y la coherencia de posiciones, según ADR-0012 y ADR-0013. El comportamiento de la tanda y del cierre se especifica en `practice-batch` y `spaced-repetition`; esta especificación solo cubre su persistencia.

## ADDED Requirements

### Requirement: Persistencia del progreso por frase

El sistema MUST persistir una fila por cada frase con progreso o con posición en la tanda activa o en la cola de arrastradas. Tras reiniciar la app, el progreso recuperado MUST ser igual al último guardado.

#### Scenario: Progreso sobrevive al reinicio

- **GIVEN** una frase de nivel 2 con próximo repaso el 13-03-2026 guardada tras un cierre de tanda
- **WHEN** la app se reinicia y se consulta el progreso
- **THEN** la frase tiene nivel 2 y próximo repaso el 13-03-2026

### Requirement: Posiciones de la tanda y de la cola

Cada frase MUST ocupar como máximo una posición: en la tanda activa (`batch_position`) o en la cola de arrastradas (`carry_position`). Ninguna frase MUST tener ambas posiciones a la vez. Las posiciones de cada lista MUST ser únicas.

#### Scenario: Frase con ambas posiciones se rechaza

- **GIVEN** una frase con posición de tanda y posición de cola a la vez
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

#### Scenario: Posición repetida se rechaza

- **GIVEN** dos frases distintas con la misma posición de tanda
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

### Requirement: Frase de nivel 0 ubicada

Una frase de nivel 0 MUST estar en la tanda activa o en la cola de arrastradas. Una frase de nivel 1 a 5 puede estar en la tanda activa, en la cola (si quedó sin responder y se arrastró con su nivel) o en ninguna de las dos (programada hasta su próximo repaso).

#### Scenario: Frase de nivel 0 fuera de tanda y cola se rechaza

- **GIVEN** una frase de nivel 0 sin posición de tanda ni de cola
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

#### Scenario: Frase nueva de la tanda activa es válida

- **GIVEN** una frase nueva compuesta en la tanda activa con nivel 0, racha 0 y próximo repaso nulo
- **WHEN** se guarda la tanda
- **THEN** el guardado se acepta y la frase sigue en la tanda activa

#### Scenario: Frase de nivel 3 arrastrada sin responder es válida

- **GIVEN** una frase de nivel 3 con próximo repaso el 20-03-2026 que no se respondió y quedó en la cola de arrastradas
- **WHEN** se guarda el estado
- **THEN** el guardado se acepta y la frase conserva nivel 3 y su próximo repaso

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

### Requirement: Cierre atómico de la tanda

El cierre de la tanda MUST persistir en una única transacción los resultados de cada frase, la nueva cola de arrastradas y la nueva tanda activa, o bien no persistir nada. Si cualquier paso falla, el estado MUST quedar exactamente como antes del cierre.

#### Scenario: Fallo a mitad del cierre no deja estado parcial

- **GIVEN** una tanda activa con tres frases y un fallo de escritura al persistir la nueva tanda
- **WHEN** se intenta cerrar la tanda
- **THEN** la tanda activa, el progreso y la cola quedan iguales a los de antes del cierre

#### Scenario: Cierre correcto persiste todo a la vez

- **GIVEN** una tanda activa con dos certificadas y una arrastrada
- **WHEN** se cierra la tanda
- **THEN** el progreso, la cola y la tanda nueva quedan persistidos en la misma operación

### Requirement: Cierre con expectativa de aviso

Cerrar la tanda MUST recibir el número de frases no certificadas que el usuario confirmó. Si el número real de la tanda activa en el momento del cierre es distinto, el cierre MUST NOT ejecutarse y MUST devolver `Stale`. Si no hay tanda activa, MUST devolver `NoActiveBatch`.

#### Scenario: Cambio entre el aviso y el cierre

- **GIVEN** un aviso de dos frases no certificadas y una tanda que, tras el aviso, tiene tres frases no certificadas
- **WHEN** se solicita el cierre con el número del aviso
- **THEN** el resultado es `Stale` y no se modifica el estado

#### Scenario: Cierre sin tanda activa

- **GIVEN** que no hay tanda activa
- **WHEN** se solicita el cierre
- **THEN** el resultado es `NoActiveBatch`

#### Scenario: Cierre con el número confirmado

- **GIVEN** un aviso de dos frases no certificadas y una tanda con exactamente dos, con la cola sobrante vacía
- **WHEN** se solicita el cierre con el número del aviso el 10-03-2026
- **THEN** el resultado es `Closed`, la tanda activa queda vacía y la cola contiene las dos frases no certificadas en orden de tanda

#### Scenario: Sin tanda activa tiene prioridad sobre la expectativa

- **GIVEN** que no hay tanda activa
- **WHEN** se solicita el cierre con expectativa 2
- **THEN** el resultado es `NoActiveBatch` y no se modifica el estado

#### Scenario: Cierre con reasignación cruzada de posiciones

- **GIVEN** una tanda con la certificada C en la posición 0 y una arrastrada X en la cola en la posición 0
- **WHEN** se cierra la tanda y X entra en la posición 0 de la nueva tanda
- **THEN** el cierre se persiste sin colisión de posiciones y el resultado es `Closed`

#### Scenario: Cierre sin composición posible deja la tanda sin activar

- **GIVEN** una tanda con dos frases certificadas, cola sobrante vacía y sin pendientes ni frases nuevas disponibles
- **WHEN** se cierra la tanda con expectativa 0 el 10-03-2026
- **THEN** el resultado es `Closed`, no hay tanda activa y la cola queda vacía

### Requirement: Primer uso

Si la base de datos no tiene progreso ni tanda, `ObtainActiveBatch` MUST componer la primera tanda con las frases nuevas del nivel elegido.

#### Scenario: Primer uso crea la primera tanda

- **GIVEN** una base de datos vacía, un catálogo de quince frases A1 y el nivel elegido A1 con N igual a 20
- **WHEN** se obtiene la tanda activa el 10-03-2026
- **THEN** el resultado es `Created` con quince frases en orden de catálogo, todas de nivel 0

### Requirement: Cambio de fecha con tanda activa

Una tanda activa MUST conservar su composición aunque cambie el día. Las frases con repaso del nuevo día MUST entrar en la tanda siguiente, nunca en la activa.

#### Scenario: Cambio de día no recompone la tanda activa

- **GIVEN** una tanda activa compuesta el 10-03-2026 con cinco frases y una frase P de nivel 1 con repaso el 11-03-2026 fuera de ella
- **WHEN** se obtiene la tanda activa el 11-03-2026
- **THEN** el resultado es `Existing` con las mismas cinco frases y P no entra en ella

#### Scenario: Repaso del nuevo día entra en la tanda siguiente

- **GIVEN** la tanda del escenario anterior, de cinco frases de las que tres no están certificadas, cerrada el 11-03-2026 con expectativa 3
- **WHEN** se compone la tanda siguiente el 11-03-2026
- **THEN** P entra en la tanda siguiente

### Requirement: Lectura coherente del estado

La lectura del estado MUST devolver un único estado consistente: el progreso, la tanda activa y la cola corresponden a la misma versión persistida.

#### Scenario: Lectura durante un cierre

- **GIVEN** un cierre de tanda en curso
- **WHEN** se lee el estado
- **THEN** la lectura devuelve el estado anterior o el posterior al cierre, nunca una mezcla de ambos
