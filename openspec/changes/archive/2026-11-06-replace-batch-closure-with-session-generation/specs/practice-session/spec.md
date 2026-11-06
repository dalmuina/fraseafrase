# Spec Delta

## REMOVED Requirements

### Requirement: Aviso de cierre antes de confirmar

**Reason**: se sustituye por un mensaje positivo que aparece al completar una vuelta a la tanda, sin aviso
previo con un número de no certificadas ni confirmación aparte.
**Migration**: ver el requisito "Mensaje de tanda completada y generación de sesión" de este delta.

### Requirement: Tanda siguiente tras confirmar el cierre

**Reason**: confirmar el cierre deja de ser la acción del usuario; la acción ahora es "generar sesión".
**Migration**: ver el requisito "Mensaje de tanda completada y generación de sesión" de este delta, que
conserva la regla de que las arrastradas aparecen primero, en su orden de cola.

### Requirement: Racha y frase sabida visibles

**Reason**: mostrar la racha como número, además de la insignia de nivel, exponía dos parámetros al usuario
para la misma idea. Se sustituye por un único indicador de cuántos aciertos faltan para la siguiente insignia.
**Migration**: ver el requisito "Aciertos para el próximo nivel visibles" de este delta.

### Requirement: Cierre rechazado por cambio de tanda

**Reason**: ya no hay un aviso previo con una expectativa de no certificadas que pueda quedar desfasada;
generar sesión actúa siempre sobre la tanda activa guardada en el momento de la acción.
**Migration**: ninguna; no hay un caso equivalente que cubrir.

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Mensaje de tanda completada y generación de sesión

Cuando la tanda activa está completa (`practice-batch`), la pantalla MUST mostrar un mensaje positivo con la
opción de generar la siguiente sesión, en vez de seguir mostrando frases para responder. Al generar sesión, la
pantalla MUST mostrar la tanda siguiente, empezando por su primera frase; las arrastradas MUST aparecer
primero, en su orden de cola. Mientras haya una respuesta en curso, pedir generar sesión MUST NOT tener efecto.

#### Scenario: Tanda completa muestra el mensaje positivo

- **GIVEN** una tanda de dos frases, ambas ya respondidas al menos una vez
- **WHEN** el usuario autoevalúa la última frase sin responder
- **THEN** la pantalla muestra un mensaje positivo con la opción de generar sesión, en vez de otra frase

#### Scenario: Generar sesión muestra la tanda siguiente con sus arrastradas primero

- **GIVEN** el mensaje de tanda completada, con dos frases que quedarían arrastradas, C y A en ese orden, sin
  pendientes, un catálogo con 8 frases nuevas del nivel elegido y N igual a 10
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra la tanda siguiente con C en la primera posición, A en la segunda y las 8 frases
  nuevas a continuación

#### Scenario: Generar sesión con todas certificadas deja la tanda siguiente sin arrastradas

- **GIVEN** el mensaje de tanda completada de una tanda de dos frases, ambas certificadas, y un catálogo con
  frases nuevas del nivel elegido
- **WHEN** el usuario genera sesión
- **THEN** la pantalla muestra una tanda nueva sin arrastradas, compuesta con frases nuevas del nivel elegido

#### Scenario: Una respuesta en curso ignora la petición de generar sesión

- **GIVEN** una frase revelada cuya autoevaluación se está registrando, todavía sin confirmar en la tanda
- **WHEN** el usuario pide generar sesión
- **THEN** la petición se ignora y la tanda no cambia

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
