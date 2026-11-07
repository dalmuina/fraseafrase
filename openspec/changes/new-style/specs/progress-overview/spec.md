# Spec Delta

## REMOVED Requirements

### Requirement: Certificadas en la tanda activa
**Reason**: El nuevo diseño de la pantalla de progreso sustituye este dato por el total de frases
disponibles del catálogo y el nivel CEFR vigente (ver "Total de frases disponibles y nivel CEFR
vigente" bajo ADDED). El dato de certificadas de la tanda activa deja de mostrarse en esta pantalla.
**Migration**: No aplica; no hay otra pantalla que dependa de este requisito.

Con tanda activa, la pantalla de progreso MUST mostrar el número de frases certificadas (racha ≥ 3)
sobre el total de frases de la tanda activa. El cálculo MUST NOT modificar la tanda, sus rachas ni
el progreso.

#### Scenario: Tanda con certificadas y no certificadas

- **GIVEN** una tanda activa de cinco frases, dos con racha 3 y tres sin certificar
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra 2 de 5 frases certificadas

#### Scenario: Tanda sin ninguna certificada

- **GIVEN** una tanda activa de tres frases, ninguna con racha 3
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra 0 de 3 frases certificadas

#### Scenario: Tanda totalmente certificada

- **GIVEN** una tanda activa de dos frases, ambas con racha 3
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra 2 de 2 frases certificadas

## MODIFIED Requirements

### Requirement: Total de frases por nivel

La pantalla de progreso MUST mostrar, para cada nivel de 0 a 5, el número de frases del catálogo en
ese nivel junto a la insignia de ese nivel (`level-badges`), incluida la insignia "sin rango" del
nivel 0; la insignia MUST ser la única identificación visible del nivel de esa fila - el número de
nivel en sí MUST NOT aparecer como texto, para no duplicar la insignia con un número. Una frase del
catálogo sin progreso registrado MUST contarse en el nivel 0. Una frase con progreso registrado MUST
contarse en su nivel actual. La suma de los seis niveles MUST ser igual al total de frases del
catálogo.

#### Scenario: Catálogo con frases en varios niveles

- **GIVEN** un catálogo de diez frases donde tres tienen progreso de nivel 2, una de nivel 5 y las
  seis restantes no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra una fila con la insignia "sin rango" y 6 frases, una fila con la
  insignia "Plata" y 3 frases, una fila con la insignia "Diamante" y 1 frase, y las filas de las
  insignias "Bronce", "Oro" y "Platino" con 0 frases cada una

#### Scenario: Ninguna frase con progreso

- **GIVEN** un catálogo de ocho frases sin ningún progreso registrado
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra una fila con la insignia "sin rango" con 8 frases y el resto de filas
  con 0

#### Scenario: Frase de la tanda activa cuenta en nivel 0

- **GIVEN** un catálogo de cinco frases donde una está en la tanda activa con nivel 0 y racha 2, y las
  otras cuatro no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra la fila con la insignia "sin rango" con 5 frases

#### Scenario: La fila de nivel 0 no muestra insignia ni número de nivel

_Título heredado del requisito que este delta reemplaza (`openspec validate` exige conservarlo); el
comportamiento cambia: la fila de nivel 0 ahora sí muestra la insignia "sin rango", solo sigue sin
mostrar el número "0" como etiqueta de nivel._

- **GIVEN** un catálogo con frases en nivel 0
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la fila de nivel 0 muestra su número de frases junto a la insignia "sin rango" y sin el
  número "0" como etiqueta de nivel

### Requirement: Progreso sin tanda activa

La pantalla de progreso MUST mostrar el total de frases por nivel, el total de frases disponibles
del catálogo y el nivel CEFR vigente, haya o no tanda activa: ninguno de estos tres datos depende de
si hay una tanda activa.

#### Scenario: Sin tanda activa muestra solo el total por nivel

_Título heredado del requisito que este delta reemplaza (`openspec validate` exige conservarlo); el
comportamiento cambia: sin tanda activa también se muestran el total disponible y el nivel CEFR._

- **GIVEN** que no hay tanda activa, un catálogo de seis frases sin progreso y el nivel CEFR B1
  guardado
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra 6 frases disponibles, el nivel CEFR B1 y el nivel 0 con 6 frases

## ADDED Requirements

### Requirement: Total de frases disponibles y nivel CEFR vigente

La pantalla de progreso MUST mostrar, en una tarjeta destacada, el número total de frases del
catálogo con progreso registrado en cualquier nivel (la suma de las seis filas por nivel) junto con
el nivel CEFR guardado en los ajustes vigentes.

#### Scenario: El total coincide con la suma de los niveles

- **GIVEN** un catálogo de diez frases donde tres tienen progreso de nivel 2, una de nivel 5 y las
  seis restantes no tienen progreso, y el nivel CEFR B1 guardado
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la tarjeta destacada muestra 10 frases disponibles y el nivel CEFR B1

#### Scenario: El total se actualiza si cambia el catálogo disponible

- **GIVEN** un catálogo de seis frases sin progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la tarjeta destacada muestra 6 frases disponibles
