# Spec Delta

## ADDED Requirements

### Requirement: Confirmación visual al guardar

Al guardar los ajustes con éxito, la pantalla MUST mostrar una confirmación visible de que se
guardaron.

#### Scenario: Confirmación tras guardar

- **GIVEN** la pantalla de ajustes abierta con un nivel CEFR y una N válidos
- **WHEN** el usuario confirma el guardado
- **THEN** la pantalla muestra una confirmación visible de que los ajustes se guardaron

### Requirement: Cierre de la tanda activa al cambiar el nivel CEFR o N

Si el nivel CEFR o el tamaño de tanda N guardados difieren de los que estaban persistidos antes de
guardar, el sistema MUST cerrar la tanda activa, si hay alguna, sin mostrar el aviso previo de no
certificadas de `practice-batch`: es un cierre deliberado desde ajustes, no un cierre iniciado desde
la práctica.

#### Scenario: Cambiar el nivel CEFR cierra la tanda activa sin aviso

- **GIVEN** una tanda activa guardada y el nivel CEFR A1 persistido
- **WHEN** el usuario elige el nivel B1 y confirma el guardado
- **THEN** la tanda activa se cierra sin mostrar el aviso de frases no certificadas

#### Scenario: Cambiar N cierra la tanda activa sin aviso

- **GIVEN** una tanda activa guardada y N igual a 20 persistido
- **WHEN** el usuario escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa se cierra sin mostrar el aviso de frases no certificadas

#### Scenario: Sin tanda activa no hay nada que cerrar

- **GIVEN** no hay tanda activa guardada
- **WHEN** el usuario cambia el nivel CEFR y confirma el guardado
- **THEN** el guardado se completa sin error y sin mostrar ningún aviso de cierre

### Requirement: Navegación a práctica al cambiar ajustes

Si el nivel CEFR o N guardados difieren de los que estaban persistidos antes de guardar, el sistema
MUST navegar a la pantalla de práctica tras completar el guardado.

#### Scenario: Guardar con el nivel CEFR cambiado navega a práctica

- **GIVEN** la pantalla de ajustes abierta con el nivel CEFR A1 persistido
- **WHEN** el usuario elige el nivel B1 y confirma el guardado
- **THEN** la aplicación muestra la pantalla de práctica

#### Scenario: Guardar con N cambiado navega a práctica

- **GIVEN** la pantalla de ajustes abierta con N igual a 20 persistido
- **WHEN** el usuario escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la aplicación muestra la pantalla de práctica

### Requirement: Guardar sin cambios no cierra ni navega

Si el nivel CEFR y la N guardados son iguales a los que estaban persistidos antes de guardar, el
sistema MUST NOT cerrar la tanda activa ni navegar a la pantalla de práctica.

#### Scenario: Guardar sin cambiar ningún valor se queda en ajustes

- **GIVEN** la pantalla de ajustes abierta con el nivel CEFR B1 y N igual a 20, ambos persistidos
- **WHEN** el usuario confirma el guardado sin cambiar ningún valor
- **THEN** la pantalla sigue mostrando los ajustes y la tanda activa no se cierra
