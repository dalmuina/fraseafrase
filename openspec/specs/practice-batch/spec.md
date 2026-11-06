# practice-batch Specification

## Purpose

Define el ciclo de la tanda activa: la lista circular de práctica, la cola de frases arrastradas, la composición de la tanda siguiente y la racha inicial de cada tanda nueva, según ADR-0012. Las reglas de racha, certificación, resolución de cada frase e intervalos están en `spaced-repetition`. Los escenarios de composición fijan siempre catálogo, progreso, nivel CEFR y N: si no se indica nivel CEFR, la frase es A1 y el nivel elegido es A1; si no se indica N, N es 20. El orden de catálogo es solo un desempate para que la composición sea determinista; ADR-0012 no lo fija.

## Requirements

### Requirement: Lista circular de la tanda

La tanda MUST ser una lista circular de frases. Al responder una frase con acierto o fallo, MUST pasar al final de la lista. Ninguna frase MUST desaparecer de la tanda durante la práctica.

#### Scenario: Acierto mueve la frase al final

- **GIVEN** una tanda con las frases A, B y C en ese orden
- **WHEN** el usuario se autoevalúa con acierto en la frase A
- **THEN** la tanda queda en el orden B, C, A y contiene las tres frases

#### Scenario: Fallo mueve la frase al final

- **GIVEN** una tanda con las frases A, B y C en ese orden
- **WHEN** el usuario se autoevalúa con fallo en la frase B
- **THEN** la tanda queda en el orden A, C, B y contiene las tres frases

#### Scenario: Responder la última frase no cambia el orden

- **GIVEN** una tanda con las frases A, B y C en ese orden
- **WHEN** el usuario se autoevalúa en la frase C
- **THEN** la tanda queda en el orden A, B, C

#### Scenario: Varias respuestas mantienen las mismas entradas

- **GIVEN** una tanda con las frases A, B y C en ese orden
- **WHEN** el usuario responde en este orden: A acierto, B fallo, C acierto, A fallo, B acierto
- **THEN** la tanda contiene A, B y C una sola vez cada una, en el orden C, A, B

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

### Requirement: Cola de arrastradas ordenada

Al cerrar la tanda, las frases no certificadas MUST quedar en una cola de arrastradas en el orden de la lista de la tanda, donde la primera frase ocupa la posición 0. Las arrastradas MUST entrar primero en la tanda siguiente, en ese orden. Una arrastrada que no quepa MUST conservar su posición en la cola.

#### Scenario: Arrastradas conservan el orden de la lista al cerrar

- **GIVEN** una tanda en el orden B, C, A (posiciones 0, 1 y 2), donde A y C no están certificadas y B sí
- **WHEN** se cierra la tanda
- **THEN** la cola de arrastradas es C, A

#### Scenario: Arrastrada entra primera en la siguiente tanda

- **GIVEN** una cola sobrante con Z, una pendiente P de nivel 1 con repaso el 09-03-2026, ambas en el catálogo A1 con progreso, y N igual a 10
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene Z en la posición 0 y P en la posición 1

#### Scenario: Arrastrada que no cabe conserva su progreso

- **GIVEN** una cola de arrastradas formada por las frases X1 a X10 seguidas de P, todas en el catálogo A1 con progreso, donde P es de nivel 3 con próximo repaso el 20-03-2026, y N igual a 10
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene X1 a X10, P queda en la posición 0 de la cola sobrante y su progreso (nivel 3, repaso 20-03-2026) no cambia

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

#### Scenario: Nuevas solo si queda hueco

- **GIVEN** cuatro arrastradas, dos pendientes, N igual a 10 y cuatro frases nuevas disponibles del nivel elegido
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene las seis frases de progreso y las cuatro primeras frases nuevas en orden de catálogo

#### Scenario: Frase con repaso futuro no entra antes de su fecha

- **GIVEN** un catálogo con solo la frase P (nivel A1), con progreso de P de nivel 1 y repaso el 11-03-2026, y N igual a 20
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** el resultado es ausencia de tanda

#### Scenario: Frase con repaso de hoy entra en su fecha

- **GIVEN** un catálogo con solo la frase P (nivel A1), con progreso de P de nivel 1 y repaso el 11-03-2026, y N igual a 20
- **WHEN** se compone la tanda el 11-03-2026
- **THEN** la tanda contiene P

#### Scenario: El filtro CEFR no afecta a arrastradas ni pendientes

- **GIVEN** una arrastrada de nivel CEFR B2 y una pendiente de nivel CEFR C1, con el nivel elegido A1 y N igual a 20
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene ambas frases

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

### Requirement: Frase en un único lugar

Una frase MUST estar como máximo en un único lugar: la tanda activa, la cola de arrastradas, las programadas o las nuevas sin progreso. Una frase que sea a la vez arrastrada y pendiente MUST contarse como arrastrada y MUST NOT duplicarse en la tanda compuesta.

#### Scenario: Arrastrada y pendiente aparece una sola vez

- **GIVEN** una frase P de nivel 1 con repaso el 09-03-2026 que está en la cola de arrastradas
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** P aparece una sola vez, en la posición 0 de la tanda

#### Scenario: Frase programada no entra como nueva

- **GIVEN** un catálogo del nivel elegido B1 que contiene solo la frase P, con progreso de nivel 2 y repaso el 13-03-2026, y N igual a 20
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** el resultado es ausencia de tanda

### Requirement: Tamaño de tanda N

El tamaño de la tanda N MUST estar entre 10 y 30 inclusive, con 20 por defecto. Un valor fuera de ese rango MUST rechazarse al construir N. Una tanda MUST conservar el tamaño con el que se creó; un cambio de N MUST aplicarse solo a la siguiente composición.

#### Scenario: Valor por defecto

- **GIVEN** ningún tamaño configurado
- **WHEN** se usa `BatchSize.DEFAULT`
- **THEN** el valor es 20

#### Scenario: Límites inferior y superior se aceptan

- **GIVEN** los valores 10 y 30
- **WHEN** se construye N con cada valor
- **THEN** se acepta cada valor sin error

#### Scenario: Valores fuera de rango se rechazan

- **GIVEN** los valores 9 y 31
- **WHEN** se construye N con cada valor
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Tanda activa conserva su tamaño al cambiar N

- **GIVEN** una tanda activa creada con N igual a 30 y 30 frases, y después el ajuste de N pasa a 10
- **WHEN** se cierra la tanda y se compone la siguiente con el ajuste 10
- **THEN** la tanda cerrada sigue siendo válida con 30 frases y la tanda siguiente tiene como máximo 10 frases

### Requirement: Invariantes de la tanda y de la cola

Las violaciones de estas invariantes MUST lanzar `IllegalArgumentException`, porque son errores de programación. Los resultados esperables no usan excepciones.

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

#### Scenario: Cola de arrastradas con ids duplicados

- **GIVEN** una cola de arrastradas con el id A dos veces
- **WHEN** se construye la cola
- **THEN** se lanza `IllegalArgumentException`

#### Scenario: Composición con una arrastrada sin progreso

- **GIVEN** una cola de arrastradas con el id X y ningún progreso de X
- **WHEN** se compone la tanda
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

#### Scenario: Cierre con un id ya en la cola

- **GIVEN** una cola sobrante con Z y un cierre que arrastra también Z
- **WHEN** se construye la cola de la tanda siguiente
- **THEN** se lanza `IllegalArgumentException`

### Requirement: Composición de la tanda o ausencia de tanda

La composición MUST devolver una tanda `Composed` con hasta N frases cuando haya al menos una frase disponible, aunque sea menor que N. Sin arrastradas, pendientes ni frases nuevas, MUST devolver `NothingAvailable` y no crear tanda. Sin progreso previo y con catálogo disponible, MUST crear la primera tanda. Las entradas nuevas MUST tener nivel 0, sin próximo repaso.

#### Scenario: Primera tanda con catálogo disponible

- **GIVEN** ningún progreso previo, un catálogo de quince frases del nivel elegido y N igual a 20
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con las quince frases en orden de catálogo, con nivel 0, racha 0, sin responder y sin próximo repaso

#### Scenario: Tanda menor que N si faltan frases

- **GIVEN** tres arrastradas en la cola, en el catálogo A1 y con progreso, N igual a 10 y ninguna frase nueva ni pendiente
- **WHEN** se compone la tanda
- **THEN** se crea una tanda con tres frases

#### Scenario: Catálogo vacío sin progreso no crea tanda

- **GIVEN** ningún progreso previo y un catálogo vacío
- **WHEN** se compone la tanda
- **THEN** el resultado es `NothingAvailable` y no se crea tanda

#### Scenario: Catálogo sin frases del nivel elegido no crea tanda

- **GIVEN** ningún progreso previo y un catálogo con solo frases A1, con el nivel elegido B1
- **WHEN** se compone la tanda
- **THEN** el resultado es `NothingAvailable`

#### Scenario: Catálogo agotado sin pendientes no crea tanda

- **GIVEN** un catálogo de tres frases B1, todas con progreso de nivel 2 y repaso el 13-03-2026, y ninguna arrastrada
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** el resultado es `NothingAvailable`

### Requirement: Racha inicial en cada tanda nueva

Cada entrada de una tanda compuesta MUST empezar con racha 0 y con `answeredInBatch` falso.

#### Scenario: Certificada que vuelve como pendiente entra con racha 0

- **GIVEN** una frase certificada de nivel 1 que, tras cerrar la tanda el 10-03-2026, queda con próximo repaso el 11-03-2026
- **WHEN** se compone la tanda el 11-03-2026
- **THEN** la frase entra con racha 0, `answeredInBatch` falso y nivel 1

### Requirement: Frases nuevas por nivel CEFR

Las frases nuevas MUST proceder solo del nivel CEFR elegido por el usuario. Los niveles CEFR válidos MUST ser A1, A2, B1, B2 y C1, en ese orden.

#### Scenario: Niveles CEFR válidos

- **GIVEN** el tipo de nivel CEFR
- **WHEN** se listan sus valores
- **THEN** son A1, A2, B1, B2 y C1, en ese orden

#### Scenario: Solo nuevas del nivel elegido

- **GIVEN** un catálogo con las frases A1-1, A1-2 y B1-1 sin progreso, con el nivel elegido A1 y N igual a 20
- **WHEN** se compone la tanda sin arrastradas ni pendientes
- **THEN** la tanda contiene A1-1 y A1-2, en orden de catálogo

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
