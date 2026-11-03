# settings-screen Specification

## Purpose

Define la pantalla de ajustes: cómo el usuario ve y cambia el nivel CEFR y el tamaño de tanda N
vigentes, los guarda, y cómo llega a esta pantalla desde la práctica y vuelve a ella.

## Requirements

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

### Requirement: Edición descartada al salir sin guardar

Si el usuario sale de la pantalla de ajustes sin confirmar el guardado, la edición en curso MUST
descartarse: una lectura posterior de los ajustes MUST NOT reflejarla.

#### Scenario: Salir sin guardar no persiste la edición

- **GIVEN** la pantalla de ajustes abierta con nivel CEFR A1 y N igual a 20 guardados, y el usuario
  edita el tamaño de tanda a 15 sin confirmar el guardado
- **WHEN** el usuario sale de la pantalla de ajustes
- **THEN** una lectura posterior de los ajustes devuelve N igual a 20
