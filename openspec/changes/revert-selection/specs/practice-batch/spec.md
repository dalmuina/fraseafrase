# Spec Delta

## ADDED Requirements

### Requirement: Reversión restaura la posición en la lista circular

Revertir la autoevaluación más reciente de toda la tanda MUST devolver la frase afectada a la
posición que ocupaba en la lista circular antes de esa autoevaluación, sin alterar el orden
relativo de las demás frases entre sí.

#### Scenario: Revertir un acierto devuelve la frase a su posición original

- **GIVEN** una tanda con las frases A, B y C en ese orden, que queda en el orden B, C, A tras
  autoevaluar A con acierto
- **WHEN** se revierte esa autoevaluación
- **THEN** la tanda vuelve al orden A, B, C

#### Scenario: Revertir no altera el orden relativo de las demás frases

- **GIVEN** una tanda con las frases A, B y C en ese orden, que queda en el orden B, C, A tras
  autoevaluar A
- **WHEN** se revierte esa autoevaluación
- **THEN** B sigue delante de C en la tanda resultante

#### Scenario: No se puede revertir la posición tras una segunda autoevaluación

- **GIVEN** una tanda con las frases A, B y C en ese orden; el usuario autoevalúa A (orden B, C, A)
  y después B (orden C, A, B)
- **WHEN** se pide revertir la autoevaluación de A
- **THEN** no es posible, porque la autoevaluación de B es la más reciente de toda la tanda
