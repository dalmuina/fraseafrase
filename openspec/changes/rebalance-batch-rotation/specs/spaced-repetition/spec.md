# Spec Delta

## MODIFIED Requirements

### Requirement: Resolución de la autoevaluación

Cada autoevaluación MUST resolverse al instante, sin esperar a generar la siguiente sesión. Un acierto que
hace que la racha llegue a 3 MUST certificar la frase en ese momento. Un acierto que no certifica MUST fijar
su `dueAtSession` en la sesión actual más la espera intra-nivel de su nivel (ADR-0025). Un fallo MUST restar
un solo acierto (ADR-0024): con racha mayor que 0, la racha baja en 1 y el nivel no cambia; con racha 0 y
nivel ≥ 1, el nivel baja en 1 y la racha queda en 2; con racha 0 y nivel 0, nivel y racha no cambian. Un
fallo, en cualquier caso, MUST fijar su `dueAtSession` en la sesión siguiente.

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
- **THEN** la frase permanece en nivel 1, racha 1, y `dueAtSession` igual a 11 (sesión 10 más la espera
  intra-nivel del nivel 1, que es 1 sesión)

#### Scenario: Acierto que no certifica en nivel 3 espera dos sesiones

- **GIVEN** una frase de nivel 3 con racha 1, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 3, racha 2, y `dueAtSession` igual a 12

#### Scenario: Acierto que no certifica en nivel 5 espera ocho sesiones

- **GIVEN** una frase de nivel 5 con racha 0, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase permanece en nivel 5, racha 1, y `dueAtSession` igual a 18

#### Scenario: Una frase nueva certificada por primera vez pasa a nivel 1

- **GIVEN** una frase de nivel 0 con racha 2, sesión actual 10
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la frase queda en nivel 1, racha 0, y `dueAtSession` igual a 11 (sesión 10 + enfriamiento de
  nivel 1, que es 1 sesión)

## ADDED Requirements

### Requirement: Espera intra-nivel tras un acierto que no certifica

La función que calcula la espera tras un acierto que no certifica MUST devolver, según el nivel actual de la
frase: 1 sesión para los niveles 0, 1 y 2; 2 sesiones para el nivel 3; 4 sesiones para el nivel 4; y 8
sesiones para el nivel 5. Es la mitad del enfriamiento de ese nivel, con un mínimo de 1 sesión (ADR-0025).

#### Scenario: Espera intra-nivel de cada nivel

- **GIVEN** los niveles 0, 1, 2, 3, 4 y 5
- **WHEN** se calcula su espera intra-nivel en sesiones
- **THEN** los resultados son 1, 1, 1, 2, 4 y 8 respectivamente
