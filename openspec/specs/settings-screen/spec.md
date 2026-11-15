# settings-screen Specification

## Purpose

Define la pantalla de ajustes: cómo el usuario ve y cambia el nivel CEFR y el tamaño de tanda N
vigentes, los guarda, y cómo llega a esta pantalla desde la práctica y vuelve a ella.

## Requirements

### Requirement: Ajustes vigentes al abrir la pantalla

Al abrir la pantalla de ajustes, el sistema MUST mostrar el idioma destino activo, y el nivel CEFR y
el tamaño de tanda N guardados para ese idioma destino en ese momento.

#### Scenario: Se muestran los ajustes guardados

- **GIVEN** el idioma destino activo `pt-PT` con nivel CEFR B1 y N igual a 25 guardados para `pt-PT`
- **WHEN** el usuario abre la pantalla de ajustes
- **THEN** la pantalla muestra `pt-PT` como idioma destino, el nivel CEFR B1 y N igual a 25

### Requirement: Selección del nivel CEFR

El usuario MUST poder elegir el nivel CEFR entre A1, A2, B1, B2 y C1. La pantalla MUST NOT permitir
seleccionar un valor fuera de esos cinco.

#### Scenario: Elegir un nivel CEFR de la lista

- **GIVEN** la pantalla de ajustes abierta con nivel CEFR A1
- **WHEN** el usuario elige el nivel B1
- **THEN** la pantalla muestra B1 como nivel CEFR seleccionado

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

### Requirement: Guardado de los ajustes editados

Al confirmar el guardado con un idioma destino, un nivel CEFR y una N válidos, el sistema MUST
persistir los tres de modo que una lectura posterior de los ajustes del idioma destino guardado los
devuelva, sin modificar los ajustes de ningún otro idioma destino.

#### Scenario: Guardar persiste el nuevo nivel CEFR y N

- **GIVEN** la pantalla de ajustes abierta con el idioma destino `pt-PT`, nivel CEFR A1 y N igual a
  20 guardados para `pt-PT`
- **WHEN** el usuario elige el nivel B2, escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** una lectura posterior de los ajustes de `pt-PT` devuelve el nivel CEFR B2 y N igual a 15

#### Scenario: Guardar tras cambiar de idioma destino persiste en el idioma destino elegido

- **GIVEN** la pantalla de ajustes abierta con el idioma destino activo `pt-PT`
- **WHEN** el usuario elige `pt-BR` como idioma destino, elige el nivel B2, escribe 15 como tamaño
  de tanda y confirma el guardado
- **THEN** una lectura posterior de los ajustes de `pt-BR` devuelve el nivel CEFR B2 y N igual a 15,
  y los ajustes de `pt-PT` no cambian

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

Si el nivel CEFR o el tamaño de tanda N guardados del idioma destino activo difieren de los que
estaban persistidos antes de guardar, el sistema MUST recomponer de inmediato la tanda activa de ese
idioma destino, sin mostrar ningún aviso previo, sin incrementar su contador de sesiones generadas y
sin tocar la racha, el nivel de consolidación ni el `dueAtSession` de ninguna de sus frases: es una
recomposición de qué frases están activas, no una resolución de cierre. Esta recomposición MUST NOT
afectar a la tanda activa, al progreso ni al contador de sesiones de ningún otro idioma destino.

#### Scenario: Cambiar el nivel CEFR no toca el progreso

- **GIVEN** una tanda activa guardada para el idioma destino activo, con una frase de racha 2, y el
  nivel CEFR A1 persistido para ese idioma destino
- **WHEN** el usuario elige el nivel B1 y confirma el guardado
- **THEN** la tanda activa de ese idioma destino se recompone con frases nuevas de nivel B1 y la
  frase de racha 2 conserva su racha, su nivel de consolidación y su `dueAtSession` sin cambios

#### Scenario: Cambiar N no toca el progreso

- **GIVEN** una tanda activa guardada para el idioma destino activo y N igual a 20 persistido, con
  una frase de racha 2
- **WHEN** el usuario escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa de ese idioma destino se recompone con tamaño 15 y la frase de racha 2
  conserva su racha, su nivel de consolidación y su `dueAtSession` sin cambios

#### Scenario: Cambiar N y el nivel CEFR a la vez tampoco toca el progreso

- **GIVEN** una tanda activa guardada para el idioma destino activo, el nivel CEFR A1 y N igual a
  20, ambos persistidos para ese idioma destino
- **WHEN** el usuario elige el nivel B1, escribe 15 como tamaño de tanda y confirma el guardado
- **THEN** la tanda activa de ese idioma destino se recompone con nivel B1 y N igual a 15, sin
  certificar ni tocar el progreso de ninguna frase

#### Scenario: Ningún cambio de ajustes incrementa el contador de sesiones

- **GIVEN** el contador de sesiones generadas en 9 para el idioma destino activo
- **WHEN** el usuario cambia el nivel CEFR, N, o ambos, y confirma el guardado
- **THEN** el contador de sesiones generadas de ese idioma destino sigue en 9

#### Scenario: Sin tanda activa no hay nada que recomponer

- **GIVEN** no hay tanda activa guardada para el idioma destino activo
- **WHEN** el usuario cambia el nivel CEFR o N y confirma el guardado
- **THEN** el guardado se completa sin error y sin mostrar ningún aviso

#### Scenario: Cambiar solo de idioma destino, sin cambiar CEFR ni N, no recompone la tanda del idioma anterior

- **GIVEN** el idioma destino activo `pt-PT` con una tanda activa guardada
- **WHEN** el usuario cambia el idioma destino activo a `pt-BR` sin cambiar el nivel CEFR ni N de
  `pt-BR` respecto a lo ya guardado, y confirma el guardado
- **THEN** la tanda activa de `pt-PT` no se recompone

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

### Requirement: Alineación visual del stepper del tamaño de tanda

Los controles de disminuir y aumentar el tamaño de tanda N MUST tener su borde superior alineado con
el borde superior del campo de texto editable adyacente, de modo que ambos arranquen a la misma
altura visual. Esta alineación MUST mantenerse aunque el campo de texto reserve espacio adicional
por debajo de su línea de entrada, por ejemplo para mostrar el texto de error del tamaño de tanda
no válido.

#### Scenario: Los controles se alinean con el campo de texto sin error visible

- **GIVEN** la pantalla de ajustes abierta con un tamaño de tanda válido, sin texto de error visible
- **WHEN** la pantalla se muestra
- **THEN** el borde superior de los controles de disminuir y aumentar coincide con el borde superior
  del campo de texto del tamaño de tanda

#### Scenario: Los controles se mantienen alineados cuando el campo muestra el texto de error

- **GIVEN** la pantalla de ajustes abierta con un tamaño de tanda no válido, con su texto de error
  visible bajo el campo
- **WHEN** la pantalla se muestra
- **THEN** el borde superior de los controles de disminuir y aumentar sigue coincidiendo con el
  borde superior del campo de texto del tamaño de tanda

### Requirement: Selección del idioma destino

La pantalla de ajustes MUST mostrar un desplegable con los idiomas destino disponibles, con el
idioma destino activo preseleccionado. El usuario MUST poder elegir cualquiera de los idiomas
destino disponibles; la pantalla MUST NOT permitir seleccionar un idioma destino que no esté
disponible.

#### Scenario: Elegir un idioma destino de la lista

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` y `pt-BR` disponibles y `pt-PT` como idioma
  destino activo
- **WHEN** el usuario elige `pt-BR` en el desplegable
- **THEN** la pantalla muestra `pt-BR` como idioma destino seleccionado

### Requirement: Cambiar el idioma destino actualiza el CEFR y la N mostrados

Al elegir un idioma destino distinto en el desplegable, la pantalla MUST mostrar el nivel CEFR y la
N guardados para ese idioma destino, en vez de los del idioma destino mostrado anteriormente.

#### Scenario: Cambiar de idioma destino muestra sus propios ajustes

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` activo, nivel CEFR B1 y N igual a 25
  guardados para `pt-PT`, y nivel CEFR A2 y N igual a 10 guardados para `pt-BR`
- **WHEN** el usuario elige `pt-BR` en el desplegable
- **THEN** la pantalla muestra el nivel CEFR A2 y N igual a 10

### Requirement: Cambio de idioma destino descartado al salir sin guardar

Si el usuario elige un idioma destino distinto en el desplegable y sale de la pantalla de ajustes
sin confirmar el guardado, el idioma destino activo MUST NOT cambiar.

#### Scenario: Salir sin guardar no cambia el idioma destino activo

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` como idioma destino activo, y el usuario elige
  `pt-BR` en el desplegable sin confirmar el guardado
- **WHEN** el usuario sale de la pantalla de ajustes
- **THEN** una lectura posterior del idioma destino activo devuelve `pt-PT`
