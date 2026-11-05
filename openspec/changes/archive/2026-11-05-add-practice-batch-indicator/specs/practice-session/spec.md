# Spec Delta

## ADDED Requirements

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
