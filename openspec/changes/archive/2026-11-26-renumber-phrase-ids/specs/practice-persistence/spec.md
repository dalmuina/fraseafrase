## MODIFIED Requirements

### Requirement: Persistencia del progreso por frase

El sistema MUST persistir una fila por cada frase con progreso o con posición en la tanda activa de
un idioma destino, con su nivel, su racha, su `dueAtSession` y su registro de respuestas (última sesión
respondida, sesiones seguidas y última sesión fallada). El progreso de una frase MUST ser específico de su
idioma destino: la misma frase puede tener progreso distinto en dos idiomas destino. Tras reiniciar la app, el
progreso recuperado de cada idioma destino MUST ser igual al último guardado para ese idioma destino.

#### Scenario: Progreso sobrevive al reinicio

- **GIVEN** una frase de nivel 2 con `dueAtSession` igual a 13 guardada tras generar sesión en el
  idioma destino `pt-PT`
- **WHEN** la app se reinicia y se consulta el progreso de `pt-PT`
- **THEN** la frase tiene nivel 2 y `dueAtSession` igual a 13

#### Scenario: El registro de respuestas sobrevive al reinicio

- **GIVEN** una frase de `pt-PT` respondida por última vez en la sesión 12 con 3 sesiones seguidas y fallada en la
  sesión 11, y otra nunca respondida
- **WHEN** la app se reinicia y se consulta el progreso de `pt-PT`
- **THEN** la primera tiene última sesión respondida 12, 3 sesiones seguidas y última sesión fallada 11, y la
  segunda no tiene registro de respuestas

#### Scenario: El progreso de dos idiomas destino para la misma frase no se mezcla

- **GIVEN** la frase `0000-a1-saludos-001` con nivel 3 en el idioma destino `pt-PT` y nivel 0 en `en-GB`
- **WHEN** se consulta el progreso de esa frase en cada idioma destino
- **THEN** `pt-PT` devuelve nivel 3 y `en-GB` devuelve nivel 0
