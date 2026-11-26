# Spec Delta

## ADDED Requirements

### Requirement: Registro de respuestas por frase

Cada frase con progreso MUST registrar su última sesión respondida, sus sesiones seguidas y su última sesión
fallada (ADR-0027). Al autoevaluarla en la sesión `s`, aunque aún no esté pendiente, la última sesión respondida
MUST pasar a `s`; las sesiones seguidas MUST subir en 1 si era `s − 1`, no cambiar si era `s` y pasar a 1 en otro
caso. Un fallo MUST fijar la última sesión fallada en `s`; un acierto no la cambia.

#### Scenario: La primera respuesta inicia el registro

- **GIVEN** una frase nueva de la tanda, nunca respondida, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión respondida es 7, sus sesiones seguidas son 1 y no tiene última sesión fallada

#### Scenario: Responder en la sesión siguiente suma una sesión seguida

- **GIVEN** una frase respondida por última vez en la sesión 6 con 3 sesiones seguidas, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión respondida es 7 y sus sesiones seguidas son 4

#### Scenario: Una segunda respuesta en la misma sesión no suma

- **GIVEN** una frase respondida en la sesión 7 con 4 sesiones seguidas, en la sesión 7
- **WHEN** el usuario la autoevalúa de nuevo
- **THEN** su última sesión respondida sigue siendo 7 y sus sesiones seguidas siguen siendo 4

#### Scenario: Una sesión sin responderla reinicia las sesiones seguidas

- **GIVEN** una frase respondida por última vez en la sesión 4 con 5 sesiones seguidas, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión respondida es 7 y sus sesiones seguidas son 1

#### Scenario: Un fallo registra la sesión fallada

- **GIVEN** una frase de nivel 2 respondida por última vez en la sesión 6 sin fallos previos, en la sesión 7
- **WHEN** el usuario la autoevalúa con fallo
- **THEN** su última sesión fallada es 7 y su última sesión respondida es 7

#### Scenario: Un acierto conserva la última sesión fallada

- **GIVEN** una frase fallada por última vez en la sesión 5, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su última sesión fallada sigue siendo 5

#### Scenario: Un acierto en repaso adelantado también se registra

- **GIVEN** una frase de nivel 2 con racha 1 y `dueAtSession` 9, respondida por última vez en la sesión 6 con 1
  sesión seguida, en la sesión 7
- **WHEN** el usuario la autoevalúa con acierto
- **THEN** su nivel, su racha y su `dueAtSession` no cambian, su última sesión respondida es 7 y sus sesiones
  seguidas son 2

### Requirement: Componer una tanda no cambia el registro de respuestas

Generar sesión o recomponer la tanda activa MUST NOT cambiar el registro de respuestas de ninguna frase, entre o
no en la tanda nueva: solo una autoevaluación o su reversión lo cambian.

#### Scenario: Generar sesión no cambia el registro

- **GIVEN** una frase respondida por última vez en la sesión 6 con 2 sesiones seguidas y fallada en la sesión 6
- **WHEN** se genera la sesión 7 y la frase entra en la tanda
- **THEN** su última sesión respondida sigue siendo 6, sus sesiones seguidas 2 y su última sesión fallada 6

### Requirement: Invariantes del registro de respuestas

El registro de respuestas de una frase MUST rechazarse si alguna de sus sesiones es negativa, si tiene sesiones
seguidas sin última sesión respondida o una última sesión respondida con menos de 1 sesión seguida, o si su última
sesión fallada es posterior a su última sesión respondida o existe sin ella.

#### Scenario: Sesión respondida negativa se rechaza

- **GIVEN** un registro con última sesión respondida −1
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Sesiones seguidas sin sesión respondida se rechazan

- **GIVEN** un registro sin última sesión respondida y con 2 sesiones seguidas
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Sesión respondida con cero sesiones seguidas se rechaza

- **GIVEN** un registro con última sesión respondida 4 y 0 sesiones seguidas
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Sesión fallada posterior a la respondida se rechaza

- **GIVEN** un registro con última sesión respondida 4, 1 sesión seguida y última sesión fallada 5
- **WHEN** se construye
- **THEN** se rechaza

#### Scenario: Registro coherente se acepta

- **GIVEN** un registro con última sesión respondida 0, 1 sesión seguida y última sesión fallada 0
- **WHEN** se construye
- **THEN** se acepta

## MODIFIED Requirements

### Requirement: Reversión de la última autoevaluación

Revertir la autoevaluación más reciente de toda la tanda MUST restaurar el nivel, la racha, el
`dueAtSession` y el registro de respuestas (última sesión respondida, sesiones seguidas y última sesión
fallada) que la frase afectada tenía inmediatamente antes de esa autoevaluación. Solo MUST
poder revertirse la autoevaluación más reciente de toda la tanda. Tras revertirla, o tras cualquier
autoevaluación posterior (de esa frase o de otra), la posibilidad de revertir esa autoevaluación
concreta MUST desaparecer.

#### Scenario: Revertir un fallo restaura el nivel, la racha y el vencimiento anteriores

- **GIVEN** una frase de nivel 3 con racha 0 y `dueAtSession` 5, sesión actual 10, que el usuario
  autoevalúa con fallo y queda en nivel 2, racha 2 y `dueAtSession` 11
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a nivel 3, racha 0 y `dueAtSession` 5

#### Scenario: Revertir un acierto que certificó restaura el nivel y la racha anteriores

- **GIVEN** una frase de nivel 1 con racha 2 y `dueAtSession` 9, sesión actual 10, que el usuario
  autoevalúa con acierto y queda en nivel 2, racha 0 y `dueAtSession` 12
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a nivel 1, racha 2 y `dueAtSession` 9

#### Scenario: Revertir un fallo restaura el registro de respuestas

- **GIVEN** una frase respondida por última vez en la sesión 9 con 2 sesiones seguidas y fallada en la sesión 8,
  sesión actual 10, que el usuario autoevalúa con fallo y queda con última sesión respondida 10, 3 sesiones
  seguidas y última sesión fallada 10
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a última sesión respondida 9, 2 sesiones seguidas y última sesión fallada 8

#### Scenario: Solo la autoevaluación más reciente de toda la tanda puede revertirse

- **GIVEN** una tanda en la que el usuario autoevalúa primero la frase A y después la frase B
- **WHEN** se pide revertir la autoevaluación de A
- **THEN** no es posible, porque B es la autoevaluación más reciente de toda la tanda, no A

#### Scenario: La reversión no se acumula

- **GIVEN** una autoevaluación que ya fue revertida
- **WHEN** se pide revertir de nuevo
- **THEN** no es posible, porque no queda ninguna autoevaluación pendiente de revertir
