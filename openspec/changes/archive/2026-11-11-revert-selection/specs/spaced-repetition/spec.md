# Spec Delta

## ADDED Requirements

### Requirement: Reversión de la última autoevaluación

Revertir la autoevaluación más reciente de toda la tanda MUST restaurar el nivel, la racha y el
`dueAtSession` que la frase afectada tenía inmediatamente antes de esa autoevaluación. Solo MUST
poder revertirse la autoevaluación más reciente de toda la tanda. Tras revertirla, o tras cualquier
autoevaluación posterior (de esa frase o de otra), la posibilidad de revertir esa autoevaluación
concreta MUST desaparecer.

#### Scenario: Revertir un fallo restaura el nivel, la racha y el vencimiento anteriores

- **GIVEN** una frase de nivel 3 con racha 1 y `dueAtSession` 5, sesión actual 10, que el usuario
  autoevalúa con fallo y queda en nivel 2 con `dueAtSession` 11
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a nivel 3, racha 1 y `dueAtSession` 5

#### Scenario: Revertir un acierto que certificó restaura el nivel y la racha anteriores

- **GIVEN** una frase de nivel 1 con racha 2 y `dueAtSession` 9, sesión actual 10, que el usuario
  autoevalúa con acierto y queda en nivel 2, racha 0 y `dueAtSession` 12
- **WHEN** se revierte esa autoevaluación
- **THEN** la frase vuelve a nivel 1, racha 2 y `dueAtSession` 9

#### Scenario: Solo la autoevaluación más reciente de toda la tanda puede revertirse

- **GIVEN** una tanda en la que el usuario autoevalúa primero la frase A y después la frase B
- **WHEN** se pide revertir la autoevaluación de A
- **THEN** no es posible, porque B es la autoevaluación más reciente de toda la tanda, no A

#### Scenario: La reversión no se acumula

- **GIVEN** una autoevaluación que ya fue revertida
- **WHEN** se pide revertir de nuevo
- **THEN** no es posible, porque no queda ninguna autoevaluación pendiente de revertir
