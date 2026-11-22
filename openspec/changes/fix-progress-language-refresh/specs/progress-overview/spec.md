# Spec Delta

## ADDED Requirements

### Requirement: El progreso refleja los ajustes vigentes al volver a mostrarse

Cada vez que la pantalla de progreso se muestra, también al volver a ella desde otra pantalla, MUST
mostrar el recuento por nivel, el total disponible y el nivel CEFR vigentes del idioma destino
activo en ese momento, no los de una visita anterior.

#### Scenario: Volver a Progreso tras cambiar el idioma destino activo

- **GIVEN** el idioma destino activo `pt-PT` con un catálogo de seis frases sin progreso de su nivel
  CEFR vigente, y el idioma destino `pt-BR` con un catálogo de cuatro frases sin progreso de su nivel
  CEFR vigente
- **AND** el usuario ha abierto la pantalla de progreso, que muestra 6 frases disponibles
- **WHEN** el usuario cambia el idioma destino activo a `pt-BR` desde Ajustes y vuelve a la pantalla de
  progreso
- **THEN** la tarjeta destacada muestra 4 frases disponibles, la fila con la insignia "sin rango"
  muestra 4 frases y la cabecera muestra el icono de `pt-BR`

#### Scenario: Volver a Progreso tras aplicar otro nivel CEFR

- **GIVEN** el nivel CEFR B1 vigente y un catálogo de seis frases de nivel CEFR B1 y cuatro de nivel
  CEFR C1, todas sin progreso
- **AND** el usuario ha abierto la pantalla de progreso, que muestra 6 frases disponibles y el nivel CEFR B1
- **WHEN** el usuario aplica el nivel CEFR C1 desde Ajustes y vuelve a la pantalla de progreso
- **THEN** la tarjeta destacada muestra 4 frases disponibles y el nivel CEFR C1

### Requirement: La relectura del progreso no pasa por el estado de carga

Si la pantalla de progreso ya mostraba progreso, al volver a mostrarse MUST NOT mostrar el estado de
carga mientras relee: mantiene el progreso anterior hasta tener el nuevo.

#### Scenario: Volver a Progreso sin cambios no pasa por el estado de carga

- **GIVEN** que el usuario ha abierto la pantalla de progreso y muestra 6 frases disponibles
- **WHEN** el usuario vuelve a la pantalla de progreso sin haber cambiado ningún ajuste
- **THEN** la pantalla sigue mostrando 6 frases disponibles y en ningún momento muestra el estado de carga
