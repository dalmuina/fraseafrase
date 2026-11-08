# Spec Delta

## MODIFIED Requirements

### Requirement: Total de frases por nivel

La pantalla de progreso MUST mostrar, para cada nivel de 0 a 5, el número de frases del catálogo
**del nivel CEFR vigente en ajustes** en ese nivel de consolidación junto a la insignia de ese nivel
(`level-badges`), incluida la insignia "sin rango" del nivel 0; la insignia MUST ser la única
identificación visible del nivel de esa fila - el número de nivel en sí MUST NOT aparecer como
texto, para no duplicar la insignia con un número. Una frase del catálogo del nivel CEFR vigente sin
progreso registrado MUST contarse en el nivel 0. Una frase del catálogo del nivel CEFR vigente con
progreso registrado MUST contarse en su nivel actual. Una frase del catálogo de un nivel CEFR
distinto del vigente MUST NOT contarse en ninguna fila. La suma de los seis niveles MUST ser igual
al total de frases del catálogo del nivel CEFR vigente.

#### Scenario: Catálogo con frases en varios niveles

- **GIVEN** el nivel CEFR B1 vigente y un catálogo de diez frases de nivel CEFR B1 donde tres tienen
  progreso de nivel de consolidación 2, una de nivel 5 y las seis restantes no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra una fila con la insignia "sin rango" y 6 frases, una fila con la
  insignia "Plata" y 3 frases, una fila con la insignia "Diamante" y 1 frase, y las filas de las
  insignias "Bronce", "Oro" y "Platino" con 0 frases cada una

#### Scenario: Ninguna frase con progreso

- **GIVEN** el nivel CEFR vigente y un catálogo de ocho frases de ese nivel CEFR sin ningún progreso
  registrado
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra una fila con la insignia "sin rango" con 8 frases y el resto de filas
  con 0

#### Scenario: Frase de la tanda activa cuenta en nivel 0

- **GIVEN** el nivel CEFR vigente y un catálogo de cinco frases de ese nivel CEFR donde una está en
  la tanda activa con nivel de consolidación 0 y racha 2, y las otras cuatro no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra la fila con la insignia "sin rango" con 5 frases

#### Scenario: La fila de nivel 0 muestra la insignia sin rango y su número de frases

- **GIVEN** el nivel CEFR vigente y un catálogo con frases de ese nivel CEFR en nivel de
  consolidación 0
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la fila de nivel 0 muestra su número de frases junto a la insignia "sin rango" y sin el
  número "0" como etiqueta de nivel

#### Scenario: Las frases de otros niveles CEFR no se cuentan

- **GIVEN** el nivel CEFR B1 vigente y un catálogo de seis frases de nivel CEFR B1 sin progreso y
  cuatro frases de nivel CEFR C1 sin progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra la fila con la insignia "sin rango" con 6 frases, no con 10

### Requirement: Total de frases disponibles y nivel CEFR vigente

La pantalla de progreso MUST mostrar, en una tarjeta destacada, el número total de frases **del
nivel CEFR vigente en ajustes** con progreso registrado en cualquier nivel de consolidación (la
suma de las seis filas por nivel) junto con el nivel CEFR guardado en los ajustes vigentes.

#### Scenario: El total coincide con la suma de los niveles

- **GIVEN** el nivel CEFR B1 guardado y un catálogo de diez frases de nivel CEFR B1 donde tres
  tienen progreso de nivel de consolidación 2, una de nivel 5 y las seis restantes no tienen
  progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la tarjeta destacada muestra 10 frases disponibles y el nivel CEFR B1

#### Scenario: El total se actualiza si cambia el catálogo disponible

- **GIVEN** el nivel CEFR vigente y un catálogo de seis frases de ese nivel CEFR sin progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la tarjeta destacada muestra 6 frases disponibles

#### Scenario: El total no incluye frases de otros niveles CEFR

- **GIVEN** el nivel CEFR B1 guardado y un catálogo de seis frases de nivel CEFR B1 sin progreso y
  cuatro frases de nivel CEFR C1 sin progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la tarjeta destacada muestra 6 frases disponibles, no 10
