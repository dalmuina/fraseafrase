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

### Requirement: Confirmación visual al guardar

Al guardar los ajustes con éxito, la pantalla MUST mostrar una confirmación visible de que se
guardaron.

#### Scenario: Confirmación tras guardar

- **GIVEN** la pantalla de ajustes abierta con un nivel CEFR y una N válidos
- **WHEN** el usuario confirma el guardado
- **THEN** la pantalla muestra una confirmación visible de que los ajustes se guardaron

### Requirement: Regeneración sin tocar el progreso al cambiar los ajustes

Si el nivel CEFR o el tamaño de tanda N guardados difieren de los que estaban persistidos antes de guardar, el
sistema MUST recomponer la tanda activa de inmediato, sin mostrar ningún aviso previo, sin incrementar el
contador de sesiones generadas y sin tocar la racha, el nivel de consolidación ni el `dueAtSession` de ninguna
frase: es una recomposición de qué frases están activas, no una resolución de cierre.

#### Scenario: Cambiar el nivel CEFR no toca el progreso

- **GIVEN** una tanda activa guardada, con una frase de racha 2, y el nivel CEFR A1 persistido
- **WHEN** el usuario elige el nivel B1 y confirma el guardado
- **THEN** la tanda activa se recompone con frases nuevas de nivel B1 y la frase de racha 2 conserva su racha,
  su nivel de consolidación y su `dueAtSession` sin cambios

#### Scenario: Cambiar N no toca el progreso

- **GIVEN** una tanda activa guardada y N igual a 20 persistido, con una frase de racha 2
- **WHEN** el usuario escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa se recompone con tamaño 15 y la frase de racha 2 conserva su racha, su nivel de
  consolidación y su `dueAtSession` sin cambios

#### Scenario: Cambiar N y el nivel CEFR a la vez tampoco toca el progreso

- **GIVEN** una tanda activa guardada, el nivel CEFR A1 y N igual a 20, ambos persistidos
- **WHEN** el usuario elige el nivel B1, escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa se recompone con nivel B1 y N igual a 15, sin certificar ni tocar el progreso de
  ninguna frase

#### Scenario: Ningún cambio de ajustes incrementa el contador de sesiones

- **GIVEN** el contador de sesiones generadas en 9
- **WHEN** el usuario cambia el nivel CEFR, N, o ambos, y confirma el guardado
- **THEN** el contador de sesiones generadas sigue en 9

#### Scenario: Sin tanda activa no hay nada que recomponer

- **GIVEN** no hay tanda activa guardada
- **WHEN** el usuario cambia el nivel CEFR o N y confirma el guardado
- **THEN** el guardado se completa sin error y sin mostrar ningún aviso

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

### Requirement: Guardar sin cambios no recompone ni navega

Si el nivel CEFR y la N guardados son iguales a los que estaban persistidos antes de guardar, el
sistema MUST NOT recomponer la tanda activa ni navegar a la pantalla de práctica.

#### Scenario: Guardar sin cambiar ningún valor se queda en ajustes

- **GIVEN** la pantalla de ajustes abierta con el nivel CEFR B1 y N igual a 20, ambos persistidos
- **WHEN** el usuario confirma el guardado sin cambiar ningún valor
- **THEN** la pantalla sigue mostrando los ajustes y la tanda activa no se recompone
