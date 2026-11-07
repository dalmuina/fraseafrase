# Spec Delta

## REMOVED Requirements

### Requirement: Cola de arrastradas ordenada

**Reason**: con la racha persistente y `dueAtSession` por frase (`spaced-repetition`), ya no hace falta una
cola de arrastradas independiente: el propio progreso de cada frase basta para decidir si debe reaparecer.
**Migration**: ver "Composición de la tanda nueva por cuotas" de este delta; una frase vuelve a estar
disponible cuando su `dueAtSession` es igual o anterior a la sesión actual, sin necesidad de encolarla.

### Requirement: Frase en un único lugar

**Reason**: sin cola de arrastradas, una frase ya no puede estar simultáneamente "arrastrada" y "pendiente":
son el mismo estado (`dueAtSession` vencido). Los bloques de la composición son mutuamente excluyentes por
construcción (ver "Composición de la tanda nueva por cuotas"), no por una regla aparte de deduplicación.
**Migration**: ninguna; no hay un caso equivalente que especificar.

### Requirement: Invariantes de la tanda y de la cola

**Reason**: sin cola de arrastradas, las invariantes específicas de la cola (ids duplicados en la cola,
composición con una arrastrada sin progreso, cierre con un id ya en la cola) dejan de aplicar.
**Migration**: ver "Invariantes de la tanda" de este delta, con las invariantes que sí siguen aplicando.

### Requirement: Racha inicial en cada tanda nueva

**Reason**: la racha ya no se reinicia por entrar en una tanda recién compuesta; persiste hasta un fallo real
(`spaced-repetition`, "Racha persistente entre sesiones").
**Migration**: ninguna; el comportamiento contrario es ahora el especificado en `spaced-repetition`.

### Requirement: Generación de sesión

**Reason**: generar sesión deja de "resolver la tanda con la misma regla que cerrarla" (cada frase ya se
resolvió al instante de su autoevaluación, `spaced-repetition`) y pasa a incrementar el contador de sesiones
generadas.
**Migration**: ver "Generación de sesión" bajo ADDED en este mismo delta.

### Requirement: Composición de la tanda nueva por cuotas

**Reason**: las cuotas cambian de 40/40/20 a 20/55/25, los candidatos a reforzar y dominadas pasan de
calcularse por fecha y cola a calcularse por `dueAtSession`, y el filtro CEFR se extiende a esos dos bloques.
**Migration**: ver "Composición de la tanda nueva por cuotas" bajo ADDED en este mismo delta.

### Requirement: Frases dominadas disponibles para repaso ligero

**Reason**: el criterio de "sin vencer" pasa de la fecha de próximo repaso a `dueAtSession`.
**Migration**: ver "Frases dominadas disponibles para repaso ligero" bajo ADDED en este mismo delta.

### Requirement: Regeneración de la tanda activa por cambio de nivel CEFR

**Reason**: ya no hay cola de arrastradas que mover ni resolución de cierre que evitar: recomponer
simplemente lee el progreso tal cual está, sin incrementar el contador de sesiones.
**Migration**: ver "Regeneración de la tanda activa por cambio de nivel CEFR" bajo ADDED en este mismo delta.

## ADDED Requirements

### Requirement: Generar sesión avanza el contador y compone

Generar sesión MUST incrementar el contador de sesiones generadas y MUST componer inmediatamente la tanda
siguiente por cuotas a partir del progreso ya resuelto (cada frase se resuelve en su propia autoevaluación,
`spaced-repetition`), en un único paso, sin aviso previo ni confirmación.

#### Scenario: Generar sesión incrementa el contador y compone en un paso

- **GIVEN** una tanda completa con el contador de sesiones en 9
- **WHEN** el usuario genera sesión
- **THEN** el contador de sesiones pasa a 10, y la tanda siguiente ya está compuesta con ese valor como sesión
  actual, disponible sin otra lectura adicional

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
- **THEN** la tanda contiene ambas

#### Scenario: Sin material del nivel elegido o superior la tanda sale más pequeña

- **GIVEN** todas las frases a reforzar y dominadas disponibles son de un nivel CEFR inferior al elegido, y no
  hay frases nuevas del nivel elegido, con N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda tiene menos de 20 frases, sin incluir ninguna de nivel inferior al elegido

### Requirement: Frases dominadas disponibles para repaso ligero antes de su vencimiento

Una frase MUST considerarse dominada si su nivel de consolidación es 4 o 5. El bloque de dominadas MUST poder
incluir una frase dominada aunque su `dueAtSession` sea posterior a la sesión actual (repaso ligero antes de
que venza su enfriamiento). Una frase dominada cuyo `dueAtSession` ya venció MUST contarse en el bloque a
reforzar, no en el de dominadas.

#### Scenario: Dominada sin vencer entra en el bloque de dominadas

- **GIVEN** una frase de nivel 5 con `dueAtSession` igual a 30, sesión actual 20
- **WHEN** se compone la tanda con hueco en el bloque de dominadas
- **THEN** la frase entra en la tanda dentro del bloque de dominadas

#### Scenario: Dominada vencida cuenta como a reforzar

- **GIVEN** una frase de nivel 4 con `dueAtSession` igual a 20, sesión actual 20
- **WHEN** se compone la tanda
- **THEN** la frase entra en el bloque a reforzar, no en el de dominadas

### Requirement: Invariantes de la tanda

Las violaciones de estas invariantes MUST lanzar `IllegalArgumentException`, porque son errores de
programación. Los resultados esperables no usan excepciones.

#### Scenario: Responder una frase que no está en la tanda

- **GIVEN** una tanda con las frases A y B
- **WHEN** se responde la frase C
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Tanda con ids duplicados

- **GIVEN** dos entradas de la tanda con el mismo id A
- **WHEN** se construye la tanda
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Tanda con más frases que su tamaño

- **GIVEN** una tanda creada con N igual a 10 y 11 frases
- **WHEN** se construye la tanda
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Progreso sin entrada en el catálogo

- **GIVEN** un progreso de la frase P sin entrada en el catálogo
- **WHEN** se compone la tanda
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Catálogo con ids duplicados

- **GIVEN** un catálogo que contiene la frase P dos veces
- **WHEN** se compone la tanda
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Progreso con ids duplicados

- **GIVEN** dos progresos de la frase P
- **WHEN** se compone la tanda
- **THEN** se lanza `IllegalArgumentException`

### Requirement: Regeneración de la tanda activa por cambio de nivel CEFR sin resolución

Al cambiar el nivel CEFR elegido, el sistema MUST recomponer la tanda activa de inmediato con el nuevo nivel,
sin incrementar el contador de sesiones generadas y sin tocar la racha, el nivel de consolidación ni el
`dueAtSession` de ninguna frase: solo cambia qué frases están activas, a partir del progreso tal como está en
ese momento.

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

## MODIFIED Requirements

### Requirement: Composición de la tanda o ausencia de tanda

La composición MUST devolver una tanda con hasta N frases cuando haya al menos una frase disponible, aunque
sea menor que N. Sin a reforzar, dominadas ni frases nuevas disponibles del nivel elegido o superior, MUST
devolver ausencia de tanda. Sin progreso previo y con catálogo disponible, MUST crear la primera tanda. Las
entradas nuevas MUST tener nivel 0 y racha 0.

#### Scenario: Primera tanda con catálogo disponible

- **GIVEN** ningún progreso previo, un catálogo de quince frases del nivel elegido y N igual a 20
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con las quince frases en orden de catálogo, con nivel 0 y racha 0

#### Scenario: Tanda menor que N si faltan frases

- **GIVEN** tres frases a reforzar disponibles, N igual a 10 y ninguna frase nueva ni dominada
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
