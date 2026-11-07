# level-badges Specification

## Purpose

Define la insignia visual (icono, color y etiqueta) de cada nivel de consolidación de una frase,
como concepto reutilizable que las pantallas de Practicar y Progreso muestran de la misma forma.

## Requirements

### Requirement: Insignia por nivel de consolidación

El sistema MUST asociar a cada nivel de consolidación de 0 a 5 (ADR-0012) una insignia: nivel 0
MUST mostrar la insignia "sin rango"; nivel 1 MUST mostrar la insignia "Bronce"; nivel 2 MUST
mostrar "Plata"; nivel 3 MUST mostrar "Oro"; nivel 4 MUST mostrar "Platino"; nivel 5 MUST mostrar
"Diamante". Cada insignia, incluida la de nivel 0, MUST mostrar un icono y una etiqueta de texto con
su nombre.

#### Scenario: Nivel 0 muestra la insignia sin rango

- **GIVEN** una frase con nivel de consolidación 0
- **WHEN** se pinta su insignia
- **THEN** se muestra un icono y la etiqueta "sin rango"

#### Scenario: Nivel 1 muestra Bronce

- **GIVEN** una frase con nivel de consolidación 1
- **WHEN** se pinta su insignia
- **THEN** se muestra un icono y la etiqueta "Bronce"

#### Scenario: Nivel 2 muestra Plata

- **GIVEN** una frase con nivel de consolidación 2
- **WHEN** se pinta su insignia
- **THEN** se muestra un icono y la etiqueta "Plata"

#### Scenario: Nivel 3 muestra Oro

- **GIVEN** una frase con nivel de consolidación 3
- **WHEN** se pinta su insignia
- **THEN** se muestra un icono y la etiqueta "Oro"

#### Scenario: Nivel 4 muestra Platino

- **GIVEN** una frase con nivel de consolidación 4
- **WHEN** se pinta su insignia
- **THEN** se muestra un icono y la etiqueta "Platino"

#### Scenario: Nivel 5 muestra Diamante

- **GIVEN** una frase con nivel de consolidación 5
- **WHEN** se pinta su insignia
- **THEN** se muestra un icono y la etiqueta "Diamante"

### Requirement: Insignias visualmente distintas

Las seis insignias de nivel 0 a 5 MUST ser distinguibles entre sí por icono o color, además de la
etiqueta de texto.

#### Scenario: Oro y Plata no comparten representación

- **GIVEN** las insignias de nivel 2 (Plata) y nivel 3 (Oro)
- **WHEN** se pintan ambas
- **THEN** usan un icono o un color distinto entre sí

#### Scenario: Sin rango y Bronce no comparten representación

- **GIVEN** las insignias de nivel 0 (sin rango) y nivel 1 (Bronce)
- **WHEN** se pintan ambas
- **THEN** usan un icono o un color distinto entre sí
