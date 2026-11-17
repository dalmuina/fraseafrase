# Spec Delta

## MODIFIED Requirements

### Requirement: Composición de la tanda nueva por cuotas sobre el progreso

La tanda nueva MUST llenarse hasta N frases en tres bloques con cuota propia sobre N: nuevas
`floor(N * 0.20)`, a reforzar `floor(N * 0.55)` y dominadas el resto (`N` menos las dos cuotas anteriores).
Si un bloque no alcanza su cuota, el hueco MUST repartirse entre los demás bloques con candidatos
disponibles, en el orden a reforzar, nuevas, dominadas.

#### Scenario: Cuotas por defecto con material suficiente

- **GIVEN** 11 frases a reforzar disponibles, 6 dominadas disponibles y nuevas suficientes del nivel elegido,
  con N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 4 frases nuevas, 11 a reforzar y 5 dominadas

#### Scenario: Reforzar corto cede su hueco a nuevas

- **GIVEN** un bloque a reforzar con solo 5 candidatos, nuevas y dominadas con material de sobra, y N igual
  a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 5 a reforzar, 10 nuevas y 5 dominadas

#### Scenario: Dominadas corto cede su hueco a reforzar

- **GIVEN** un bloque de dominadas con solo 1 candidato, a reforzar y nuevas con material de sobra, y N igual
  a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 15 a reforzar, 4 nuevas y 1 dominada

#### Scenario: Cálculo de las cuotas con N igual a 20

- **GIVEN** N igual a 20
- **WHEN** se calculan las cuotas de los tres bloques
- **THEN** la cuota de nuevas es 4, la de a reforzar es 11 y la de dominadas es 5

#### Scenario: Cálculo de las cuotas con un N no divisible exactamente

- **GIVEN** N igual a 17
- **WHEN** se calculan las cuotas de los tres bloques
- **THEN** la cuota de nuevas es 3 (`floor(17 * 0.20)`), la de a reforzar es 9 (`floor(17 * 0.55)`) y la de
  dominadas es 5 (17 menos las dos anteriores)

#### Scenario: Una frase pendiente vuelve a estar disponible en su sesión

- **GIVEN** una frase con `dueAtSession` igual a 12, y la sesión actual es 12
- **WHEN** se compone la tanda
- **THEN** la frase es candidata al bloque a reforzar

#### Scenario: Una frase con sesión futura no entra antes de su vencimiento

- **GIVEN** una frase con `dueAtSession` igual a 13, y la sesión actual es 12
- **WHEN** se compone la tanda
- **THEN** la frase no es candidata al bloque a reforzar en esta composición

#### Scenario: El filtro CEFR no deja bajar de nivel a reforzar ni a dominadas

- **GIVEN** una frase a reforzar de nivel CEFR A1 y una dominada de nivel CEFR A2, con el nivel elegido B1
- **WHEN** se compone la tanda
- **THEN** la tanda no contiene ninguna de las dos

#### Scenario: El filtro CEFR admite niveles iguales o superiores al elegido en reforzar y dominadas

- **GIVEN** una frase a reforzar de nivel CEFR B1 y una dominada de nivel CEFR C1, con el nivel elegido A1
- **WHEN** se compone la tanda
- **THEN** la tanda no contiene ninguna de las dos, porque ambas son de un nivel CEFR distinto al elegido: el
  filtro deja de admitir niveles superiores al elegido y pasa a exigir el nivel exacto

#### Scenario: El filtro CEFR admite solo el nivel elegido en reforzar y dominadas

- **GIVEN** una frase a reforzar de nivel CEFR A1 y una dominada de nivel CEFR A1, con el nivel elegido A1
- **WHEN** se compone la tanda
- **THEN** la tanda contiene ambas

#### Scenario: Sin material del nivel elegido o superior la tanda sale más pequeña

- **GIVEN** todas las frases a reforzar y dominadas disponibles son de un nivel CEFR distinto al elegido, y no
  hay frases nuevas del nivel elegido, con N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda tiene menos de 20 frases, sin incluir ninguna frase de un nivel CEFR distinto al elegido

### Requirement: Regeneración de la tanda activa por cambio de nivel CEFR sin resolución

Al cambiar el nivel CEFR elegido, el sistema MUST recomponer la tanda activa de inmediato con el nuevo nivel,
sin incrementar el contador de sesiones generadas y sin tocar la racha, el nivel de consolidación ni el
`dueAtSession` de ninguna frase: solo cambia qué frases están activas, a partir del progreso tal como está en
ese momento. El progreso de las frases de un nivel CEFR distinto al recién elegido MUST conservarse sin
cambios y MUST volver a ser candidato a la tanda si el usuario vuelve a elegir ese nivel CEFR más adelante.

#### Scenario: Cambiar el nivel CEFR no toca racha, nivel ni dueAtSession

- **GIVEN** una tanda activa con una frase de nivel 3 y racha 2, y otra de nivel 1 y racha 0
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** ambas frases conservan su nivel de consolidación, su racha y su `dueAtSession` sin cambios

#### Scenario: Cambiar el nivel CEFR no incrementa el contador de sesiones

- **GIVEN** el contador de sesiones en 9
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** el contador de sesiones sigue en 9

#### Scenario: La tanda siguiente se compone por cuotas con el nuevo nivel

- **GIVEN** material suficiente de a reforzar, pendientes, dominadas y nuevas del nuevo nivel elegido
- **WHEN** se compone la tanda siguiente tras el cambio de nivel CEFR
- **THEN** se compone con las mismas cuotas de nuevas, a reforzar y dominadas que una generación de sesión

#### Scenario: El progreso de un nivel CEFR abandonado se conserva y vuelve a ofrecerse al volver a ese nivel

- **GIVEN** una frase a reforzar de nivel CEFR B1 con nivel de consolidación 3 y racha 2, mientras el nivel
  CEFR elegido es B1
- **WHEN** el usuario cambia el nivel CEFR elegido a A1, practica varias sesiones, y después vuelve a elegir
  B1
- **THEN** la frase de B1 conserva su nivel de consolidación 3 y su racha 2, y vuelve a ser candidata a la
  tanda en el bloque que le corresponda según su `dueAtSession`
