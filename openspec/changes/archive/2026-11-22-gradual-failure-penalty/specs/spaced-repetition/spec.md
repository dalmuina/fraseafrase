## MODIFIED Requirements

### Requirement: Racha persistente entre sesiones

Un acierto MUST incrementar en 1 la racha de una frase. Un fallo sobre una frase con racha mayor que 0 MUST
restarle 1 a la racha (ADR-0024); el efecto de un fallo con racha 0 lo fija "Resolución de la
autoevaluación". La racha MUST NOT reiniciarse por el solo hecho de que la frase entre en una tanda recién
compuesta: persiste de una sesión generada a la siguiente.

#### Scenario: Acierto suma uno a la racha

- **GIVEN** una frase con racha 1
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** su racha pasa a 2

#### Scenario: Fallo resta uno a la racha

- **GIVEN** una frase de nivel 2 con racha 2
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** su racha pasa a 1 y su nivel sigue siendo 2

#### Scenario: Fallo reinicia la racha

- **GIVEN** una frase de nivel 2 con racha 1
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** su racha pasa a 0 y su nivel sigue siendo 2

#### Scenario: La racha sobrevive a una sesión generada de por medio

- **GIVEN** una frase con racha 1 que, tras responderla, queda fuera de la tanda activa al generarse la
  siguiente sesión
- **WHEN** la frase vuelve a ofrecerse en una sesión posterior y la pantalla la muestra
- **THEN** su racha sigue siendo 1, no 0

### Requirement: Resolución de la autoevaluación

Cada autoevaluación MUST resolverse al instante, sin esperar a generar la siguiente sesión. Un acierto que
hace que la racha llegue a 3 MUST certificar la frase en ese momento. Un fallo MUST restar un solo acierto
(ADR-0024): con racha mayor que 0, la racha baja en 1 y el nivel no cambia; con racha 0 y nivel ≥ 1, el
nivel baja en 1 y la racha queda en 2; con racha 0 y nivel 0, nivel y racha no cambian. Un fallo, en
cualquier caso, MUST fijar su `dueAtSession` en la sesión siguiente.

#### Scenario: Fallo en nivel 3 baja a nivel 2 y fija la sesión siguiente

- **GIVEN** una frase de nivel 3 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 2, racha 2, y `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 1 baja a nivel 0 y fija la sesión siguiente

- **GIVEN** una frase de nivel 1 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 0, racha 2, y `dueAtSession` igual a 11

#### Scenario: Fallo con racha 0 en nivel 5 baja a nivel 4 con racha 2

- **GIVEN** una frase de nivel 5 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 4, racha 2, y `dueAtSession` igual a 11

#### Scenario: Fallo con racha mayor que 0 no baja de nivel

- **GIVEN** una frase de nivel 3 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 3, racha 1, y `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 0 no cambia el nivel pero sí fija la sesión siguiente

- **GIVEN** una frase de nivel 0 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase permanece en nivel 0, racha 0, con `dueAtSession` igual a 11

#### Scenario: Fallo en nivel 0 con racha mayor que 0 resta uno a la racha

- **GIVEN** una frase de nivel 0 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la frase queda en nivel 0, racha 1, y `dueAtSession` igual a 11

#### Scenario: Acierto que certifica sube de nivel y entra en enfriamiento

- **GIVEN** una frase de nivel 1 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 2, racha 0, y `dueAtSession` igual a 12 (sesión actual 10 más el
  enfriamiento del nivel 2, que es 2 sesiones)

#### Scenario: Acierto que no certifica fija la sesión siguiente

- **GIVEN** una frase de nivel 1 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 1, racha 1, y `dueAtSession` igual a 11

#### Scenario: Una frase nueva certificada por primera vez pasa a nivel 1

- **GIVEN** una frase de nivel 0 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 1, racha 0, y `dueAtSession` igual a 11 (sesión 10 + enfriamiento de
  nivel 1, que es 1 sesión)

### Requirement: Reversión de la última autoevaluación

Revertir la autoevaluación más reciente de toda la tanda MUST restaurar el nivel, la racha y el
`dueAtSession` que la frase afectada tenía inmediatamente antes de esa autoevaluación. Solo MUST
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

#### Scenario: Solo la autoevaluación más reciente de toda la tanda puede revertirse

- **GIVEN** una tanda en la que el usuario autoevalúa primero la frase A y después la frase B
- **WHEN** se pide revertir la autoevaluación de A
- **THEN** no es posible, porque B es la autoevaluación más reciente de toda la tanda, no A

#### Scenario: La reversión no se acumula

- **GIVEN** una autoevaluación que ya fue revertida
- **WHEN** se pide revertir de nuevo
- **THEN** no es posible, porque no queda ninguna autoevaluación pendiente de revertir
