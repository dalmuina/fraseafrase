# Spec Delta

## MODIFIED Requirements

### Requirement: Total de frases por nivel

La pantalla de progreso MUST mostrar, para cada nivel de 0 a 5, el número de frases del catálogo en
ese nivel junto a la insignia de ese nivel (`level-badges`); la insignia (o su ausencia, en nivel 0)
MUST ser la única identificación visible del nivel de esa fila - el número de nivel en sí MUST NOT
aparecer como texto, para no duplicar la insignia con un número. Una frase del catálogo sin progreso
registrado MUST contarse en el nivel 0. Una frase con progreso registrado MUST contarse en su nivel
actual. La suma de los seis niveles MUST ser igual al total de frases del catálogo.

#### Scenario: Catálogo con frases en varios niveles

- **GIVEN** un catálogo de diez frases donde tres tienen progreso de nivel 2, una de nivel 5 y las
  seis restantes no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra una fila sin insignia con 6 frases, una fila con la insignia "Plata"
  y 3 frases, una fila con la insignia "Diamante" y 1 frase, y las filas de las insignias "Bronce",
  "Oro" y "Platino" con 0 frases cada una

#### Scenario: Ninguna frase con progreso

- **GIVEN** un catálogo de ocho frases sin ningún progreso registrado
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra una fila sin insignia con 8 frases y el resto de filas con 0

#### Scenario: Frase de la tanda activa cuenta en nivel 0

- **GIVEN** un catálogo de cinco frases donde una está en la tanda activa con nivel 0 y racha 2, y las
  otras cuatro no tienen progreso
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la pantalla muestra la fila sin insignia con 5 frases

#### Scenario: La fila de nivel 0 no muestra insignia ni número de nivel

- **GIVEN** un catálogo con frases en nivel 0
- **WHEN** el usuario abre la pantalla de progreso
- **THEN** la fila de nivel 0 muestra su número de frases sin ninguna insignia y sin el número "0"
  como etiqueta de nivel
