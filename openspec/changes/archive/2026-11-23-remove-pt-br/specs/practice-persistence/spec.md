## MODIFIED Requirements

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

### Requirement: Primer uso

Si la base de datos no tiene progreso ni tanda para un idioma destino, `ObtainActiveBatch` de ese
idioma destino MUST componer la primera tanda de ese idioma destino con las frases nuevas del nivel
elegido para ese idioma destino.

#### Scenario: Primer uso crea la primera tanda

- **GIVEN** una base de datos sin progreso ni tanda para el idioma destino `pt-PT`, un catálogo de
  `pt-PT` con quince frases A1 y el nivel elegido A1 con N igual a 20 para `pt-PT`
- **WHEN** se obtiene la tanda activa de `pt-PT`
- **THEN** el resultado es `Created` con quince frases en orden de catálogo, todas de nivel 0

#### Scenario: El primer uso de un idioma destino no crea tanda para otro

- **GIVEN** `pt-PT` con progreso y tanda activa ya existentes, y `en-GB` sin progreso ni tanda
- **WHEN** se obtiene la tanda activa de `en-GB`
- **THEN** se compone la primera tanda de `en-GB` y la tanda activa de `pt-PT` no se modifica
