# Spec Delta

## REMOVED Requirements

### Requirement: Composición de la tanda nueva por cuotas sobre el progreso

**Reason**: ADR-0025 sustituye las cuotas fijas (20 % nuevas, 55 % a reforzar, resto dominadas) por la
prioridad de pendientes y un número de nuevas que depende del aprendizaje en curso.

**Migration**: ver los requisitos añadidos "Candidatas a la tanda nueva", "Prioridad de las pendientes",
"Número de frases nuevas según el aprendizaje" y "Reparto de las nuevas en la tanda". No hay datos que migrar.

### Requirement: Frases dominadas disponibles para repaso ligero antes de su vencimiento

**Reason**: ADR-0025 elimina el bloque de dominadas: volvía a ofrecer frases de nivel 4 y 5 antes de su
vencimiento, en contra de que los niveles altos aparezcan menos.

**Migration**: una frase de nivel 4 o 5 vuelve a la tanda solo cuando está pendiente, como cualquier otra. Su
progreso no cambia.

## ADDED Requirements

### Requirement: Candidatas a la tanda nueva

La tanda nueva MUST componerse solo con dos tipos de frase del nivel CEFR exactamente igual al elegido
(ADR-0021): pendientes (con progreso y `dueAtSession` igual o anterior a la sesión actual) y nuevas (sin
progreso). Una frase con progreso y `dueAtSession` posterior a la sesión actual MUST NOT entrar, sea cual sea
su nivel de consolidación.

#### Scenario: Una frase pendiente vuelve a estar disponible en su sesión

- **GIVEN** una frase con `dueAtSession` igual a 12, y la sesión actual es 12
- **WHEN** se compone la tanda
- **THEN** la frase entra en la tanda

#### Scenario: Una frase con sesión futura no entra antes de su vencimiento

- **GIVEN** una frase con `dueAtSession` igual a 13, y la sesión actual es 12
- **WHEN** se compone la tanda
- **THEN** la frase no entra en la tanda

#### Scenario: Una frase de nivel alto sin vencer no entra

- **GIVEN** una frase de nivel 5 con `dueAtSession` igual a 30, una frase de nivel 4 con `dueAtSession`
  igual a 25, sesión actual 20, y frases nuevas del nivel elegido disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda no contiene ninguna de las dos frases de nivel 4 y 5

#### Scenario: Una pendiente de nivel CEFR inferior o superior no entra

- **GIVEN** una frase pendiente de nivel CEFR A1 y otra pendiente de nivel CEFR C1, con el nivel elegido B1
- **WHEN** se compone la tanda
- **THEN** la tanda no contiene ninguna de las dos

#### Scenario: Una pendiente del nivel CEFR elegido entra

- **GIVEN** dos frases pendientes de nivel CEFR A1, con el nivel elegido A1
- **WHEN** se compone la tanda
- **THEN** la tanda contiene ambas

#### Scenario: Sin material del nivel elegido la tanda sale más pequeña

- **GIVEN** todas las frases pendientes son de un nivel CEFR distinto al elegido, salvo dos del nivel
  elegido, sin frases nuevas del nivel elegido, con N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda tiene exactamente esas dos frases

### Requirement: Prioridad de las pendientes

Las pendientes MUST ordenarse por prioridad `nivel de consolidación − sesiones de retraso`, de menor a
mayor, donde las sesiones de retraso son la sesión actual menos su `dueAtSession`. A igual prioridad, MUST
desempatar el orden de catálogo. Si hay más pendientes que hueco, MUST entrar las de menor prioridad y el
resto MUST esperar sin cambiar su progreso (ADR-0025).

#### Scenario: Menor nivel va antes a igual retraso

- **GIVEN** sesión actual 20, la frase A de nivel 2 y la frase B de nivel 0, ambas con `dueAtSession` 20, con
  A antes que B en el catálogo
- **WHEN** se compone la tanda
- **THEN** B va antes que A

#### Scenario: El retraso adelanta a una frase de nivel alto

- **GIVEN** sesión actual 20, la frase A de nivel 3 con `dueAtSession` 15 (prioridad −2) y la frase B de
  nivel 0 con `dueAtSession` 20 (prioridad 0), con B antes que A en el catálogo
- **WHEN** se compone la tanda
- **THEN** A va antes que B

#### Scenario: Empate de prioridad se resuelve por catálogo

- **GIVEN** sesión actual 20, la frase A de nivel 4 con `dueAtSession` 16 (prioridad 0) y la frase B de nivel
  0 con `dueAtSession` 20 (prioridad 0), con B antes que A en el catálogo
- **WHEN** se compone la tanda
- **THEN** B va antes que A

#### Scenario: Las pendientes que no caben esperan

- **GIVEN** N igual a 10, diecisiete frases pendientes con el mismo retraso, cinco de nivel 0 y doce de nivel
  1, sin frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 10 pendientes: las cinco de nivel 0 y las cinco primeras de nivel 1 en orden de
  catálogo; las otras siete no entran y conservan su progreso

### Requirement: Número de frases nuevas según el aprendizaje

Con `hueco = floor(N × 0,5) − frases de nivel 0 con progreso del nivel CEFR elegido, estén o no
pendientes`, el número de nuevas MUST ser el menor entre:
las nuevas disponibles; un techo de `floor(N × 0,4)`, o de `floor(N × 0,5)` si no hay pendientes; y el mayor
entre `hueco` (mínimo 0) y `N − pendientes`. Las pendientes MUST llenar el resto hasta N, si las hay.

#### Scenario: Primera tanda de un nivel CEFR se llena hasta la mitad de N

- **GIVEN** ningún progreso previo, un catálogo de quince frases del nivel elegido y N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las 10 primeras frases en orden de catálogo, con nivel 0 y racha 0

#### Scenario: Pocas pendientes admiten hasta el 40 % de N en nuevas

- **GIVEN** N igual a 10, tres pendientes de nivel 1, ninguna frase de nivel 0 con progreso y veinte frases
  nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 pendientes y 4 nuevas, 7 frases en total

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

- **GIVEN** N igual a 17, sin pendientes ni frases de nivel 0 con progreso, y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 8 nuevas (`floor(17 × 0,5)`)

#### Scenario: Con pendientes y N igual a 17 el techo es el 40 %

- **GIVEN** N igual a 17, una pendiente de nivel 2, ninguna frase de nivel 0 con progreso, y veinte frases
  nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 6 nuevas (`floor(17 × 0,4)`) y 1 pendiente

### Requirement: Reparto de las nuevas en la tanda

Con `p` pendientes y `k` nuevas en la tanda, si `p` es mayor que 0, la nueva `i`-ésima (empezando en 0, en
orden de catálogo) MUST ocupar la posición `floor((i + 1) × (p + k) / (k + 1))` (empezando en 0). Las
pendientes MUST ocupar el resto de posiciones en su orden de prioridad. Sin pendientes, las nuevas MUST ir en
orden de catálogo.

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

#### Scenario: Con dos o más pendientes las nuevas no quedan todas al final

- **GIVEN** una tanda compuesta con al menos dos pendientes y al menos una nueva
- **WHEN** se compone la tanda
- **THEN** la última posición de la tanda la ocupa una pendiente

## MODIFIED Requirements

### Requirement: Composición de la tanda o ausencia de tanda

La composición MUST devolver una tanda con hasta N frases cuando haya al menos una frase disponible, aunque
sea menor que N. Sin pendientes ni frases nuevas disponibles del nivel elegido, MUST devolver ausencia de
tanda. Sin progreso previo y con catálogo disponible, MUST crear la primera tanda. Las entradas nuevas MUST
tener nivel 0 y racha 0.

#### Scenario: Primera tanda con catálogo disponible

- **GIVEN** ningún progreso previo, un catálogo de quince frases del nivel elegido y N igual a 20
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con las 10 primeras frases en orden de catálogo, con nivel 0 y racha 0

#### Scenario: Tanda menor que N si faltan frases

- **GIVEN** tres frases pendientes disponibles, N igual a 10 y ninguna frase nueva
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

- **GIVEN** un catálogo de tres frases B1, todas con progreso de nivel 2 y `dueAtSession` en el futuro, y
  ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** el resultado es ausencia de tanda

### Requirement: Frases nuevas por nivel CEFR

Las frases nuevas MUST proceder solo del nivel CEFR elegido por el usuario. Los niveles CEFR válidos MUST ser A1, A2, B1, B2 y C1, en ese orden.

#### Scenario: Niveles CEFR válidos

- **GIVEN** el tipo de nivel CEFR
- **WHEN** se listan sus valores
- **THEN** son A1, A2, B1, B2 y C1, en ese orden

#### Scenario: Solo nuevas del nivel elegido

- **GIVEN** un catálogo con las frases A1-1, A1-2 y B1-1 sin progreso, con el nivel elegido A1 y N igual a 20
- **WHEN** se compone la tanda sin frases pendientes
- **THEN** la tanda contiene A1-1 y A1-2, en orden de catálogo

### Requirement: Generar sesión avanza el contador y compone

Generar sesión MUST incrementar el contador de sesiones generadas y MUST componer inmediatamente la tanda
siguiente (ADR-0025) a partir del progreso ya resuelto (cada frase se resuelve en su propia autoevaluación,
`spaced-repetition`), en un único paso, sin aviso previo ni confirmación.

#### Scenario: Generar sesión incrementa el contador y compone en un paso

- **GIVEN** una tanda completa con el contador de sesiones en 9
- **WHEN** el usuario genera sesión
- **THEN** el contador de sesiones pasa a 10, y la tanda siguiente ya está compuesta con ese valor como sesión
  actual, disponible sin otra lectura adicional

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

> El título es el heredado del requisito que este delta modifica (OpenSpec no permite renombrar un escenario
> dentro de MODIFIED). El resultado real está en el THEN: la tanda se compone con las mismas reglas que una
> generación de sesión (ADR-0025), sin cuotas ni dominadas.

- **GIVEN** N igual a 20, el nuevo nivel elegido B1, doce pendientes B1 de nivel 1 con el mismo retraso,
  ninguna frase B1 de nivel 0 con progreso y veinte frases nuevas B1
- **WHEN** se compone la tanda siguiente tras el cambio de nivel CEFR
- **THEN** la tanda contiene 8 nuevas y 12 pendientes, las mismas que daría una generación de sesión con ese
  progreso

#### Scenario: El progreso de un nivel CEFR abandonado se conserva y vuelve a ofrecerse al volver a ese nivel

- **GIVEN** una frase a reforzar de nivel CEFR B1 con nivel de consolidación 3 y racha 2, mientras el nivel
  CEFR elegido es B1
- **WHEN** el usuario cambia el nivel CEFR elegido a A1, practica varias sesiones, y después vuelve a elegir
  B1
- **THEN** la frase de B1 conserva su nivel de consolidación 3 y su racha 2, y vuelve a ser candidata a la
  tanda como pendiente cuando su `dueAtSession` sea igual o anterior a la sesión actual
