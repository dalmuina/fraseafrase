## MODIFIED Requirements

### Requirement: Estado vacío sin frases disponibles

Si el nivel CEFR elegido no tiene ninguna frase en el catálogo, la pantalla MUST indicar que no hay frases para
practicar y MUST NOT mostrar una tanda. En ese estado no MUST haber ninguna frase visible. Si el nivel CEFR
elegido tiene frases, aunque ninguna esté pendiente ni sea nueva, la pantalla MUST mostrar una tanda (ADR-0026).

#### Scenario: Catálogo agotado tras cerrar

- **GIVEN** una tanda generada y un catálogo sin ninguna frase del nivel CEFR elegido
- **WHEN** el usuario genera sesión
- **THEN** la pantalla indica que no hay frases para practicar y no muestra ninguna frase

#### Scenario: Sin frases disponibles al pedir el cierre

- **GIVEN** la pantalla muestra una tanda, esa tanda ya no está activa y el catálogo no tiene ninguna frase del
  nivel CEFR elegido
- **WHEN** el usuario pide generar sesión
- **THEN** la pantalla indica que no hay frases para practicar, no muestra el mensaje de tanda completada y no
  muestra ninguna frase

#### Scenario: Frases con repaso futuro sin tanda

- **GIVEN** un catálogo cuya única frase del nivel elegido, P, tiene nivel 1 y `dueAtSession` posterior a la
  sesión actual, sin tanda activa
- **WHEN** el usuario abre la pantalla de práctica
- **THEN** la pantalla muestra P sin revelar y no indica que no haya frases para practicar

### Requirement: Mensaje de tanda completada y generación de sesión

Cuando la tanda activa está completa (`practice-batch`), la pantalla MUST mostrar un mensaje positivo con la
opción de generar la siguiente sesión, en vez de seguir mostrando frases para responder. Ese mensaje MUST
mostrar también, de la tanda que se acaba de completar, el número de frases autoevaluadas con acierto, el
número autoevaluadas con fallo y el número de frases cuyo nivel de consolidación subió en algún momento
durante esa tanda, y MUST ofrecer un enlace para ir a la pantalla de Progreso sin generar sesión. Al generar
sesión, la pantalla MUST mostrar la tanda siguiente, empezando por su primera frase, en el orden en que la
compone `practice-batch`: pendientes por prioridad, después el repaso adelantado, y nuevas repartidas entre
ambos (ADR-0025, ADR-0026). Mientras haya una respuesta en curso, pedir generar sesión MUST NOT tener efecto.

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

> El título es el heredado del requisito que este delta modifica (OpenSpec no permite renombrar un escenario
> dentro de MODIFIED). El resultado real está en el THEN: con ADR-0025, las pendientes van por prioridad y las
> nuevas se reparten entre ellas.

- **GIVEN** el mensaje de tanda completada, con dos frases que serán pendientes en la sesión siguiente, A y
  C, ambas de nivel 0 y con el mismo retraso, A antes que C en el orden de catálogo, un catálogo con 4 frases
  nuevas del nivel elegido, sin otra frase con progreso del nivel elegido, y N igual a 10
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra la tanda siguiente con 6 frases: A en la primera posición, las 4 primeras
  frases nuevas del catálogo en las posiciones 2 a 5 y C en la sexta

#### Scenario: Generar sesión con todas certificadas deja la tanda siguiente sin arrastradas

> El título es el heredado del requisito que este delta modifica: "arrastradas" ya no es un estado posible.
> Desde ADR-0026 las frases certificadas sin vencer entran como repaso adelantado si falta sitio.

- **GIVEN** el mensaje de tanda completada de una tanda de dos frases, A y B, ambas certificadas con un
  enfriamiento que no ha vencido, un catálogo A, B, N1, N2 del nivel elegido, en ese orden, y N igual a 10
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra la tanda siguiente A, N1, N2, B, y A y B conservan su nivel, su racha y su
  `dueAtSession`

#### Scenario: Una respuesta en curso ignora la petición de generar sesión

- **GIVEN** una frase revelada cuya autoevaluación se está registrando, todavía sin confirmar en la tanda
- **WHEN** el usuario pide generar sesión
- **THEN** la petición se ignora y la tanda no cambia

#### Scenario: Generar sesión reinicia el desglose para la tanda siguiente

- **GIVEN** la tanda completada con 2 aciertos y 1 fallo, con la pantalla mostrando ese desglose
- **WHEN** el usuario genera sesión y completa la tanda siguiente con 1 acierto y 0 fallos
- **THEN** el nuevo mensaje de tanda completada muestra 1 acierto y 0 fallos, no el desglose de la tanda
  anterior
