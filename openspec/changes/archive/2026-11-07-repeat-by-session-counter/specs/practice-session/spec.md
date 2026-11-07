# Spec Delta

## MODIFIED Requirements

### Requirement: Mensaje de tanda completada y generación de sesión

Cuando la tanda activa está completa (`practice-batch`), la pantalla MUST mostrar un mensaje positivo con la
opción de generar la siguiente sesión, en vez de seguir mostrando frases para responder. Al generar sesión, la
pantalla MUST mostrar la tanda siguiente, empezando por su primera frase; las frases a reforzar MUST aparecer
en orden de catálogo, delante de las nuevas (`practice-batch`, "Composición de la tanda nueva por cuotas
sobre el progreso": ya no hay una cola de arrastradas que fije el orden). Mientras haya una respuesta en
curso, pedir generar sesión MUST NOT tener efecto.

#### Scenario: Tanda completa muestra el mensaje positivo

- **GIVEN** una tanda de dos frases, ambas ya respondidas al menos una vez
- **WHEN** el usuario autoevalúa la última frase sin responder
- **THEN** la pantalla muestra un mensaje positivo con la opción de generar sesión, en vez de otra frase

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
