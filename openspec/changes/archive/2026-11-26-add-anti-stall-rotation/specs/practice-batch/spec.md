# Spec Delta

## ADDED Requirements

### Requirement: Composición de la tanda por tramos

Tras la primera tanda de un nivel CEFR, la tanda MUST rellenarse por tramos (ADR-0027): 1) falladas por volver;
2) repetidas hasta `ceil(N / 4)`; 3) pendientes; 4) repaso adelantado; 5) resto de frases con progreso; 6) más
nuevas; 7) frases en descanso. Los tramos 1 a 4 MUST parar cuando las no nuevas lleguen a `N − nuevas` y no
añadir una repetida si ya hay `floor(2N / 5)`; los 5 a 7, cuando las no nuevas más las nuevas lleguen a N.

#### Scenario: Una fallada en la sesión anterior entra la primera

- **GIVEN** N igual a 10, sesión actual 20, la frase F de nivel 2 fallada y respondida por última vez en la sesión
  19 con `dueAtSession` 20, doce pendientes P1 a P12 de nivel 0 con `dueAtSession` 20 respondidas por última vez
  en la sesión 17, y frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda es F seguida de P1 a P9, sin nuevas, aunque F tenga peor prioridad que las doce pendientes

#### Scenario: Una fallada que no cupo vuelve en la sesión siguiente

- **GIVEN** N igual a 10, sesión actual 21, la frase F de nivel 2 fallada y respondida por última vez en la sesión
  19 con `dueAtSession` 20, diez pendientes de nivel 0 con `dueAtSession` 21 respondidas por última vez en la
  sesión 17, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** F ocupa la primera posición de la tanda

#### Scenario: Las falladas repetidas no superan el máximo de repetidas

- **GIVEN** N igual a 10, sesión actual 20, seis frases F1 a F6 de nivel 0 falladas y respondidas por última vez
  en la sesión 19 con `dueAtSession` 20, seis pendientes P1 a P6 de nivel 0 con `dueAtSession` 20 respondidas por
  última vez en la sesión 15, todas en ese orden de catálogo, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda es F1, F2, F3, F4, P1, P2, P3, P4, P5, P6; F5 y F6 no entran y conservan su progreso

#### Scenario: Se completan las repetidas hasta el mínimo

- **GIVEN** N igual a 10, sesión actual 20, cinco frases R1 a R5 de nivel 1 respondidas con acierto por última vez
  en la sesión 19 con `dueAtSession` 21, diez pendientes P1 a P10 de nivel 1 con `dueAtSession` 20 respondidas por
  última vez en la sesión 15, en ese orden de catálogo, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda es R1, R2, R3, P1, P2, P3, P4, P5, P6, P7

#### Scenario: Las pendientes repetidas no superan el máximo de repetidas

- **GIVEN** N igual a 10, sesión actual 20, ocho pendientes R1 a R8 de nivel 0 respondidas con acierto por última
  vez en la sesión 19 con `dueAtSession` 20, dos pendientes P1 y P2 de nivel 0 con `dueAtSession` 20 respondidas
  por última vez en la sesión 15, cinco frases Q1 a Q5 de nivel 1 con `dueAtSession` 22 respondidas por última vez
  en la sesión 15, en ese orden de catálogo, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda es R1, R2, R3, R4, P1, P2, Q1, Q2, Q3, Q4

#### Scenario: El máximo de repetidas se supera solo para llenar la tanda

- **GIVEN** N igual a 10, sesión actual 20, nueve pendientes R1 a R9 de nivel 0 respondidas con acierto por
  última vez en la sesión 19 con `dueAtSession` 20, una pendiente P1 de nivel 0 con `dueAtSession` 20 respondida
  por última vez en la sesión 15, en ese orden de catálogo, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda es R1, R2, R3, R4, P1, R5, R6, R7, R8, R9

### Requirement: Frases repetidas y falladas por volver

Con `s` la sesión actual, una frase MUST considerarse repetida si su última sesión respondida es `s − 1`, y
fallada por volver si su última sesión fallada coincide con su última sesión respondida y esta es `s − 2`, `s − 1`
o `s` (el caso `s` se da al recomponer la tanda en la misma sesión por un cambio de ajustes). Una frase sin
registro de respuestas no es ni repetida ni fallada por volver (ADR-0027).

#### Scenario: Una fallada en la sesión actual vuelve al recomponer

- **GIVEN** N igual a 10, sesión actual 20, la frase F de nivel 2 fallada y respondida en la sesión 20 con
  `dueAtSession` 21, diez pendientes de nivel 0 con `dueAtSession` 20 respondidas por última vez en la sesión 15,
  y ninguna frase nueva disponible
- **WHEN** se recompone la tanda en la sesión 20 por un cambio de ajustes
- **THEN** F ocupa la primera posición de la tanda

#### Scenario: Una fallada hace tres sesiones no tiene preferencia

- **GIVEN** N igual a 10, sesión actual 20, la frase F de nivel 3 fallada y respondida por última vez en la sesión
  17 con `dueAtSession` 18 (prioridad 1), diez pendientes de nivel 0 con `dueAtSession` 20 respondidas por
  última vez en la sesión 16, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las diez pendientes y F no entra

#### Scenario: Una fallada que ya se respondió después no tiene preferencia

- **GIVEN** N igual a 10, sesión actual 20, la frase F de nivel 3 fallada en la sesión 17 y respondida con acierto
  en la sesión 18 con `dueAtSession` 21, diez pendientes de nivel 0 con `dueAtSession` 20 respondidas por última
  vez en la sesión 16, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las diez pendientes y F no entra

### Requirement: Frases en descanso

Una frase respondida en la sesión anterior con 6 o más sesiones seguidas está en descanso: MUST no entrar en la
tanda salvo que, tras todas las demás frases con progreso y todas las nuevas disponibles del nivel CEFR elegido,
aún falte para N. Una frase que no se respondió en la sesión anterior MUST no estar en descanso, sean cuales sean
sus sesiones seguidas (ADR-0027).

#### Scenario: Una frase con seis sesiones seguidas descansa

- **GIVEN** N igual a 10, sesión actual 20, la frase D de nivel 0 fallada y respondida por última vez en la sesión
  19 con 6 sesiones seguidas y `dueAtSession` 20, diez pendientes de nivel 0 con `dueAtSession` 20 respondidas por
  última vez en la sesión 15, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las diez pendientes y D no entra

#### Scenario: Una frase con cinco sesiones seguidas puede entrar

- **GIVEN** N igual a 10, sesión actual 20, la frase D de nivel 0 respondida con acierto por última vez en la
  sesión 19 con 5 sesiones seguidas y `dueAtSession` 20, diez pendientes de nivel 0 con `dueAtSession` 20
  respondidas por última vez en la sesión 15, y ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** D entra en la tanda

#### Scenario: Una frase en descanso vuelve tras una sesión sin responderla

- **GIVEN** N igual a 10, sesión actual 21, la frase D de nivel 0 respondida por última vez en la sesión 19 con 6
  sesiones seguidas y `dueAtSession` 20, y ninguna otra frase con progreso ni nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda es D

#### Scenario: Las nuevas entran antes que una frase en descanso

- **GIVEN** N igual a 10, sesión actual 20, la frase D de nivel 0 respondida por última vez en la sesión 19 con 6
  sesiones seguidas y `dueAtSession` 20, tres pendientes de nivel 1 con `dueAtSession` 20 respondidas por última
  vez en la sesión 15, y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las tres pendientes y 7 nuevas, y D no entra

#### Scenario: Una frase en descanso entra si no hay otra para llenar la tanda

- **GIVEN** N igual a 10, sesión actual 20, la frase D de nivel 0 respondida por última vez en la sesión 19 con 6
  sesiones seguidas y `dueAtSession` 20, tres pendientes de nivel 1 respondidas por última vez en la sesión 15, y
  ninguna frase nueva disponible
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las tres pendientes y D, en ese orden

### Requirement: Límite de frases nuevas por tanda

En la primera tanda de un nivel CEFR (ninguna frase del nivel elegido con progreso), las nuevas MUST ser
`min(N, disponibles)`. En las demás, las nuevas de los tramos 1 a 4 MUST ser `min(disponibles, floor(N / 5))`
mientras las frases de nivel 0 con progreso del nivel CEFR elegido sean menos de `N + floor(N / 5)`, y 0 en otro
caso. Solo el tramo 6 MUST añadir más nuevas, en orden de catálogo (ADR-0027).

#### Scenario: Tras la primera tanda entran como mucho N/5 nuevas

- **GIVEN** N igual a 10, doce pendientes de nivel 1, ninguna frase de nivel 0 con progreso y veinte frases nuevas
  disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 2 nuevas y 8 pendientes

#### Scenario: Con N igual a 17 el límite es 3 nuevas

- **GIVEN** N igual a 17, veinte pendientes de nivel 1, ninguna frase de nivel 0 con progreso y veinte frases
  nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 nuevas (`floor(17 / 5)`) y 14 pendientes

#### Scenario: Aprendizaje saturado no admite nuevas

- **GIVEN** N igual a 10, doce pendientes de nivel 0 del nivel CEFR elegido y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 10 pendientes y ninguna nueva

#### Scenario: Con once frases de nivel 0 aún entran nuevas

- **GIVEN** N igual a 10, once pendientes de nivel 0 del nivel CEFR elegido y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 2 nuevas y 8 pendientes

#### Scenario: Las frases de nivel 0 de otro nivel CEFR no cuentan

- **GIVEN** N igual a 10, el nivel elegido A1, doce frases B1 de nivel 0 con progreso, ocho pendientes A1 de nivel
  1 y veinte frases nuevas A1 disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 2 nuevas y las ocho pendientes A1

#### Scenario: Primera tanda de un nivel CEFR con progreso en otro nivel

- **GIVEN** N igual a 10, el nivel elegido B1, progreso solo de frases A1 y quince frases nuevas B1 disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las 10 primeras frases B1 en orden de catálogo, con nivel 0 y racha 0

#### Scenario: El repaso adelantado entra antes que más nuevas

- **GIVEN** N igual a 10, tres pendientes de nivel 1, cinco frases de nivel 2 con `dueAtSession` posterior a la
  sesión actual, ninguna frase de nivel 0 con progreso y veinte frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 pendientes, 5 frases de repaso adelantado y 2 nuevas

#### Scenario: Las nuevas completan la tanda si no hay otras frases

- **GIVEN** N igual a 10, tres pendientes de nivel 1, ninguna otra frase con progreso del nivel elegido y veinte
  frases nuevas disponibles
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 3 pendientes y 7 nuevas

## MODIFIED Requirements

### Requirement: Candidatas a la tanda nueva

La tanda nueva MUST componerse solo con frases del nivel CEFR exactamente igual al elegido (ADR-0021) de tres
tipos: pendientes (con progreso y `dueAtSession` igual o anterior a la sesión actual), nuevas (sin progreso) y
repaso adelantado (con progreso y `dueAtSession` posterior a la sesión actual). Cada tipo MUST entrar en el tramo
que le corresponde según la composición por tramos (ADR-0027); las frases en descanso solo MUST entrar en el
último tramo.

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

- **GIVEN** N igual a 10, sesión actual 20, ocho pendientes de nivel 1, ninguna de nivel 0, veinte frases nuevas
  disponibles, una frase de nivel 5 con `dueAtSession` igual a 30 y una frase de nivel 4 con `dueAtSession`
  igual a 25
- **WHEN** se compone la tanda
- **THEN** la tanda contiene las ocho pendientes y 2 nuevas, y ninguna de las dos frases de nivel 4 y 5

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

### Requirement: Reparto de las nuevas en la tanda

Con `p` frases no nuevas y `k` nuevas en la tanda, si `p` es mayor que 0, la nueva `i`-ésima (empezando en 0, en
orden de catálogo) MUST ocupar la posición `floor((i + 1) × (p + k) / (k + 1))` (empezando en 0). Las no nuevas
MUST ocupar el resto de posiciones en el orden de sus tramos (ADR-0027) y, dentro de cada tramo, en su orden de
prioridad. Sin no nuevas, las nuevas MUST ir en orden de catálogo.

#### Scenario: Dos nuevas en una tanda de diez

- **GIVEN** N igual a 10, ocho pendientes P1 a P8 de nivel 0 con el mismo retraso, en ese orden de catálogo, y
  veinte frases nuevas N1, N2, … disponibles (ocho frases de nivel 0, menos de 12, así que entran 2 nuevas)
- **WHEN** se compone la tanda
- **THEN** el orden es P1, P2, P3, N1, P4, P5, N2, P6, P7, P8

#### Scenario: Cuatro nuevas en una tanda de diez

- **GIVEN** N igual a 10, seis pendientes P1 a P6 de nivel 1 con el mismo retraso, en ese orden de catálogo,
  ninguna otra frase con progreso, y veinte frases nuevas N1, N2, … disponibles (entran 2 nuevas en el límite y 2
  más para llenar la tanda)
- **WHEN** se compone la tanda
- **THEN** el orden es P1, P2, N1, P3, N2, P4, N3, P5, N4, P6

#### Scenario: Las nuevas se reparten también entre el repaso adelantado

- **GIVEN** N igual a 10, tres pendientes P1 a P3 de nivel 1 con el mismo retraso, ninguna de nivel 0, tres
  frases de repaso adelantado R1 a R3 de nivel 2 con el mismo `dueAtSession`, en ese orden de catálogo, ninguna
  otra frase con progreso, y veinte frases nuevas N1, N2, … disponibles (entran 4 nuevas)
- **WHEN** se compone la tanda
- **THEN** el orden es P1, P2, N1, P3, N2, R1, N3, R2, N4, R3

#### Scenario: Con dos o más pendientes las nuevas no quedan todas al final

- **GIVEN** una tanda compuesta con al menos dos frases no nuevas (pendientes o de repaso adelantado) y al menos
  una nueva
- **WHEN** se compone la tanda
- **THEN** la última posición de la tanda la ocupa una frase que no es nueva (una pendiente o, si las hay, una
  frase de repaso adelantado)

### Requirement: Regeneración de la tanda activa por cambio de nivel CEFR sin resolución

Al cambiar el nivel CEFR elegido, el sistema MUST recomponer la tanda activa de inmediato con el nuevo nivel,
sin incrementar el contador de sesiones generadas y sin tocar la racha, el nivel de consolidación, el
`dueAtSession` ni el registro de respuestas de ninguna frase: solo cambia qué frases están activas, a partir del
progreso tal como está en ese momento. El progreso de las frases de un nivel CEFR distinto al recién elegido MUST
conservarse sin cambios y MUST volver a ser candidato a la tanda si el usuario vuelve a elegir ese nivel CEFR más
adelante.

#### Scenario: Cambiar el nivel CEFR no toca racha, nivel ni dueAtSession

- **GIVEN** una tanda activa con una frase de nivel 3 y racha 2, y otra de nivel 1 y racha 0
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** ambas frases conservan su nivel de consolidación, su racha y su `dueAtSession` sin cambios

#### Scenario: Cambiar el nivel CEFR no toca el registro de respuestas

- **GIVEN** una tanda activa en la sesión 9 con una frase respondida y fallada en esa sesión, con 2 sesiones
  seguidas, y otra frase de la tanda aún sin responder en esa sesión, respondida por última vez en la sesión 7
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** la primera frase conserva su última sesión respondida 9, sus 2 sesiones seguidas y su última sesión
  fallada 9, y la segunda conserva su última sesión respondida 7

#### Scenario: Cambiar el nivel CEFR no incrementa el contador de sesiones

- **GIVEN** el contador de sesiones en 9
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** el contador de sesiones sigue en 9

#### Scenario: La tanda siguiente se compone por cuotas con el nuevo nivel

> El título es el heredado del requisito que este delta modifica (OpenSpec no permite renombrar un escenario
> dentro de MODIFIED). El resultado real está en el THEN: la tanda se compone con las mismas reglas que una
> generación de sesión (ADR-0027), sin cuotas ni dominadas.

- **GIVEN** N igual a 20, el nuevo nivel elegido B1, doce pendientes B1 de nivel 1 con el mismo retraso,
  ninguna frase B1 de nivel 0 con progreso y veinte frases nuevas B1
- **WHEN** se compone la tanda siguiente tras el cambio de nivel CEFR
- **THEN** la tanda contiene las 12 pendientes y 8 nuevas (4 del límite y 4 para llenarla), las mismas que daría
  una generación de sesión con ese progreso

#### Scenario: El progreso de un nivel CEFR abandonado se conserva y vuelve a ofrecerse al volver a ese nivel

- **GIVEN** una frase a reforzar de nivel CEFR B1 con nivel de consolidación 3 y racha 2, mientras el nivel
  CEFR elegido es B1
- **WHEN** el usuario cambia el nivel CEFR elegido a A1, practica varias sesiones, y después vuelve a elegir
  B1
- **THEN** la frase de B1 conserva su nivel de consolidación 3 y su racha 2, y vuelve a ser candidata a la
  tanda como pendiente cuando su `dueAtSession` sea igual o anterior a la sesión actual

### Requirement: Prioridad de las pendientes

Las pendientes MUST ordenarse por prioridad `nivel de consolidación − sesiones de retraso`, de menor a
mayor, donde las sesiones de retraso son la sesión actual menos su `dueAtSession`. A igual prioridad, MUST
desempatar el orden de catálogo. Este orden MUST aplicarse dentro de cada tramo de la composición (ADR-0027). Si
no caben todas, MUST entrar las de menor prioridad que el tramo admita (sin superar el máximo de repetidas en los
tramos 1 a 4) y el resto MUST esperar sin cambiar su progreso (ADR-0025, ADR-0027).

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

### Requirement: Orden del repaso adelantado

Las frases de repaso adelantado (con progreso del nivel CEFR elegido y `dueAtSession` posterior a la sesión
actual) MUST ordenarse por `nivel de consolidación + (dueAtSession − sesión actual)`, de menor a mayor. A igual
valor, MUST desempatar el orden de catálogo. Este orden MUST aplicarse dentro de cada tramo de la composición
(ADR-0027). Si no caben todas, MUST entrar las primeras de ese orden que el tramo admita (sin superar el máximo de
repetidas en los tramos 1 a 4) y el resto MUST quedar fuera sin cambiar su progreso (ADR-0026, ADR-0027).

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

## REMOVED Requirements

### Requirement: Número de frases nuevas según el aprendizaje

**Reason**: ADR-0027 sustituye el techo, el hueco y `N − pendientes` de ADR-0025 por un límite fijo de
`floor(N / 5)` nuevas tras la primera tanda, condicionado al número de frases de nivel 0.

**Migration**: Ver el requisito nuevo "Límite de frases nuevas por tanda" y, para el relleno con más nuevas, el
tramo 6 de "Composición de la tanda por tramos".
