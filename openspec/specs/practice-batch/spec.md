# practice-batch Specification

## Purpose

Define el ciclo de la tanda activa: la lista circular de práctica, el aviso previo al cierre, la cola de frases arrastradas, la composición de la tanda siguiente y la racha inicial de cada tanda nueva, según ADR-0012. Las reglas de racha, certificación, resolución de cada frase e intervalos están en `spaced-repetition`. Los escenarios de composición fijan siempre catálogo, progreso, nivel CEFR y N: si no se indica nivel CEFR, la frase es A1 y el nivel elegido es A1; si no se indica N, N es 20. El orden de catálogo es solo un desempate para que la composición sea determinista; ADR-0012 no lo fija.

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

### Requirement: Aviso previo al cierre

Antes de cerrar la tanda, el sistema MUST informar del número de frases no certificadas que pasarían a la siguiente tanda. Ese número MUST calcularse sobre el progreso antes de resolver el cierre. Calcular el aviso MUST NOT modificar la tanda, sus rachas ni el progreso. Una frase nunca respondida MUST contar como no certificada.

#### Scenario: Aviso con frases no certificadas

- **GIVEN** una tanda de cinco frases, dos de ellas con racha 3 y tres sin certificar
- **WHEN** el usuario solicita cerrar la tanda
- **THEN** el aviso indica que tres frases pasarían a la siguiente tanda

#### Scenario: Aviso sin frases no certificadas

- **GIVEN** una tanda de dos frases, ambas con racha 3
- **WHEN** el usuario solicita cerrar la tanda
- **THEN** el aviso indica que cero frases pasarían a la siguiente tanda

#### Scenario: Frase nunca respondida cuenta como no certificada

- **GIVEN** una tanda de dos frases, una certificada y otra nunca respondida
- **WHEN** el usuario solicita cerrar la tanda
- **THEN** el aviso indica que una frase pasaría a la siguiente tanda

#### Scenario: Calcular el aviso no modifica la tanda

- **GIVEN** una tanda de tres frases no certificadas con rachas 1, 2 y 0
- **WHEN** se calcula el aviso
- **THEN** el aviso indica tres frases y la tanda, sus rachas y el progreso quedan iguales a los de antes del cálculo

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

### Requirement: Composición de la tanda nueva por prioridad

La tanda nueva MUST llenarse hasta N frases en este orden: primero las arrastradas en su orden de cola; después las pendientes de la definición de `spaced-repetition`, por nivel ascendente, a igual nivel por fecha de repaso más antigua y después por orden de catálogo; por último las frases nuevas del nivel CEFR elegido, en orden de catálogo, solo si queda hueco. El nivel CEFR filtra solo las frases nuevas.

#### Scenario: Arrastradas van antes que pendientes

- **GIVEN** una cola de arrastradas con la frase X y una frase pendiente P de nivel 1 con repaso el 09-03-2026, y N igual a 20
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene X antes que P

#### Scenario: Pendientes por nivel ascendente

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

#### Scenario: Nuevas solo si queda hueco

- **GIVEN** cuatro arrastradas, dos pendientes, N igual a 10 y cuatro frases nuevas disponibles del nivel elegido
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene las seis frases de progreso y las cuatro primeras frases nuevas en orden de catálogo

#### Scenario: Sin nuevas mientras queden pendientes sin caber

- **GIVEN** doce pendientes de nivel 1 con repaso el 09-03-2026, identificadas P01 a P12 en orden de catálogo, N igual a 10 y frases nuevas disponibles del nivel elegido
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda contiene P01 a P10 y ninguna frase nueva

#### Scenario: Pendientes que no caben esperan sin perder nivel

- **GIVEN** las doce pendientes del escenario anterior
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** P11 y P12 conservan nivel 1 y repaso el 09-03-2026, y siguen pendientes

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

### Requirement: Ciclo de cierre y composición

Al cerrar la tanda, la cola de la tanda siguiente MUST ser la cola sobrante seguida de las arrastradas del cierre, en ese orden. El número del aviso MUST coincidir con el número de arrastradas del cierre.

#### Scenario: La cola sobrante va antes que las arrastradas del cierre

- **GIVEN** una cola sobrante con Z y una tanda en el orden B, C, A, donde A y C no están certificadas y B sí
- **WHEN** se cierra la tanda
- **THEN** la cola de la tanda siguiente es Z, C, A

#### Scenario: El aviso coincide con las arrastradas del cierre

- **GIVEN** una tanda de cinco frases, dos certificadas y tres no certificadas
- **WHEN** se calcula el aviso y después se cierra la tanda
- **THEN** el aviso indica tres frases y la cola de arrastradas tiene tres frases

#### Scenario: La cola sobrante entra primera tras el cierre

- **GIVEN** la cola Z, C, A, las tres en el catálogo A1 con progreso, y nueve pendientes P1 a P9 de nivel 1 con repaso el 09-03-2026, también en el catálogo A1, con N igual a 10
- **WHEN** se compone la tanda el 10-03-2026
- **THEN** la tanda empieza por Z, C y A, seguidas de P1
