# progress-overview Specification

## Purpose

Define el progreso básico (ADR-0018): el total de frases disponibles del catálogo para el nivel
CEFR vigente, ese mismo nivel CEFR, y cuántas de esas frases hay en cada nivel de consolidación, más
la pantalla que los muestra.

## Requirements

### Requirement: Total de frases por nivel

La pantalla de progreso MUST mostrar, para cada nivel de 0 a 5, el número de frases del catálogo
del nivel CEFR vigente en ese nivel de consolidación junto a la insignia de ese nivel
(`level-badges`), incluida la insignia "sin rango" del nivel 0; la insignia MUST ser la única
identificación visible del nivel de esa fila - el número de nivel en sí MUST NOT aparecer como
texto, para no duplicar la insignia con un número.

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

#### Scenario: La fila de nivel 0 muestra la insignia sin rango y su número de frases

- **GIVEN** el nivel CEFR vigente y un catálogo con frases de ese nivel CEFR en nivel de
  consolidación 0
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la fila de nivel 0 muestra su número de frases junto a la insignia "sin rango" y sin el
  número "0" como etiqueta de nivel

### Requirement: Recuento por nivel acotado al nivel CEFR vigente

Solo las frases del catálogo del nivel CEFR vigente en ajustes MUST contarse en las filas por nivel
de consolidación. Una de esas frases sin progreso registrado MUST contarse en el nivel 0, y con
progreso registrado MUST contarse en su nivel actual. Una frase de un nivel CEFR distinto del
vigente MUST NOT contarse en ninguna fila. La suma de los seis niveles MUST ser igual al total de
frases del catálogo del nivel CEFR vigente.

#### Scenario: Frase de la tanda activa cuenta en nivel 0 de su propio nivel CEFR

- **GIVEN** el nivel CEFR vigente y un catálogo de cinco frases de ese nivel CEFR donde una está en
  la tanda activa con nivel de consolidación 0 y racha 2, y las otras cuatro no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra la fila con la insignia "sin rango" con 5 frases

#### Scenario: Una frase de otro nivel CEFR no se cuenta en ninguna fila

- **GIVEN** el nivel CEFR B1 vigente y un catálogo de seis frases de nivel CEFR B1 sin progreso y
  cuatro frases de nivel CEFR C1 sin progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la suma de las seis filas por nivel es 6, no 10

### Requirement: Progreso sin tanda activa

La pantalla de progreso MUST mostrar el total de frases por nivel, el total de frases disponibles
del catálogo y el nivel CEFR vigente, haya o no tanda activa: ninguno de estos tres datos depende de
si hay una tanda activa.

#### Scenario: Sin tanda activa muestra el total disponible, el nivel CEFR y el total por nivel

- **GIVEN** que no hay tanda activa, un catálogo de seis frases sin progreso y el nivel CEFR B1
  guardado
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra 6 frases disponibles, el nivel CEFR B1 y el nivel 0 con 6 frases

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

### Requirement: Error de catálogo en el progreso

Si el catálogo no se puede cargar por cualquiera de sus motivos de error, la pantalla de progreso
MUST mostrar un estado de error que indique que el progreso no está disponible y MUST NOT mostrar
números de certificadas ni de niveles.

#### Scenario: Catálogo ilegible al abrir el progreso

- **GIVEN** un catálogo que no se puede leer
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra un error que indica que el progreso no está disponible y no muestra
  ningún número
