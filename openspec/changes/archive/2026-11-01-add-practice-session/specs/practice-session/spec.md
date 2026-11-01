# Spec Delta

## Purpose

Define la pantalla de práctica de la tanda activa: cómo el usuario ve la frase, revela la traducción, se autoevalúa, cierra la tanda con su aviso y recibe la tanda siguiente, según ADR-0012. Las reglas de composición, cierre y racha están en `practice-batch` y `spaced-repetition`; esta especificación describe lo que el usuario ve y puede hacer.

## ADDED Requirements

### Requirement: Frase en español con traducción oculta

La pantalla MUST mostrar la frase de la tanda activa en español y MUST ocultar la frase en portugués hasta que el usuario la revele. La frase que se muestra MUST ser la primera de la lista de la tanda.

#### Scenario: Al abrir la práctica se muestra la primera frase en español

- **GIVEN** una tanda activa con las frases A, B y C en ese orden, donde A tiene el texto en español "Buenos días" y en portugués "Bom dia"
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra "Buenos días" y no muestra "Bom dia"

#### Scenario: Revelar muestra la traducción en portugués

- **GIVEN** la pantalla muestra la frase A sin revelar
- **WHEN** el usuario revela la frase
- **THEN** la pantalla muestra "Bom dia" junto a "Buenos días" y ofrece acierto y fallo

### Requirement: Autoevaluación tras revelar

El usuario MUST poder autoevaluar la frase revelada como acierto o fallo. Al autoevaluarla, el sistema MUST registrar la respuesta en la tanda una sola vez y MUST mostrar la siguiente frase de la lista ocultando su traducción. No MUST ser posible autoevaluar una frase sin revelarla, ni registrar dos respuestas para la misma revelación.

#### Scenario: Acierto muestra la siguiente frase

- **GIVEN** una tanda con las frases A, B y C en ese orden y la frase A revelada
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la pantalla muestra B sin revelar y la tanda queda en el orden B, C, A

#### Scenario: Fallo muestra la siguiente frase

- **GIVEN** una tanda con las frases A, B y C en ese orden y la frase A revelada
- **WHEN** el usuario se autoevalúa con fallo
- **THEN** la pantalla muestra B sin revelar y la tanda queda en el orden B, C, A

#### Scenario: Sin revelar no hay autoevaluación

- **GIVEN** la pantalla muestra la frase A sin revelar
- **WHEN** el usuario intenta autoevaluarla
- **THEN** la pantalla no ofrece acierto ni fallo y la tanda no cambia

#### Scenario: Doble toque no registra dos respuestas

- **GIVEN** una tanda con las frases A, B y C en ese orden y la frase A revelada
- **WHEN** el usuario se autoevalúa con acierto dos veces seguidas
- **THEN** la racha de A es 1, la tanda queda en el orden B, C, A y la segunda autoevaluación se ignora

#### Scenario: La tanda da la vuelta sin desaparecer frases

- **GIVEN** una tanda con dos frases A y B
- **WHEN** el usuario responde A con acierto y después B con fallo
- **THEN** la pantalla vuelve a mostrar A sin revelar y la tanda sigue conteniendo A y B

### Requirement: Racha y frase sabida visibles

La pantalla MUST mostrar la racha de la frase revelada dentro de la tanda activa. Una frase con racha mayor o igual que 3 MUST marcarse como sabida. Una frase sabida MUST seguir apareciendo en la lista de la tanda.

#### Scenario: Racha visible tras un acierto

- **GIVEN** una frase con racha 1 en la tanda activa
- **WHEN** el usuario la revela
- **THEN** la pantalla muestra la racha 1 de esa frase

#### Scenario: Fallo reinicia la racha visible

- **GIVEN** una tanda con una sola frase A de racha 2
- **WHEN** el usuario revela A y se autoevalúa con fallo
- **THEN** al mostrarse A de nuevo y revelarla, la pantalla muestra racha 0

#### Scenario: Frase con racha 3 marcada como sabida

- **GIVEN** una tanda con las frases A y B, donde A tiene racha 3
- **WHEN** la pantalla muestra A, revelada o sin revelar
- **THEN** la pantalla marca A como sabida y A sigue en la tanda

#### Scenario: Frase con racha 2 no se marca

- **GIVEN** una frase con racha 2 en la tanda activa
- **WHEN** el usuario la revela
- **THEN** la pantalla no la marca como sabida

### Requirement: Aviso de cierre antes de confirmar

Cuando el usuario pide cerrar la tanda, la pantalla MUST mostrar un diálogo modal con el número de frases no certificadas que pasarían a la siguiente tanda, incluido el caso de cero. El usuario MUST poder confirmar el cierre o cancelarlo. Mientras el diálogo esté abierto, MUST NOT ser posible autoevaluar frases. Mientras haya una respuesta en curso, pedir el cierre MUST NOT abrir el diálogo. Cancelar MUST dejar la tanda, las rachas y el progreso sin cambios.

#### Scenario: Aviso con frases no certificadas

- **GIVEN** una tanda de cinco frases, dos con racha 3 y tres sin certificar
- **WHEN** el usuario pide cerrar la tanda
- **THEN** la pantalla muestra el diálogo "3 frases pasarán a la siguiente tanda" y ofrece confirmar o cancelar

#### Scenario: Aviso con una sola frase no certificada

- **GIVEN** una tanda de dos frases, una con racha 3 y otra sin certificar
- **WHEN** el usuario pide cerrar la tanda
- **THEN** la pantalla muestra el diálogo "1 frase pasará a la siguiente tanda"

#### Scenario: Aviso sin frases no certificadas

- **GIVEN** una tanda de dos frases, ambas con racha 3
- **WHEN** el usuario pide cerrar la tanda
- **THEN** la pantalla muestra el diálogo "0 frases pasarán a la siguiente tanda" y ofrece confirmar o cancelar

#### Scenario: Con el aviso abierto no se puede autoevaluar

- **GIVEN** el diálogo de cierre abierto con una frase revelada
- **WHEN** el usuario intenta autoevaluarla
- **THEN** la autoevaluación no se registra y la tanda no cambia

#### Scenario: Cerrar mientras hay una respuesta en curso no abre el aviso

- **GIVEN** una frase revelada cuya autoevaluación se está registrando, todavía sin confirmar en la tanda
- **WHEN** el usuario pide cerrar la tanda
- **THEN** el diálogo de cierre no se muestra y la tanda no cambia

#### Scenario: Cancelar el cierre no cambia nada

- **GIVEN** el aviso de cierre de una tanda con tres frases no certificadas
- **WHEN** el usuario cancela
- **THEN** la pantalla vuelve a la práctica, la tanda conserva su orden y sus rachas y no se cierra

#### Scenario: Confirmar el cierre usa el número del aviso

- **GIVEN** el aviso de cierre que indicó tres frases no certificadas
- **WHEN** el usuario confirma
- **THEN** la tanda se cierra con esa expectativa de tres frases

### Requirement: Tanda siguiente tras confirmar el cierre

Tras un cierre confirmado, la pantalla MUST mostrar la tanda siguiente. Si la tanda siguiente se puede componer, MUST mostrar sus frases en el orden de composición y empezar por la primera. Las frases arrastradas MUST aparecer primero, en su orden de cola.

#### Scenario: Cierre con arrastradas muestra la tanda siguiente

- **GIVEN** una tanda cerrada en la que quedaron arrastradas C y A, en ese orden, sin frases pendientes, un catálogo con 8 frases nuevas del nivel elegido y N igual a 10
- **WHEN** el usuario confirma el cierre
- **THEN** la pantalla muestra la tanda siguiente con C en la primera posición, A en la segunda y las 8 frases nuevas en orden de catálogo a continuación

#### Scenario: Cierre con todas certificadas deja la tanda siguiente sin arrastradas

- **GIVEN** una tanda de dos frases, ambas certificadas, y un catálogo con frases nuevas del nivel elegido
- **WHEN** el usuario confirma el cierre
- **THEN** la pantalla muestra una tanda nueva sin arrastradas, compuesta con frases nuevas del nivel elegido

### Requirement: Primer uso y cambio de día

Al abrir la pantalla sin tanda activa guardada, la pantalla MUST componer la primera tanda con las frases nuevas del nivel elegido. Con una tanda activa guardada, la pantalla MUST mostrar esa tanda aunque cambie la fecha.

#### Scenario: Primer uso muestra la primera frase del catálogo

- **GIVEN** una base de datos vacía, un catálogo de quince frases A1, el nivel elegido A1, N igual a 20 y la fecha 10-03-2026
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra la primera frase del catálogo sin revelar, con racha 0

#### Scenario: Cambio de día con tanda activa no altera la tanda

- **GIVEN** una tanda activa compuesta el 10-03-2026 con cinco frases, y una frase P con repaso el 11-03-2026 fuera de ella
- **WHEN** el usuario abre la pantalla el 11-03-2026
- **THEN** la pantalla muestra las mismas cinco frases y P no aparece en la tanda

### Requirement: Estado vacío sin frases disponibles

Si no hay arrastradas, pendientes ni frases nuevas, la pantalla MUST indicar que no hay frases para practicar y MUST NOT mostrar una tanda. En ese estado no MUST haber ninguna frase visible.

#### Scenario: Catálogo agotado tras cerrar

- **GIVEN** una tanda cerrada y un catálogo sin frases nuevas, sin arrastradas y sin pendientes
- **WHEN** el usuario confirma el cierre
- **THEN** la pantalla indica que no hay frases para practicar y no muestra ninguna frase

#### Scenario: Sin frases disponibles al pedir el cierre

- **GIVEN** la pantalla muestra una tanda, esa tanda ya no está activa y no hay frases disponibles para componer otra
- **WHEN** el usuario pide cerrar la tanda
- **THEN** la pantalla indica que no hay frases para practicar, no muestra el diálogo de cierre y no muestra ninguna frase

#### Scenario: Frases con repaso futuro sin tanda

- **GIVEN** un catálogo cuya única frase P tiene nivel 1 con repaso el 11-03-2026, y la fecha actual es 10-03-2026
- **WHEN** el usuario abre la pantalla de práctica sin tanda activa
- **THEN** la pantalla indica que no hay frases para practicar y no muestra ninguna frase

### Requirement: Error de catálogo

Si el catálogo no se puede cargar por cualquiera de sus motivos de error, la pantalla MUST mostrar un estado de error que indique que no se puede practicar. MUST NOT mostrar frases ni crear tanda.

#### Scenario: Asset de catálogo ilegible

- **GIVEN** un catálogo que no se puede leer
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra un error que indica que no se puede practicar y no muestra frases

#### Scenario: Catálogo ilegible al pedir el cierre

- **GIVEN** la pantalla de práctica con una tanda activa y un catálogo que deja de poder leerse
- **WHEN** el usuario pide cerrar la tanda
- **THEN** la pantalla muestra un error que indica que no se puede practicar, no muestra el diálogo de cierre y no muestra frases

#### Scenario: Documento de catálogo malformado

- **GIVEN** un catálogo cuyo documento no es JSON válido
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra un error que indica que no se puede practicar y no muestra frases

### Requirement: Frase de la tanda sin texto en el catálogo

Si una frase de la tanda activa no existe en el catálogo cargado, la lectura de la pantalla MUST fallar con un error de datos que indique el id de la frase. La pantalla MUST NOT capturar ese error ni mostrar una tanda parcial. Los ids del catálogo son permanentes (ADR-0013), así que este caso es una inconsistencia de datos.

#### Scenario: Frase de la tanda ausente del catálogo

- **GIVEN** una tanda activa que contiene la frase P, y un catálogo que no contiene P
- **WHEN** se lee la sesión de práctica
- **THEN** la lectura falla con un error de datos que indica el id de P y no devuelve ninguna frase

### Requirement: Cierre rechazado por cambio de tanda

Si al confirmar el cierre la tanda cambió desde el aviso o ya no hay tanda activa, la pantalla MUST recargar la tanda activa tal como está guardada y MUST NOT mostrar un error al usuario.

#### Scenario: Aviso desfasado recarga la tanda

- **GIVEN** un aviso de cierre con tres frases no certificadas, y una tanda que ya tiene dos
- **WHEN** el usuario confirma el cierre
- **THEN** la pantalla muestra la tanda activa guardada, sin error, y no la cierra

#### Scenario: Cierre sin tanda activa recarga la pantalla

- **GIVEN** que no hay tanda activa cuando el usuario confirma el cierre
- **WHEN** el usuario confirma
- **THEN** la pantalla obtiene la tanda activa guardada sin mostrar error

### Requirement: Reanudar la tanda activa

Al abrir la pantalla de práctica con una tanda activa guardada, la pantalla MUST mostrar esa misma tanda, en el mismo orden y con las mismas rachas que tenía al salir.

#### Scenario: Reabrir devuelve la misma tanda

- **GIVEN** una tanda activa con las frases A, B y C en el orden B, C, A y la racha de C igual a 2
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla empieza por B y la racha de C al revelarla es 2

### Requirement: Pantalla de práctica como inicio

La aplicación MUST abrir en la pantalla de práctica.

#### Scenario: Arranque muestra la práctica

- **GIVEN** la aplicación se abre
- **WHEN** termina el arranque
- **THEN** la aplicación muestra la pantalla de práctica como primer destino
