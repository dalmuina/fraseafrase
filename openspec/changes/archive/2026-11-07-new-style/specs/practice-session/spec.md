# Spec Delta

## MODIFIED Requirements

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

#### Scenario: Sin insignia en nivel 0

_Título heredado del requisito que este delta reemplaza (`openspec validate` exige conservarlo); el
comportamiento cambia: ahora sí se muestra una insignia, "sin rango"._

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
