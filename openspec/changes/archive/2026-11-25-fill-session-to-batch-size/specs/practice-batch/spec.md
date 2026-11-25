## ADDED Requirements

### Requirement: Orden del repaso adelantado

Las frases de repaso adelantado (con progreso del nivel CEFR elegido y `dueAtSession` posterior a la sesión
actual) MUST ordenarse por `nivel de consolidación + (dueAtSession − sesión actual)`, de menor a mayor. A igual
valor, MUST desempatar el orden de catálogo. Si no caben todas, MUST entrar las primeras de ese orden y el resto
MUST quedar fuera sin cambiar su progreso (ADR-0026).

#### Scenario: Solo entra el repaso adelantado que cabe

- **GIVEN** N igual a 10, sesión actual 20, nueve pendientes de nivel 1, ninguna frase nueva disponible, la
  frase A de nivel 2 con `dueAtSession` 21 (valor 3) y la frase B de nivel 0 con `dueAtSession` 22 (valor 2)
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las nueve pendientes y B; A no entra y conserva su progreso

#### Scenario: Empate en el repaso adelantado se resuelve por catálogo

- **GIVEN** N igual a 10, sesión actual 20, ninguna pendiente ni frase nueva, la frase A de nivel 1 con
  `dueAtSession` 22 (valor 3) y la frase B de nivel 2 con `dueAtSession` 21 (valor 3), con B antes que A en el
  catálogo
- **WHEN** se compone la tanda
- **THEN** la tanda es B, A

## MODIFIED Requirements

### Requirement: Composición de la tanda o ausencia de tanda

La composición MUST devolver una tanda de `min(N, frases del nivel CEFR elegido en el catálogo)` frases, con o
sin progreso (ADR-0026). Solo si el nivel CEFR elegido no tiene ninguna frase en el catálogo MUST devolver
ausencia de tanda. Sin progreso previo y con catálogo disponible, MUST crear la primera tanda. Las entradas
nuevas MUST tener nivel 0 y racha 0.

#### Scenario: Primera tanda con catálogo disponible

- **GIVEN** ningún progreso previo, un catálogo de quince frases del nivel elegido y N igual a 20
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con las 15 frases en orden de catálogo, con nivel 0 y racha 0

#### Scenario: Primera tanda con más frases que N

- **GIVEN** ningún progreso previo, un catálogo de quince frases del nivel elegido y N igual a 10
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con las 10 primeras frases en orden de catálogo, con nivel 0 y racha 0

#### Scenario: Tanda menor que N si faltan frases

- **GIVEN** un nivel elegido con solo tres frases en el catálogo, las tres pendientes, y N igual a 10
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con tres frases

#### Scenario: Catálogo vacío sin progreso no crea tanda

- **GIVEN** ningún progreso previo y un catálogo vacío
- **WHEN** se compone la tanda
- **THEN** el resultado es ausencia de tanda

#### Scenario: Catálogo sin frases del nivel elegido no crea tanda

- **GIVEN** ningún progreso previo y un catálogo con solo frases A1, con el nivel elegido B1
- **WHEN** se compone la tanda
- **THEN** el resultado es ausencia de tanda

#### Scenario: Catálogo agotado sin pendientes no crea tanda

- **GIVEN** progreso solo de frases B1, con el nivel elegido A1 y un catálogo sin ninguna frase A1
- **WHEN** se compone la tanda
- **THEN** el resultado es ausencia de tanda

#### Scenario: Solo repasos futuros crean una tanda de repaso adelantado

- **GIVEN** un catálogo de tres frases B1, todas con progreso de nivel 2 y `dueAtSession` posterior a la sesión
  actual, ninguna frase nueva disponible, el nivel elegido B1 y N igual a 10
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con las tres frases, sin cambiar su nivel, su racha ni su `dueAtSession`

### Requirement: Candidatas a la tanda nueva

La tanda nueva MUST componerse solo con frases del nivel CEFR exactamente igual al elegido (ADR-0021) de tres
tipos: pendientes (con progreso y `dueAtSession` igual o anterior a la sesión actual), nuevas (sin progreso) y
repaso adelantado (con progreso y `dueAtSession` posterior a la sesión actual). El repaso adelantado MUST entrar
solo si las pendientes y las nuevas dentro del límite no llegan a N (ADR-0026).

#### Scenario: Una frase pendiente vuelve a estar disponible en su sesión

- **GIVEN** una frase con `dueAtSession` igual a 12, y la sesión actual es 12
- **WHEN** se compone la tanda
- **THEN** la frase entra en la tanda

#### Scenario: Una frase con sesión futura no entra antes de su vencimiento

> El título es el heredado (OpenSpec no permite renombrar un escenario dentro de MODIFIED). Desde ADR-0026 la
> frase solo queda fuera si la tanda se llena antes; el caso contrario está en el escenario siguiente.

- **GIVEN** N igual a 10, sesión actual 12, diez pendientes de nivel 1, ninguna de nivel 0, ninguna frase nueva
  disponible, y una frase F con `dueAtSession` igual a 13
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las diez pendientes y F no entra

#### Scenario: Una frase con sesión futura entra como repaso adelantado si falta sitio

- **GIVEN** N igual a 10, sesión actual 12, dos pendientes, ninguna frase nueva disponible, y una frase F con
  `dueAtSession` igual a 13
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las dos pendientes y F

#### Scenario: Una frase de nivel alto sin vencer no entra

- **GIVEN** N igual a 10, sesión actual 20, seis pendientes de nivel 1, ninguna de nivel 0, veinte frases nuevas
  disponibles, una frase de nivel 5 con `dueAtSession` igual a 30 y una frase de nivel 4 con `dueAtSession`
  igual a 25
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las seis pendientes y 4 nuevas, y ninguna de las dos frases de nivel 4 y 5

#### Scenario: Una pendiente de nivel CEFR inferior o superior no entra

- **GIVEN** una frase pendiente de nivel CEFR A1, otra pendiente de nivel CEFR C1 y otra de nivel CEFR C1 con
  `dueAtSession` posterior a la sesión actual, una frase nueva B1 N1, con el nivel elegido B1
- **WHEN** se compone la tanda
- **THEN** la tanda es solo N1

#### Scenario: Una pendiente del nivel CEFR elegido entra

- **GIVEN** dos frases pendientes de nivel CEFR A1, con el nivel elegido A1
- **WHEN** se compone la tanda
- **THEN** la tanda contiene ambas

#### Scenario: Sin material del nivel elegido la tanda sale más pequeña

- **GIVEN** todas las frases con progreso son de un nivel CEFR distinto al elegido, salvo dos pendientes del nivel
  elegido, sin frases nuevas del nivel elegido, con N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda tiene exactamente esas dos frases

### Requirement: Número de frases nuevas según el aprendizaje

Con `hueco = floor(N × 0,5) − frases de nivel 0 con progreso del nivel CEFR elegido, estén o no pendientes`, las
nuevas dentro del límite MUST ser el menor entre: las nuevas disponibles; un techo de `floor(N × 0,4)`, o de
`floor(N × 0,5)` si no hay pendientes; y el mayor entre `hueco` (mínimo 0) y `N − pendientes`. Tras las
pendientes, esas nuevas y el repaso adelantado, si aún falta para N, MUST entrar más nuevas en orden de catálogo.

#### Scenario: Primera tanda de un nivel CEFR se llena hasta la mitad de N

> El título es el heredado (OpenSpec no permite renombrar un escenario dentro de MODIFIED). La mitad de N es solo
> el límite de las nuevas dentro del límite; sin repaso adelantado, más nuevas llenan la tanda hasta N (ADR-0026).

- **GIVEN** ningún progreso previo, un catálogo de treinta frases del nivel elegido y N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las 20 primeras frases en orden de catálogo, con nivel 0 y racha 0

#### Scenario: Pocas pendientes admiten hasta el 40 % de N en nuevas

- **GIVEN** N igual a 10, tres pendientes de nivel 1, ninguna frase de nivel 0 con progreso, cinco frases de
  nivel 2 con `dueAtSession` posterior a la sesión actual y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 pendientes, 4 nuevas y 3 frases de repaso adelantado, 10 en total

#### Scenario: Sin repaso adelantado las nuevas completan la tanda

- **GIVEN** N igual a 10, tres pendientes de nivel 1, ninguna otra frase con progreso del nivel elegido y veinte
  frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 pendientes y 7 nuevas

#### Scenario: Las nuevas completan la tanda cuando se agota el repaso adelantado

- **GIVEN** N igual a 10, tres pendientes de nivel 1, ninguna frase de nivel 0 con progreso, dos frases de nivel
  2 con `dueAtSession` posterior a la sesión actual y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 pendientes, 2 frases de repaso adelantado y 5 nuevas

#### Scenario: Aprendizaje saturado no admite nuevas cuando las pendientes llenan la tanda

- **GIVEN** N igual a 10, doce pendientes de las que cinco son de nivel 0, y frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 10 pendientes y ninguna nueva

#### Scenario: Aprendizaje con hueco admite nuevas aunque las pendientes llenen la tanda

- **GIVEN** N igual a 10, doce pendientes de las que dos son de nivel 0, y frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 nuevas y 7 pendientes

#### Scenario: Aprendizaje consolidado admite el máximo de nuevas

- **GIVEN** N igual a 10, doce pendientes de nivel 1 o superior, ninguna de nivel 0, y frases nuevas
  disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 4 nuevas y 6 pendientes

#### Scenario: Cálculo de los límites con un N no divisible exactamente

- **GIVEN** N igual a 17, sin pendientes ni frases de nivel 0 con progreso, veinte frases de nivel 2 con
  `dueAtSession` posterior a la sesión actual y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 8 nuevas (`floor(17 × 0,5)`) y 9 frases de repaso adelantado

#### Scenario: Con pendientes y N igual a 17 el techo es el 40 %

- **GIVEN** N igual a 17, una pendiente de nivel 2, ninguna frase de nivel 0 con progreso, veinte frases de nivel
  3 con `dueAtSession` posterior a la sesión actual y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 6 nuevas (`floor(17 × 0,4)`), 1 pendiente y 10 frases de repaso adelantado

### Requirement: Reparto de las nuevas en la tanda

Con `p` frases no nuevas (pendientes más repaso adelantado) y `k` nuevas en la tanda, si `p` es mayor que 0, la
nueva `i`-ésima (empezando en 0, en orden de catálogo) MUST ocupar la posición `floor((i + 1) × (p + k) / (k + 1))`
(empezando en 0). Las no nuevas MUST ocupar el resto de posiciones: primero las pendientes en su orden de
prioridad y después el repaso adelantado en su orden. Sin no nuevas, las nuevas MUST ir en orden de catálogo.

#### Scenario: Dos nuevas en una tanda de diez

- **GIVEN** N igual a 10, ocho pendientes P1 a P8 de nivel 0 con el mismo retraso, en ese orden de catálogo, y
  veinte frases nuevas N1, N2, … disponibles (hueco 0 y `N − pendientes` 2, así que entran 2 nuevas)
- **WHEN** se compone la tanda
- **THEN** el orden es P1, P2, P3, N1, P4, P5, N2, P6, P7, P8

#### Scenario: Cuatro nuevas en una tanda de diez

- **GIVEN** N igual a 10, seis pendientes P1 a P6 de nivel 1 con el mismo retraso, en ese orden de catálogo,
  ninguna de nivel 0, y veinte frases nuevas N1, N2, … disponibles (entran 4 nuevas)
- **WHEN** se compone la tanda
- **THEN** el orden es P1, P2, N1, P3, N2, P4, N3, P5, N4, P6

#### Scenario: Las nuevas se reparten también entre el repaso adelantado

- **GIVEN** N igual a 10, tres pendientes P1 a P3 de nivel 1 con el mismo retraso, ninguna de nivel 0, tres
  frases de repaso adelantado R1 a R3 de nivel 2 con el mismo `dueAtSession`, en ese orden de catálogo, y veinte
  frases nuevas N1, N2, … disponibles (entran 4 nuevas)
- **WHEN** se compone la tanda
- **THEN** el orden es P1, P2, N1, P3, N2, R1, N3, R2, N4, R3

#### Scenario: Con dos o más pendientes las nuevas no quedan todas al final

- **GIVEN** una tanda compuesta con al menos dos frases no nuevas (pendientes o de repaso adelantado) y al menos
  una nueva
- **WHEN** se compone la tanda
- **THEN** la última posición de la tanda la ocupa una frase que no es nueva (una pendiente o, si las hay, una
  frase de repaso adelantado)
