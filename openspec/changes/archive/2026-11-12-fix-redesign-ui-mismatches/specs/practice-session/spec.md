# Spec Delta

## MODIFIED Requirements

### Requirement: Botón flotante para revertir la última autoevaluación

Tras autoevaluar una frase y avanzar a la frase siguiente, la pantalla MUST mostrar un botón que
permite revertir esa autoevaluación, apilado sobre el control principal en vez de superponerse al
contenido. El botón MUST NOT mostrarse antes de la primera autoevaluación de la tanda activa,
mientras una autoevaluación esté en curso (`busy`), durante la transición del indicador de cambio
de insignia de la frase que se acaba de autoevaluar, ni mientras la frase actual esté revelada.

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

#### Scenario: El botón desaparece al revelar la frase siguiente

- **GIVEN** una tanda con las frases A y B en ese orden; A ya autoevaluada, con la pantalla
  mostrando B sin revelar y el botón flotante de revertir visible (apuntando a la autoevaluación
  de A)
- **WHEN** el usuario revela B
- **THEN** el botón flotante de revertir deja de estar visible
