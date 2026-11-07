# Spec Delta

## MODIFIED Requirements

### Requirement: Edición del tamaño de tanda N con validación

El usuario MUST poder editar el tamaño de tanda N como un valor de texto libre, o con los controles
de disminuir y aumentar. Mientras el valor editado no sea un entero entre 10 y 30 inclusive, la
pantalla MUST mostrar que no es válido y MUST NOT permitir guardar los ajustes. El control de
disminuir MUST estar deshabilitado cuando N ya es 10 y el control de aumentar MUST estar
deshabilitado cuando N ya es 30, de modo que ninguno de los dos pueda dejar N fuera de ese rango.

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

#### Scenario: El control de disminuir se deshabilita en el mínimo

- **GIVEN** la pantalla de ajustes abierta con el tamaño de tanda en 10
- **WHEN** la pantalla se muestra
- **THEN** el control de disminuir el tamaño de tanda está deshabilitado

#### Scenario: El control de aumentar se deshabilita en el máximo

- **GIVEN** la pantalla de ajustes abierta con el tamaño de tanda en 30
- **WHEN** la pantalla se muestra
- **THEN** el control de aumentar el tamaño de tanda está deshabilitado

#### Scenario: El control de disminuir se reactiva por encima del mínimo

- **GIVEN** la pantalla de ajustes abierta con el tamaño de tanda en 10
- **WHEN** el usuario escribe 15 como tamaño de tanda
- **THEN** el control de disminuir el tamaño de tanda deja de estar deshabilitado
