# Spec Delta

## ADDED Requirements

### Requirement: Mensaje de ayuda para traducir mentalmente

Mientras la frase actual no esté revelada, la pantalla de práctica MUST mostrar un mensaje de ayuda que
invite al usuario a traducir la frase mentalmente antes de comprobar la traducción. Al revelar la frase,
la pantalla MUST dejar de mostrar ese mensaje.

#### Scenario: El mensaje de ayuda se muestra sin revelar

- **GIVEN** la pantalla de práctica muestra una frase sin revelar
- **WHEN** la pantalla se muestra
- **THEN** la pantalla muestra el mensaje de ayuda para traducir la frase mentalmente

#### Scenario: El mensaje de ayuda desaparece al revelar

- **GIVEN** la pantalla de práctica muestra el mensaje de ayuda junto a una frase sin revelar
- **WHEN** el usuario revela la frase
- **THEN** la pantalla deja de mostrar el mensaje de ayuda

#### Scenario: El mensaje de ayuda vuelve a mostrarse con la frase siguiente

- **GIVEN** una tanda con las frases A y B en ese orden, con A revelada
- **WHEN** el usuario se autoevalúa A y la pantalla pasa a mostrar B sin revelar
- **THEN** la pantalla muestra de nuevo el mensaje de ayuda para traducir la frase mentalmente
