# Spec Delta

## Purpose

Define la pantalla de ajustes: cómo el usuario ve y cambia el nivel CEFR y el tamaño de tanda N
vigentes, los guarda, y cómo llega a esta pantalla desde la práctica y vuelve a ella.

## ADDED Requirements

### Requirement: Ajustes vigentes al abrir la pantalla

Al abrir la pantalla de ajustes, el sistema MUST mostrar el nivel CEFR y el tamaño de tanda N
guardados en ese momento.

#### Scenario: Se muestran los ajustes guardados

- **GIVEN** un nivel CEFR B1 y N igual a 25 guardados
- **WHEN** el usuario abre la pantalla de ajustes
- **THEN** la pantalla muestra el nivel CEFR B1 y N igual a 25

### Requirement: Selección del nivel CEFR

El usuario MUST poder elegir el nivel CEFR entre A1, A2, B1, B2 y C1. La pantalla MUST NOT permitir
seleccionar un valor fuera de esos cinco.

#### Scenario: Elegir un nivel CEFR de la lista

- **GIVEN** la pantalla de ajustes abierta con nivel CEFR A1
- **WHEN** el usuario elige el nivel B1
- **THEN** la pantalla muestra B1 como nivel CEFR seleccionado

### Requirement: Edición del tamaño de tanda N con validación

El usuario MUST poder editar el tamaño de tanda N como un valor de texto libre. Mientras el valor
editado no sea un entero entre 10 y 30 inclusive, la pantalla MUST mostrar que no es válido y MUST
NOT permitir guardar los ajustes.

#### Scenario: N fuera de rango impide guardar

- **GIVEN** la pantalla de ajustes abierta
- **WHEN** el usuario escribe 31 como tamaño de tanda
- **THEN** la pantalla indica que el valor no es válido y la acción de guardar no está disponible

#### Scenario: N no numérico impide guardar

- **GIVEN** la pantalla de ajustes abierta
- **WHEN** el usuario escribe `veinte` como tamaño de tanda
- **THEN** la pantalla indica que el valor no es válido y la acción de guardar no está disponible

#### Scenario: N dentro de rango permite guardar

- **GIVEN** la pantalla de ajustes abierta
- **WHEN** el usuario escribe 25 como tamaño de tanda
- **THEN** la acción de guardar está disponible

### Requirement: Guardado de los ajustes editados

Al confirmar el guardado con un nivel CEFR y una N válidos, el sistema MUST persistir ambos valores
de modo que una lectura posterior de los ajustes los devuelva.

#### Scenario: Guardar persiste el nuevo nivel CEFR y N

- **GIVEN** la pantalla de ajustes abierta con nivel CEFR A1 y N igual a 20 guardados
- **WHEN** el usuario elige el nivel B2, escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** una lectura posterior de los ajustes devuelve el nivel CEFR B2 y N igual a 15

### Requirement: Acceso a la pantalla de ajustes desde la práctica

La pantalla de práctica MUST ofrecer una acción visible para abrir la pantalla de ajustes.

#### Scenario: Abrir ajustes desde la práctica

- **GIVEN** el usuario está en la pantalla de práctica
- **WHEN** el usuario activa la acción de ajustes
- **THEN** se muestra la pantalla de ajustes

### Requirement: Vuelta a la práctica desde los ajustes

Desde la pantalla de ajustes, el usuario MUST poder volver a la pantalla de práctica, tanto si
guardó cambios como si no.

#### Scenario: Volver tras guardar

- **GIVEN** el usuario guardó un cambio de ajustes
- **WHEN** el usuario activa la acción de volver
- **THEN** se muestra la pantalla de práctica

#### Scenario: Volver sin guardar

- **GIVEN** el usuario editó el tamaño de tanda sin confirmar el guardado
- **WHEN** el usuario activa la acción de volver
- **THEN** se muestra la pantalla de práctica y una lectura posterior de los ajustes no refleja la edición sin guardar
