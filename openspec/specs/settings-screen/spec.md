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
pantalla MUST mostrar que no es válido y el sistema MUST NOT autoguardar ese valor: los ajustes
persistidos del idioma destino mostrado conservan la última N válida. El control de disminuir MUST
estar deshabilitado cuando N ya es 10 y el control de aumentar MUST estar deshabilitado cuando N ya
es 30, de modo que ninguno de los dos pueda dejar N fuera de ese rango.

#### Scenario: N fuera de rango impide guardar

- **GIVEN** la pantalla de ajustes abierta con N igual a 20 guardado para el idioma destino mostrado
- **WHEN** el usuario escribe 31 como tamaño de tanda
- **THEN** la pantalla indica que el valor no es válido y una lectura posterior de los ajustes de ese
  idioma destino sigue devolviendo N igual a 20, porque no se autoguarda

#### Scenario: N no numérico impide guardar

- **GIVEN** la pantalla de ajustes abierta con N igual a 20 guardado para el idioma destino mostrado
- **WHEN** el usuario escribe `veinte` como tamaño de tanda
- **THEN** la pantalla indica que el valor no es válido y una lectura posterior de los ajustes de ese
  idioma destino sigue devolviendo N igual a 20, porque no se autoguarda

#### Scenario: N dentro de rango permite guardar

- **GIVEN** la pantalla de ajustes abierta
- **WHEN** el usuario escribe 25 como tamaño de tanda
- **THEN** una lectura posterior de los ajustes del idioma destino mostrado devuelve N igual a 25,
  porque se autoguarda

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

Al elegir un nivel CEFR, escribir una N válida o elegir un idioma destino en la pantalla de ajustes,
el sistema MUST persistir ese valor de inmediato para el idioma destino correspondiente, sin esperar
ninguna confirmación del usuario, de modo que una lectura posterior de los ajustes de ese idioma
destino lo refleje, sin modificar los ajustes de ningún otro idioma destino.

#### Scenario: Guardar persiste el nuevo nivel CEFR y N

- **GIVEN** la pantalla de ajustes abierta con el idioma destino `pt-PT`, nivel CEFR A1 y N igual a
  20 guardados para `pt-PT`
- **WHEN** el usuario elige el nivel B2 y escribe 15 como tamaño de tanda
- **THEN** una lectura posterior de los ajustes de `pt-PT` devuelve el nivel CEFR B2 y N igual a 15,
  sin que el usuario confirme ningún guardado

#### Scenario: Guardar tras cambiar de idioma destino persiste en el idioma destino elegido

- **GIVEN** la pantalla de ajustes abierta con el idioma destino activo `pt-PT`
- **WHEN** el usuario elige `en-GB` como idioma destino, elige el nivel B2 y escribe 15 como tamaño
  de tanda
- **THEN** una lectura posterior de los ajustes de `en-GB` devuelve el nivel CEFR B2 y N igual a 15,
  y los ajustes de `pt-PT` no cambian

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

La pantalla de ajustes MUST mostrar un desplegable con los idiomas destino disponibles excepto el idioma de
origen, con el idioma destino activo preseleccionado. El usuario MUST poder elegir cualquiera de las opciones
mostradas; la pantalla MUST NOT permitir seleccionar un idioma destino que no esté disponible ni el idioma de
origen.

#### Scenario: Elegir un idioma destino de la lista

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` y `en-GB` disponibles y `pt-PT` como idioma
  destino activo
- **WHEN** el usuario elige `en-GB` en el desplegable
- **THEN** la pantalla muestra `en-GB` como idioma destino seleccionado

#### Scenario: El desplegable de destino excluye el idioma de origen

- **GIVEN** la pantalla de ajustes abierta con `pt-PT`, `es-ES` y `en-GB` disponibles, `es-ES` como idioma de
  origen y `pt-PT` como idioma destino activo
- **WHEN** el usuario abre el desplegable de idioma destino
- **THEN** las opciones son `pt-PT` y `en-GB`, y `es-ES` no aparece

#### Scenario: Elegir un idioma que ha pasado a ser el origen recarga las opciones sin cerrar la app

- **GIVEN** la pantalla de ajustes cargada con `es-ES` como origen, `pt-PT` como destino activo y `en-GB` entre las
  opciones, y después el idioma del dispositivo pasa a `en-GB`
- **WHEN** el usuario elige `en-GB` en el desplegable
- **THEN** la app no se cierra, el idioma destino activo sigue siendo `pt-PT` y las opciones pasan a ser las
  disponibles menos `en-GB`

### Requirement: Estilo de tarjeta del selector de idioma destino

El selector de idioma destino MUST mostrarse con el mismo estilo de tarjeta (fondo blanco, esquinas
redondeadas y relleno interior) que las tarjetas de nivel CEFR y de tamaño de tanda, en vez de quedar
directamente sobre el fondo de la pantalla.

#### Scenario: El selector de idioma destino usa el mismo estilo que las demás tarjetas

- **GIVEN** la pantalla de ajustes abierta
- **WHEN** la pantalla se muestra
- **THEN** el selector de idioma destino tiene el mismo fondo, forma y relleno que las tarjetas de
  nivel CEFR y de tamaño de tanda

### Requirement: Cambiar el idioma destino actualiza el CEFR y la N mostrados

Al elegir un idioma destino distinto en el desplegable, la pantalla MUST mostrar el nivel CEFR y la
N guardados para ese idioma destino, en vez de los del idioma destino mostrado anteriormente.

#### Scenario: Cambiar de idioma destino muestra sus propios ajustes

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` activo, nivel CEFR B1 y N igual a 25
  guardados para `pt-PT`, y nivel CEFR A2 y N igual a 10 guardados para `en-GB`
- **WHEN** el usuario elige `en-GB` en el desplegable
- **THEN** la pantalla muestra el nivel CEFR A2 y N igual a 10

### Requirement: Elegir un idioma destino lo activa de inmediato

Al elegir un idioma destino en el desplegable, el sistema MUST activarlo de inmediato como idioma
destino activo de la aplicación, sin esperar ninguna confirmación del usuario ni que el usuario salga
de la pantalla de ajustes.

#### Scenario: Elegir un idioma destino lo activa sin salir de ajustes

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` como idioma destino activo
- **WHEN** el usuario elige `en-GB` en el desplegable
- **THEN** una lectura posterior del idioma destino activo devuelve `en-GB`, aunque el usuario siga
  en la pantalla de ajustes
