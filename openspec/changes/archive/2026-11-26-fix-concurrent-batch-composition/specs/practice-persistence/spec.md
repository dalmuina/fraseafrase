# Spec Delta

## ADDED Requirements

### Requirement: Obtención concurrente de la tanda activa

Cuando dos obtenciones de la tanda activa de un mismo idioma destino coinciden y las dos deciden componer o
recomponer, el sistema MUST componer una sola vez. La obtención que, al escribir, encuentra la tanda activa distinta
de la que observó (por otra obtención o por una autoevaluación) MUST devolver la tanda vigente como `Existing`, o
`NothingAvailable` si ya no hay tanda activa, sin vaciarla ni recomponerla, sin añadir progreso y sin cambiar el
contador de sesiones generadas.

#### Scenario: Dos obtenciones simultáneas sin tanda activa componen una sola tanda

- **GIVEN** `pt-PT` sin tanda activa, con el contador de sesiones generadas a 0, un catálogo de `pt-PT` con treinta
  frases A1, el nivel elegido A1 con N igual a 10, y progreso previo de 4 frases A1 con nivel de consolidación 1 y
  `dueAtSession` 5
- **WHEN** se obtienen dos veces a la vez la tanda activa de `pt-PT` y las dos leen "sin tanda activa" antes de que
  ninguna escriba
- **THEN** queda una única tanda activa de 10 frases, exactamente una obtención devuelve `Created` y la otra
  `Existing` con esa misma tanda en el mismo orden, el progreso de `pt-PT` contiene exactamente las 4 frases previas
  y las 6 nuevas de esa tanda, y el contador de sesiones generadas sigue a 0

#### Scenario: La obtención que llega tarde no recompone la tanda ya compuesta

- **GIVEN** una obtención de la tanda activa de un idioma destino que ha leído "sin tanda activa" y otra que, antes
  de que la primera escriba, ya ha guardado una tanda de 10 frases
- **WHEN** la primera obtención escribe
- **THEN** el resultado es `Existing` con la tanda de 10 frases ya guardada, en el mismo orden, y el progreso no
  cambia

#### Scenario: La obtención que llega tarde sin tanda vigente devuelve que no hay frases

- **GIVEN** una obtención de la tanda activa de un idioma destino que ha leído una tanda activa y otra que, antes de
  que la primera escriba, ha vaciado esa tanda sin encontrar frases para componer la siguiente
- **WHEN** la primera obtención escribe
- **THEN** el resultado es `NothingAvailable`, no hay tanda activa y el progreso y el contador de sesiones generadas
  no cambian

#### Scenario: Dos obtenciones simultáneas tras un cambio de ajustes recomponen una sola vez

- **GIVEN** una tanda activa de un idioma destino compuesta con el nivel CEFR A1 y N igual a 10, registrados como sus
  ajustes aplicados, con el contador de sesiones generadas a 0, progreso previo de 4 frases B1 con nivel de
  consolidación 1 y `dueAtSession` 5, un catálogo con las frases A1 de la tanda activa y treinta frases B1, y los ajustes guardados de ese idioma
  destino cambiados al nivel CEFR B1
- **WHEN** se obtienen dos veces a la vez la tanda activa de ese idioma destino y las dos leen la tanda A1 antes de
  que ninguna escriba
- **THEN** la tanda activa se recompone una sola vez con 10 frases B1, exactamente una obtención devuelve `Created` y
  la otra `Existing` con esa misma tanda, y el progreso solo añade las 6 frases B1 nuevas de esa tanda
