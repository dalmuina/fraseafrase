# Spec Delta

## ADDED Requirements

### Requirement: Bajada instantánea de nivel ante un fallo

Un fallo (`SelfAssessment.MISS`) en una frase con nivel ≥ 1 MUST bajar su nivel en 1 (nunca por debajo de
0) en el momento de la autoevaluación, sin esperar al cierre de la tanda, y MUST recalcular su próximo
repaso con el intervalo del nivel resultante a partir de la fecha de esa autoevaluación. Un fallo en una
frase de nivel 0 MUST NOT cambiar su nivel ni su próximo repaso (ambos ya son 0 y nulo).

#### Scenario: Fallo en nivel 3 baja a nivel 2 y recalcula el repaso

- **GIVEN** una frase de nivel 3 con próximo repaso el 20-03-2026
- **WHEN** el usuario se autoevalúa con fallo el 10-03-2026
- **THEN** la frase queda en nivel 2 con próximo repaso el 13-03-2026

#### Scenario: Fallo en nivel 1 baja a nivel 0 y elimina el próximo repaso

- **GIVEN** una frase de nivel 1 con próximo repaso el 11-03-2026
- **WHEN** el usuario se autoevalúa con fallo el 10-03-2026
- **THEN** la frase queda en nivel 0 sin próximo repaso

#### Scenario: Fallo en nivel 0 no cambia el nivel

- **GIVEN** una frase de nivel 0 sin próximo repaso
- **WHEN** el usuario se autoevalúa con fallo el 10-03-2026
- **THEN** la frase permanece en nivel 0 sin próximo repaso

#### Scenario: Varios fallos en la misma tanda bajan el nivel de nuevo cada vez

- **GIVEN** una frase de nivel 4 con próximo repaso el 20-03-2026
- **WHEN** el usuario se autoevalúa con fallo el 10-03-2026 y, más adelante en la misma tanda, vuelve a
  fallar el mismo 10-03-2026
- **THEN** la frase queda en nivel 2 con próximo repaso el 13-03-2026

## MODIFIED Requirements

### Requirement: Resolución de frase respondida no certificada al cerrar la tanda

Al cerrar la tanda, una frase respondida y no certificada MUST arrastrarse a la siguiente tanda con el
nivel y el próximo repaso que ya tenía en ese momento: ambos quedaron fijados, fallo a fallo, por la
regla de "Bajada instantánea de nivel ante un fallo" durante la práctica, no al cerrar la tanda. El cierre
MUST NOT volver a modificarlos.

#### Scenario: Respondida no certificada de nivel 4 vuelve a nivel 0

> El título es el heredado del requisito que este delta sustituye; el resultado real está en el THEN:
> con ADR-0015 la frase ya no vuelve a nivel 0 al cerrar, conserva el nivel que le fijó su último fallo.

- **GIVEN** una frase de nivel 4 que, tras un fallo durante la tanda activa, bajó a nivel 3 con próximo
  repaso el 17-03-2026, y terminó la tanda con racha 1
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase se arrastra a la siguiente tanda en nivel 3 con próximo repaso el 17-03-2026, el nivel
  que ya tenía al cerrar; el cierre no la vuelve a bajar a nivel 0

#### Scenario: Respondida no certificada de nivel 5 vuelve a nivel 0

> El título es el heredado del requisito que este delta sustituye; el resultado real está en el THEN:
> con ADR-0015 la frase ya no vuelve a nivel 0 al cerrar, conserva el nivel que le fijó su último fallo.

- **GIVEN** una frase de nivel 5 que, tras un fallo durante la tanda activa, bajó a nivel 4 con próximo
  repaso el 24-03-2026, y terminó la tanda con racha 0
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase se arrastra a la siguiente tanda en nivel 4 con próximo repaso el 24-03-2026, el nivel
  que ya tenía al cerrar; el cierre no la vuelve a bajar a nivel 0

#### Scenario: Respondida no certificada de nivel 0 permanece en nivel 0

- **GIVEN** una frase de nivel 0 con racha 0 que fue respondida en la tanda activa
- **WHEN** se cierra la tanda el 10-03-2026
- **THEN** la frase queda en nivel 0, sin próximo repaso, y se arrastra a la siguiente tanda
