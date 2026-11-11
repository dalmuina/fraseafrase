# practice-session Specification

## Purpose

Define la pantalla de práctica de la tanda activa: cómo el usuario ve la frase, revela la traducción, se autoevalúa, genera la sesión siguiente y recibe la tanda siguiente, según ADR-0012 y ADR-0016. Las reglas de composición, cierre y racha están en `practice-batch` y `spaced-repetition`; esta especificación describe lo que el usuario ve y puede hacer.

## Requirements

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

### Requirement: Mensaje de tanda completada y generación de sesión

Cuando la tanda activa está completa (`practice-batch`), la pantalla MUST mostrar un mensaje positivo con la
opción de generar la siguiente sesión, en vez de seguir mostrando frases para responder. Ese mensaje MUST
mostrar también, de la tanda que se acaba de completar, el número de frases autoevaluadas con acierto, el
número autoevaluadas con fallo y el número de frases cuyo nivel de consolidación subió en algún momento
durante esa tanda, y MUST ofrecer un enlace para ir a la pantalla de Progreso sin generar sesión. Al generar
sesión, la pantalla MUST mostrar la tanda siguiente, empezando por su primera frase; las frases a reforzar
MUST aparecer en orden de catálogo, delante de las nuevas (`practice-batch`). Mientras haya una respuesta en
curso, pedir generar sesión MUST NOT tener efecto.

#### Scenario: Tanda completa muestra el mensaje positivo

- **GIVEN** una tanda de dos frases, ambas ya respondidas al menos una vez
- **WHEN** el usuario autoevalúa la última frase sin responder
- **THEN** la pantalla muestra un mensaje positivo con la opción de generar sesión, en vez de otra frase

#### Scenario: El mensaje muestra el desglose de aciertos, fallos y subidas de nivel

- **GIVEN** una tanda de tres frases, A respondida con acierto sin subir de nivel, B respondida con
  fallo, y C respondida con acierto subiendo de nivel 2 a nivel 3
- **WHEN** el usuario autoevalúa la última frase sin responder y la pantalla muestra el mensaje de
  tanda completada
- **THEN** el mensaje muestra 2 aciertos, 1 fallo y 1 frase que subió de nivel

#### Scenario: Una frase revertida no cuenta en el desglose

- **GIVEN** una tanda de dos frases, A respondida con acierto y después revertida, y B respondida con
  fallo
- **WHEN** la pantalla muestra el mensaje de tanda completada tras responder A de nuevo con acierto
- **THEN** el mensaje muestra 1 acierto y 1 fallo, sin contar la autoevaluación revertida de A

#### Scenario: Una frase que sube y vuelve a bajar de nivel en la misma tanda cuenta como subida

- **GIVEN** una tanda de dos frases, A que sube de nivel 1 a nivel 2 con un acierto y después baja de
  nivel 2 a nivel 1 con un fallo, y B respondida con acierto sin subir de nivel
- **WHEN** la pantalla muestra el mensaje de tanda completada
- **THEN** el mensaje muestra 1 frase que subió de nivel, contando la subida de A aunque su nivel final
  en la tanda sea igual al inicial

#### Scenario: El mensaje ofrece ir a Progreso sin generar sesión

- **GIVEN** la pantalla muestra el mensaje de tanda completada
- **WHEN** el usuario activa el enlace a Progreso
- **THEN** la aplicación muestra la pantalla de Progreso y la tanda activa no cambia

#### Scenario: Generar sesión muestra la tanda siguiente con sus arrastradas primero

> El título es el heredado del requisito que este delta modifica; el resultado real está en el THEN: con
> ADR-0017 ya no hay cola de arrastradas, así que el orden de las frases a reforzar es el de catálogo, no el
> de una cola.

- **GIVEN** el mensaje de tanda completada, con dos frases candidatas a reforzar, A antes que C en el orden
  de catálogo, sin dominadas, un catálogo con 8 frases nuevas del nivel elegido y N igual a 10
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra la tanda siguiente con A en la primera posición, C en la segunda y las 8
  frases nuevas a continuación

#### Scenario: Generar sesión con todas certificadas deja la tanda siguiente sin arrastradas

> El título es el heredado del requisito que este delta modifica: "arrastradas" ya no es un estado posible,
> pero el resultado real (una tanda sin frases a reforzar) sigue siendo válido tal cual.

- **GIVEN** el mensaje de tanda completada de una tanda de dos frases, ambas certificadas con un enfriamiento
  que no ha vencido, y un catálogo con frases nuevas del nivel elegido
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra una tanda nueva sin ninguna frase a reforzar, compuesta con frases nuevas del
  nivel elegido

#### Scenario: Una respuesta en curso ignora la petición de generar sesión

- **GIVEN** una frase revelada cuya autoevaluación se está registrando, todavía sin confirmar en la tanda
- **WHEN** el usuario pide generar sesión
- **THEN** la petición se ignora y la tanda no cambia

#### Scenario: Generar sesión reinicia el desglose para la tanda siguiente

- **GIVEN** la tanda completada con 2 aciertos y 1 fallo, con la pantalla mostrando ese desglose
- **WHEN** el usuario genera sesión y completa la tanda siguiente con 1 acierto y 0 fallos
- **THEN** el nuevo mensaje de tanda completada muestra 1 acierto y 0 fallos, no el desglose de la tanda
  anterior

### Requirement: Aciertos para el próximo nivel visibles

La pantalla MUST mostrar, para la frase revelada o sin revelar, cuántos aciertos consecutivos le faltan para
alcanzar la insignia del siguiente nivel de consolidación: `max(0, 3 - racha)` aciertos para la insignia de
`min(nivel + 1, 5)`. El contador MUST mostrarse de forma literal, sin texto alternativo cuando ya llegó a 3
aciertos en la tanda activa ni cuando el nivel de consolidación ya es el máximo (5).

#### Scenario: Contador visible tras un acierto

- **GIVEN** una frase de nivel 0 con racha 1 en la tanda activa
- **WHEN** el usuario la revela
- **THEN** la pantalla muestra que le faltan 2 aciertos para la insignia "Bronce"

#### Scenario: Fallo reinicia el contador

- **GIVEN** una tanda con la frase A de nivel 2 y racha 2
- **WHEN** el usuario revela A y se autoevalúa con fallo
- **THEN** al mostrarse A de nuevo y revelarla, la pantalla muestra que le faltan 3 aciertos para la insignia
  "Plata", porque el fallo bajó su nivel de consolidación a 1 (ADR-0015) antes de calcular el contador

#### Scenario: Racha de tres sigue mostrando el contador en 0

- **GIVEN** una frase de nivel 1 con racha 3 en la tanda activa
- **WHEN** la pantalla la muestra
- **THEN** la pantalla muestra que le faltan 0 aciertos para la insignia "Plata"

#### Scenario: Nivel máximo sigue mostrando el contador

- **GIVEN** una frase de nivel 5 con racha 3 en la tanda activa
- **WHEN** la pantalla la muestra
- **THEN** la pantalla muestra que le faltan 0 aciertos para la insignia "Diamante"

#### Scenario: El contador cambia con la frase mostrada

- **GIVEN** una tanda con la frase A de nivel 0 y racha 0, y la frase B de nivel 2 y racha 1, mostrando A
- **WHEN** el usuario autoevalúa A y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra que a B le faltan 2 aciertos para la insignia "Oro"

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

Si no hay arrastradas, pendientes ni frases nuevas, la pantalla MUST indicar que no hay frases para practicar y
MUST NOT mostrar una tanda. En ese estado no MUST haber ninguna frase visible.

#### Scenario: Catálogo agotado tras cerrar

- **GIVEN** una tanda generada y un catálogo sin frases nuevas, sin arrastradas y sin pendientes
- **WHEN** el usuario genera sesión
- **THEN** la pantalla indica que no hay frases para practicar y no muestra ninguna frase

#### Scenario: Sin frases disponibles al pedir el cierre

- **GIVEN** la pantalla muestra una tanda, esa tanda ya no está activa y no hay frases disponibles para
  componer otra
- **WHEN** el usuario pide generar sesión
- **THEN** la pantalla indica que no hay frases para practicar, no muestra el mensaje de tanda completada y no
  muestra ninguna frase

#### Scenario: Frases con repaso futuro sin tanda

- **GIVEN** un catálogo cuya única frase P tiene nivel 1 con repaso el 11-03-2026, y la fecha actual es
  10-03-2026
- **WHEN** el usuario abre la pantalla de práctica sin tanda activa
- **THEN** la pantalla indica que no hay frases para practicar y no muestra ninguna frase

### Requirement: Error de catálogo

Si el catálogo no se puede cargar por cualquiera de sus motivos de error, la pantalla MUST mostrar un estado de
error que indique que no se puede practicar. MUST NOT mostrar frases ni crear tanda.

#### Scenario: Asset de catálogo ilegible

- **GIVEN** un catálogo que no se puede leer
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra un error que indica que no se puede practicar y no muestra frases

#### Scenario: Catálogo ilegible al pedir el cierre

- **GIVEN** la pantalla de práctica con una tanda activa y un catálogo que deja de poder leerse
- **WHEN** el usuario pide generar sesión
- **THEN** la pantalla muestra un error que indica que no se puede practicar, no muestra el mensaje de tanda
  completada y no muestra frases

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

### Requirement: Reanudar la tanda activa

Al abrir la pantalla de práctica con una tanda activa guardada, la pantalla MUST mostrar esa misma tanda, en el mismo orden y con las mismas rachas que tenía al salir.

#### Scenario: Reabrir devuelve la misma tanda

- **GIVEN** una tanda activa con las frases A, B y C en el orden B, C, A y la racha de C igual a 2
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla empieza por B y la racha de C al revelarla es 2

### Requirement: Posición en la tanda visible

La pantalla de práctica MUST mostrar la posición de la frase actual dentro de la tanda activa como
"X de Y", donde X es la posición (empezando en 1) de la frase actual en el orden de la tanda e Y es
el número total de frases de esa tanda. El indicador MUST actualizarse cada vez que cambia la frase
mostrada, incluida la vuelta circular de la tanda.

#### Scenario: Primera frase de una tanda de cinco

- **GIVEN** una tanda activa de cinco frases en el orden A, B, C, D, E
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra "1 de 5"

#### Scenario: El indicador avanza tras autoevaluar

- **GIVEN** una tanda de tres frases en el orden A, B, C con A revelada, mostrando "1 de 3"
- **WHEN** el usuario se autoevalúa con acierto
- **THEN** la pantalla muestra B y el indicador "2 de 3"

#### Scenario: El indicador vuelve a 1 al dar la vuelta a la tanda

- **GIVEN** una tanda de dos frases A y B, mostrando B en la posición "2 de 2"
- **WHEN** el usuario autoevalúa B
- **THEN** la pantalla vuelve a mostrar A con el indicador "1 de 2"

#### Scenario: El indicador refleja el tamaño de una tanda siguiente distinto

- **GIVEN** el cierre confirmado de una tanda de cinco frases cuya tanda siguiente tiene ocho frases
- **WHEN** la pantalla muestra la tanda siguiente
- **THEN** el indicador muestra "1 de 8"

### Requirement: Nivel CEFR de la frase visible

La pantalla de práctica MUST mostrar el nivel CEFR (A1, A2, B1, B2 o C1) de la frase actual, revelada
o sin revelar. El nivel mostrado MUST ser el nivel CEFR de esa frase en el catálogo.

#### Scenario: Nivel CEFR visible sin revelar

- **GIVEN** una tanda activa cuya primera frase tiene nivel CEFR B1 en el catálogo
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra "B1" junto a la frase sin revelar

#### Scenario: Nivel CEFR visible al revelar

- **GIVEN** una frase de nivel CEFR A2 sin revelar
- **WHEN** el usuario la revela
- **THEN** la pantalla sigue mostrando "A2"

#### Scenario: El nivel CEFR cambia con la frase mostrada

- **GIVEN** una tanda con la frase A de nivel A1 y la frase B de nivel C1, mostrando A
- **WHEN** el usuario autoevalúa A y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra "C1"

### Requirement: Insignia de nivel de consolidación visible

La pantalla de práctica MUST mostrar la insignia de nivel de consolidación (`level-badges`) de la
frase actual, revelada o sin revelar, junto a su nivel CEFR. La insignia mostrada MUST corresponder
al nivel de consolidación actual de esa frase, incluida la insignia "sin rango" en nivel 0.

#### Scenario: Insignia visible sin revelar

- **GIVEN** una tanda activa cuya primera frase tiene nivel de consolidación 2
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra la insignia "Plata" junto a la frase sin revelar

#### Scenario: Insignia visible al revelar

- **GIVEN** una frase de nivel de consolidación 4 sin revelar
- **WHEN** el usuario la revela
- **THEN** la pantalla sigue mostrando la insignia "Platino"

#### Scenario: Insignia sin rango en nivel 0

- **GIVEN** una frase nueva con nivel de consolidación 0
- **WHEN** la pantalla la muestra
- **THEN** la pantalla muestra la insignia "sin rango" para esa frase

#### Scenario: La insignia cambia con la frase mostrada

- **GIVEN** una tanda con la frase A de nivel de consolidación 1 y la frase B de nivel de
  consolidación 5, mostrando A
- **WHEN** el usuario autoevalúa A y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra la insignia "Diamante"

#### Scenario: Un fallo que baja el nivel actualiza la insignia

- **GIVEN** una frase con nivel de consolidación 3 (insignia "Oro")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** al volver a mostrarse esa frase, la pantalla muestra la insignia "Plata"

### Requirement: Controles de acción siempre visibles y pulsables

La pantalla de práctica MUST mantener siempre visible y pulsable, sin necesidad de ningún
desplazamiento adicional, el control de revelar la traducción cuando la frase no está revelada, o
los controles de acierto y fallo cuando sí lo está. Si el contenido de la frase y su traducción no
cabe en el espacio disponible, la pantalla MUST permitir desplazamiento (scroll) sobre ese
contenido sin desplazar ni ocultar esos controles.

#### Scenario: El control de revelar es visible con una frase muy larga

- **GIVEN** la pantalla de práctica muestra una frase sin revelar cuyo texto ocupa más alto que el
  espacio disponible en pantalla
- **WHEN** la pantalla se muestra
- **THEN** el control de revelar la traducción está visible y es pulsable sin necesitar
  desplazamiento

#### Scenario: Los controles de acierto y fallo son visibles con una traducción muy larga

- **GIVEN** la pantalla de práctica muestra una frase revelada cuya traducción ocupa más alto que el
  espacio disponible en pantalla
- **WHEN** la pantalla se muestra
- **THEN** los controles de acierto y fallo están visibles y son pulsables sin necesitar
  desplazamiento

#### Scenario: La frase y la traducción muy largas se pueden desplazar

- **GIVEN** la pantalla de práctica muestra una frase revelada cuya frase y traducción juntas ocupan
  más alto que el espacio disponible en pantalla
- **WHEN** el usuario desplaza el contenido de la frase y la traducción
- **THEN** el contenido se desplaza y los controles de acierto y fallo permanecen visibles y
  pulsables en el mismo lugar

### Requirement: Indicador animado de cambio de insignia

Al autoevaluar una frase, si su nivel de consolidación resultante es distinto del nivel que tenía
inmediatamente antes de esa autoevaluación, la insignia de nivel (`level-badges`) que la pantalla
muestra junto al nivel CEFR de esa misma frase MUST transicionar visiblemente de la insignia
anterior a la insignia resultante, sin que la pantalla avance todavía a la frase siguiente de la
tanda. La pantalla MUST avanzar automáticamente a la frase siguiente solo al terminar esa
transición, sin que el usuario tenga que intervenir. Si el nivel resultante es igual al nivel
anterior, la pantalla MUST avanzar a la frase siguiente sin esperar ninguna transición.

#### Scenario: Un acierto que certifica muestra el indicador

- **GIVEN** una frase de nivel 2 (insignia "Plata") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la insignia junto al nivel CEFR de esa frase transiciona de "Plata" a "Oro", sin que
  la pantalla avance todavía a la frase siguiente

#### Scenario: Un fallo que baja el nivel muestra el indicador

- **GIVEN** una frase de nivel 3 (insignia "Oro")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la insignia junto al nivel CEFR de esa frase transiciona de "Oro" a "Plata" (ADR-0015),
  sin que la pantalla avance todavía a la frase siguiente

#### Scenario: Un acierto que no certifica no muestra el indicador

- **GIVEN** una frase de nivel 1 (insignia "Bronce") con racha 0
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente

#### Scenario: Certificar de nuevo en nivel máximo no muestra el indicador

- **GIVEN** una frase de nivel 5 (insignia "Diamante") con racha 2
- **WHEN** el usuario la revela y se autoevalúa con acierto
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente, porque el nivel se mantiene en 5 (ADR-0015)

#### Scenario: Un fallo en nivel 0 no muestra el indicador

- **GIVEN** una frase nueva de nivel 0 (insignia "sin rango")
- **WHEN** el usuario la revela y se autoevalúa con fallo
- **THEN** la pantalla no muestra ninguna transición de insignia y avanza de inmediato a la frase
  siguiente, porque el nivel 0 no tiene nivel inferior al que bajar (ADR-0015)

#### Scenario: El indicador desaparece sin intervención del usuario

- **GIVEN** la insignia de la frase respondida transicionando tras una subida de nivel
- **WHEN** transcurre la duración de esa transición sin que el usuario la toque
- **THEN** la pantalla avanza por sí sola a la frase siguiente de la tanda

#### Scenario: La transición no oculta el resto de la tarjeta

- **GIVEN** una autoevaluación que cambia el nivel de consolidación de la frase respondida
- **WHEN** la pantalla muestra la transición de insignia sobre esa frase
- **THEN** la traducción revelada y los controles de acierto/fallo de esa frase siguen presentes
  en pantalla, deshabilitados por estar la respuesta en curso (`busy`)

### Requirement: Estilo distinto según la dirección del cambio de insignia

El indicador de cambio de insignia MUST distinguirse visualmente según la dirección del cambio: una subida
de nivel MUST mostrarse con énfasis (halo alrededor de la insignia) y MUST permanecer visible más tiempo
que una bajada; una bajada de nivel MUST mostrarse sin ese halo y MUST permanecer visible menos tiempo que
una subida.

#### Scenario: La subida se muestra con halo

- **GIVEN** un acierto que certifica una frase
- **WHEN** la pantalla muestra el indicador de cambio de insignia
- **THEN** el indicador muestra un halo alrededor de la insignia "Oro"

#### Scenario: La bajada se muestra sin halo

- **GIVEN** un fallo que baja el nivel de una frase
- **WHEN** la pantalla muestra el indicador de cambio de insignia
- **THEN** el indicador no muestra ningún halo alrededor de la insignia resultante

#### Scenario: La bajada permanece visible menos tiempo que la subida

- **GIVEN** una subida de nivel y, por separado, una bajada de nivel
- **WHEN** se muestra el indicador de cambio de insignia en cada caso
- **THEN** la duración del indicador de la bajada es menor que la duración del indicador de la subida

### Requirement: Botón flotante para revertir la última autoevaluación

Tras autoevaluar una frase y avanzar a la frase siguiente, la pantalla MUST mostrar un botón
flotante en la esquina inferior izquierda que permite revertir esa autoevaluación. El botón MUST
NOT mostrarse antes de la primera autoevaluación de la tanda activa, mientras una autoevaluación
esté en curso (`busy`), ni durante la transición del indicador de cambio de insignia de la frase
que se acaba de autoevaluar.

#### Scenario: El botón aparece tras autoevaluar

- **GIVEN** una tanda con las frases A y B en ese orden, con A revelada
- **WHEN** el usuario se autoevalúa A con acierto y la pantalla pasa a mostrar B
- **THEN** la pantalla muestra el botón flotante de revertir

#### Scenario: El botón no aparece antes de la primera autoevaluación

- **GIVEN** el usuario abre la pantalla de práctica y todavía no ha autoevaluado ninguna frase de
  la tanda activa
- **WHEN** la pantalla muestra la primera frase
- **THEN** el botón flotante de revertir no está visible

#### Scenario: El botón no aparece durante la transición de cambio de insignia

- **GIVEN** un acierto que certifica una frase y dispara la transición del indicador de cambio de
  insignia
- **WHEN** la pantalla muestra esa transición, antes de avanzar a la frase siguiente
- **THEN** el botón flotante de revertir todavía no está visible

#### Scenario: El botón no aparece mientras se registra una nueva autoevaluación

- **GIVEN** una tanda con las frases A, B y C en ese orden; A ya autoevaluada, con la pantalla
  mostrando B y el botón flotante de revertir visible (apuntando a la autoevaluación de A)
- **WHEN** el usuario se autoevalúa B, mientras esa autoevaluación está en curso, antes de que la
  pantalla pase a mostrar C
- **THEN** el botón flotante de revertir no está visible

### Requirement: Revertir vuelve a la frase anterior y oculta el botón

Al pulsar el botón flotante de revertir, la pantalla MUST volver a mostrar la frase afectada como
la frase activa, revelada, en la posición que ocupaba antes de esa autoevaluación, con su racha y
su insignia de nivel de consolidación anteriores, y MUST dejar de mostrar el botón flotante. No
MUST ser posible registrar más de una reversión por autoevaluación.

#### Scenario: Revertir un acierto vuelve a mostrar la frase anterior

- **GIVEN** una tanda con las frases A, B y C en ese orden, con A revelada, que el usuario
  autoevalúa con acierto y la pantalla pasa a mostrar B sin revelar
- **WHEN** el usuario pulsa el botón flotante de revertir
- **THEN** la pantalla vuelve a mostrar A revelada, con su racha e insignia anteriores, y el botón
  flotante deja de mostrarse

#### Scenario: Doble toque en revertir solo revierte una vez

- **GIVEN** el botón flotante de revertir visible tras autoevaluar una frase
- **WHEN** el usuario lo pulsa dos veces seguidas
- **THEN** solo se revierte esa autoevaluación una vez y la segunda pulsación se ignora

#### Scenario: Autoevaluar de nuevo tras revertir vuelve a ofrecer el botón

- **GIVEN** la frase A vuelta a mostrar, revelada, tras revertir su autoevaluación anterior
- **WHEN** el usuario se autoevalúa A de nuevo y la pantalla avanza a la frase siguiente
- **THEN** la pantalla muestra el botón flotante de revertir, disponible para esta nueva
  autoevaluación

#### Scenario: Generar sesión hace desaparecer el botón

- **GIVEN** el botón flotante de revertir visible tras la última autoevaluación de la tanda activa,
  con la pantalla mostrando el mensaje de tanda completada
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra la tanda siguiente sin el botón flotante de revertir visible
