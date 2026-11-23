## MODIFIED Requirements

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
