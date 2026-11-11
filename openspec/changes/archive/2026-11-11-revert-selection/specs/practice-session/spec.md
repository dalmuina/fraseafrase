# Spec Delta

## ADDED Requirements

### Requirement: Botón flotante para revertir la última autoevaluación

Tras autoevaluar una frase y avanzar a la frase siguiente, la pantalla MUST mostrar un botón
flotante en la esquina inferior izquierda que permite revertir esa autoevaluación. El botón MUST
NOT mostrarse antes de la primera autoevaluación de la tanda activa, mientras una autoevaluación
esté en curso (`busy`), ni durante la transición del indicador de cambio de insignia de la frase
que se acaba de autoevaluar.

#### Scenario: El botón aparece tras autoevaluar

- **GIVEN** una tanda con las frases A y B en ese orden, con A revelada
- **WHEN** el usuario se autoevalúa A con acierto y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra el botón flotante de revertir

#### Scenario: El botón no aparece antes de la primera autoevaluación

- **GIVEN** el usuario abre la pantalla de práctica y todavía no ha autoevaluado ninguna frase de
  la tanda activa
- **WHEN** la pantalla muestra la primera frase
- **THEN** el botón flotante de revertir no está visible

#### Scenario: El botón no aparece durante la transición de cambio de insignia

- **GIVEN** un acierto que certifica una frase y dispara la transición del indicador de cambio de
  insignia
- **WHEN** la pantalla muestra esa transición, antes de avanzar a la frase siguiente
- **THEN** el botón flotante de revertir todavía no está visible

#### Scenario: El botón no aparece mientras se registra una nueva autoevaluación

- **GIVEN** una tanda con las frases A, B y C en ese orden; A ya autoevaluada, con la pantalla
  mostrando B y el botón flotante de revertir visible (apuntando a la autoevaluación de A)
- **WHEN** el usuario se autoevalúa B, mientras esa autoevaluación está en curso, antes de que la
  pantalla pase a mostrar C
- **THEN** el botón flotante de revertir no está visible

### Requirement: Revertir vuelve a la frase anterior y oculta el botón

Al pulsar el botón flotante de revertir, la pantalla MUST volver a mostrar la frase afectada como
la frase activa, revelada, en la posición que ocupaba antes de esa autoevaluación, con su racha y
su insignia de nivel de consolidación anteriores, y MUST dejar de mostrar el botón flotante. No
MUST ser posible registrar más de una reversión por autoevaluación.

#### Scenario: Revertir un acierto vuelve a mostrar la frase anterior

- **GIVEN** una tanda con las frases A, B y C en ese orden, con A revelada, que el usuario
  autoevalúa con acierto y la pantalla pasa a mostrar B sin revelar
- **WHEN** el usuario pulsa el botón flotante de revertir
- **THEN** la pantalla vuelve a mostrar A revelada, con su racha e insignia anteriores, y el botón
  flotante deja de mostrarse

#### Scenario: Doble toque en revertir solo revierte una vez

- **GIVEN** el botón flotante de revertir visible tras autoevaluar una frase
- **WHEN** el usuario lo pulsa dos veces seguidas
- **THEN** solo se revierte esa autoevaluación una vez y la segunda pulsación se ignora

#### Scenario: Autoevaluar de nuevo tras revertir vuelve a ofrecer el botón

- **GIVEN** la frase A vuelta a mostrar, revelada, tras revertir su autoevaluación anterior
- **WHEN** el usuario se autoevalúa A de nuevo y la pantalla avanza a la frase siguiente
- **THEN** la pantalla muestra el botón flotante de revertir, disponible para esta nueva
  autoevaluación

#### Scenario: Generar sesión hace desaparecer el botón

- **GIVEN** el botón flotante de revertir visible tras la última autoevaluación de la tanda activa,
  con la pantalla mostrando el mensaje de tanda completada
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra la tanda siguiente sin el botón flotante de revertir visible
