# Spec Delta

## REMOVED Requirements

### Requirement: Posiciones de la tanda y de la cola

**Reason**: sin cola de arrastradas (`practice-batch`), solo queda una posición que persistir: la de la tanda
activa.
**Migration**: ver "Posición de la tanda activa" de este delta.

### Requirement: Frase de nivel 0 ubicada

**Reason**: sin cola de arrastradas, una frase de nivel 0 puede simplemente existir en el progreso, dormida
hasta su `dueAtSession`, sin estar en la tanda activa ni en ninguna cola.
**Migration**: ninguna; no hay un caso equivalente que especificar.

### Requirement: Cierre con expectativa de aviso

**Reason**: este requisito describe el cierre con `expectedUncertified`/`Stale`/`NoActiveBatch` de
`CloseActiveBatch`, retirado en `replace-batch-closure-with-session-generation`. Quedó en la especificación
por un descuido de aquel cambio; no corresponde a ningún código desde entonces.
**Migration**: ninguna; no hay un caso equivalente que especificar.

### Requirement: Cambio de fecha con tanda activa

**Reason**: el repaso ya no depende de la fecha del dispositivo, así que una tanda activa es insensible al
calendario por construcción; no hace falta una regla aparte sobre cambios de día.
**Migration**: ninguna; no hay un caso equivalente que especificar.

### Requirement: Cierre atómico de la tanda

**Reason**: ya no hay "resultados de cada frase" que persistir al cerrar (cada frase se resuelve al instante
de su autoevaluación); lo que se persiste atómicamente es la tanda siguiente y el contador de sesiones.
**Migration**: ver "Composición atómica de la tanda siguiente" de este delta.

### Requirement: Composición única tras el cierre

**Reason**: "cierre" deja de ser el término correcto; la tanda activa simplemente se vacía (al generar sesión
o al recomponer por ajustes) y `ObtainActiveBatch` compone la siguiente la próxima vez que se pide.
**Migration**: ver "Composición única tras vaciar la tanda activa" de este delta.

## MODIFIED Requirements

### Requirement: Persistencia del progreso por frase

El sistema MUST persistir una fila por cada frase con progreso o con posición en la tanda activa. Tras
reiniciar la app, el progreso recuperado MUST ser igual al último guardado.

#### Scenario: Progreso sobrevive al reinicio

- **GIVEN** una frase de nivel 2 con `dueAtSession` igual a 13 guardada tras generar sesión
- **WHEN** la app se reinicia y se consulta el progreso
- **THEN** la frase tiene nivel 2 y `dueAtSession` igual a 13

## ADDED Requirements

### Requirement: Posición de la tanda activa

Cada frase de la tanda activa MUST ocupar una posición única (`batch_position`). Una frase fuera de la tanda
activa MUST NOT tener posición.

#### Scenario: Posición repetida se rechaza

- **GIVEN** dos frases distintas con la misma posición de tanda
- **WHEN** se intenta guardar
- **THEN** el guardado falla y no se modifica el estado

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
