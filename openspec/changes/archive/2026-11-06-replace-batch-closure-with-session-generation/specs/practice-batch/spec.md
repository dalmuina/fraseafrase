# Spec Delta

## REMOVED Requirements

### Requirement: Aviso previo al cierre

**Reason**: el aviso con el número de no certificadas se sustituye por el mensaje de tanda completada de
`practice-session`, que no depende de un número calculado antes de generar la sesión siguiente.
**Migration**: ver el requisito "Generación de sesión" de este delta.

### Requirement: Ciclo de cierre y composición

**Reason**: cerrar la tanda y componer la siguiente dejan de ser dos pasos (cierre explícito, composición
perezosa en la próxima lectura): "generar sesión" los une en un único paso.
**Migration**: ver el requisito "Generación de sesión" de este delta, que conserva la misma regla de orden de la
cola (sobrante antes que arrastradas del cierre).

### Requirement: Composición de la tanda nueva por prioridad

**Reason**: se sustituye por una composición con cuotas fijas de nuevas, a reforzar y dominadas, para garantizar
que cada tanda incluya los tres tipos en vez de llenarse solo por prioridad estricta.
**Migration**: ver los requisitos "Composición de la tanda nueva por cuotas" y "Frases dominadas disponibles
para repaso ligero" de este delta. El orden interno arrastradas-antes-que-pendientes se conserva dentro del
bloque "a reforzar".

## ADDED Requirements

### Requirement: Composición de la tanda nueva por cuotas

La tanda nueva MUST llenarse hasta N frases en tres bloques con cuota propia: nuevas, a reforzar (arrastradas y
pendientes) y dominadas. Si un bloque no alcanza su cuota, el hueco MUST repartirse entre los demás bloques con
candidatos disponibles, en el orden a reforzar, nuevas, dominadas. El nivel CEFR elegido sigue filtrando solo
las frases nuevas.

#### Scenario: Cuotas por defecto con material suficiente

- **GIVEN** una cola de 3 arrastradas, 10 pendientes, 6 frases dominadas disponibles y nuevas suficientes del
  nivel elegido, con N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 8 frases nuevas, 8 a reforzar (las 3 arrastradas seguidas de 5 pendientes) y 4
  dominadas

#### Scenario: Reforzar corto cede su hueco a nuevas

- **GIVEN** un bloque a reforzar con solo 5 candidatos, nuevas y dominadas con material de sobra, y N igual a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 5 a reforzar, 11 nuevas y 4 dominadas

#### Scenario: Dominadas corto cede su hueco a reforzar

- **GIVEN** un bloque de dominadas con solo 1 candidato, a reforzar y nuevas con material de sobra, y N igual
  a 20
- **WHEN** se compone la tanda
- **THEN** la tanda contiene 11 a reforzar, 8 nuevas y 1 dominada

#### Scenario: Arrastradas siguen antes que pendientes dentro de a reforzar

- **GIVEN** una cola de arrastradas con la frase X y una pendiente P de nivel 1 con repaso el 09-03-2026, y N
  igual a 20
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** X aparece antes que P en la tanda

#### Scenario: Pendientes por nivel ascendente dentro de a reforzar

- **GIVEN** dos pendientes: P1 de nivel 3 con repaso el 01-03-2026 y P2 de nivel 1 con repaso el 09-03-2026
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene P2 antes que P1

#### Scenario: A igual nivel, la fecha de repaso más antigua primero

- **GIVEN** dos pendientes de nivel 2: P1 con repaso el 08-03-2026 y P2 con repaso el 05-03-2026
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene P2 antes que P1

#### Scenario: A igual nivel y fecha, orden de catálogo

- **GIVEN** dos pendientes de nivel 2 con repaso el 05-03-2026, P2 en orden de catálogo antes que P1
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene P2 antes que P1

#### Scenario: Cálculo de las cuotas con N igual a 20

- **GIVEN** N igual a 20
- **WHEN** se calculan las cuotas de los tres bloques
- **THEN** la cuota de nuevas es 8, la de a reforzar es 8 y la de dominadas es 4

### Requirement: Frases dominadas disponibles para repaso ligero

Una frase MUST considerarse dominada si su nivel de consolidación es 4 o 5. El bloque de dominadas MUST poder
incluir una frase dominada aunque su próximo repaso sea posterior a hoy. Una frase dominada que además sea
arrastrada o pendiente hoy MUST contarse en el bloque a reforzar, no en el de dominadas.

#### Scenario: Dominada sin repaso pendiente entra en el bloque de dominadas

- **GIVEN** una frase de nivel 5 con próximo repaso el 20-03-2026, sin estar en la cola de arrastradas
- **WHEN** se compone la tanda el 10-03-2026 con hueco en el bloque de dominadas
- **THEN** la frase entra en la tanda dentro del bloque de dominadas

#### Scenario: Dominada pendiente hoy cuenta como a reforzar

- **GIVEN** una frase de nivel 4 con próximo repaso el 10-03-2026
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la frase entra en el bloque a reforzar, no en el de dominadas

### Requirement: Generación de sesión

Generar sesión MUST resolver la tanda activa con la misma regla que cerrarla (certificadas suben de nivel,
respondidas no certificadas se arrastran, nunca respondidas se arrastran) y MUST componer inmediatamente la
tanda siguiente por cuotas, en un único paso, sin aviso previo ni confirmación. La cola de la tanda siguiente
MUST ser la cola sobrante seguida de las arrastradas de este cierre, en ese orden.

#### Scenario: Generar sesión resuelve y compone en un paso

- **GIVEN** una tanda de cinco frases, dos certificadas y tres no certificadas, sin cola sobrante
- **WHEN** el usuario genera sesión
- **THEN** las dos certificadas suben de nivel, las tres no certificadas quedan en la cola de arrastradas en su
  orden de tanda, y la tanda siguiente ya está compuesta y disponible sin otra lectura adicional

#### Scenario: La cola sobrante va antes que las arrastradas de este cierre

- **GIVEN** una cola sobrante con Z y una tanda en el orden B, C, A, donde A y C no están certificadas y B sí
- **WHEN** el usuario genera sesión
- **THEN** la cola de la tanda siguiente es Z, C, A

### Requirement: Tanda completa

Una tanda activa MUST considerarse completa cuando todas sus frases tienen `answeredInBatch` a verdadero, es
decir, el usuario ha dado una vuelta completa a la lista circular. Calcular si una tanda está completa MUST NOT
modificar la tanda, sus rachas ni el progreso.

#### Scenario: Tanda completa tras responder a todas sus frases

- **GIVEN** una tanda de tres frases A, B y C, ninguna respondida todavía
- **WHEN** el usuario responde A, B y C, en ese orden
- **THEN** la tanda está completa

#### Scenario: Tanda incompleta con una frase sin responder

- **GIVEN** una tanda de tres frases A, B y C, donde A y B ya fueron respondidas y C no
- **WHEN** se evalúa si la tanda está completa
- **THEN** la tanda no está completa

### Requirement: Regeneración de la tanda activa por cambio de nivel CEFR

Al cambiar el nivel CEFR elegido, el sistema MUST recomponer la tanda activa de inmediato con el nuevo nivel,
sin ejecutar la resolución de cierre: ninguna frase de la tanda activa MUST certificarse, subir ni bajar de
nivel de consolidación, ni cambiar su próximo repaso por esta recomposición.

#### Scenario: Cambiar el nivel CEFR no certifica ni cambia niveles

- **GIVEN** una tanda activa con una frase de nivel 3 y racha 3, y otra de nivel 1 y racha 0
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** ambas frases conservan su nivel de consolidación y su próximo repaso sin cambios

#### Scenario: Las frases de la tanda activa pasan a arrastradas con el nuevo nivel

- **GIVEN** una tanda activa en el orden B, C, A y un catálogo con frases nuevas del nuevo nivel elegido
- **WHEN** el usuario cambia el nivel CEFR elegido
- **THEN** la tanda siguiente empieza por B, C y A, seguidas de frases nuevas del nuevo nivel, delante de
  cualquier cola sobrante que ya existiera

#### Scenario: La tanda siguiente se compone por cuotas con el nuevo nivel

- **GIVEN** una tanda activa recién sustituida por el cambio de nivel CEFR, con material suficiente de
  arrastradas, pendientes, dominadas y nuevas del nuevo nivel
- **WHEN** se compone la tanda siguiente
- **THEN** se compone con las mismas cuotas de nuevas, a reforzar y dominadas que una generación de sesión
