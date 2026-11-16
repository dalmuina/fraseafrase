# Spec Delta

## MODIFIED Requirements

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
- **WHEN** el usuario elige `pt-BR` como idioma destino, elige el nivel B2 y escribe 15 como tamaño
  de tanda
- **THEN** una lectura posterior de los ajustes de `pt-BR` devuelve el nivel CEFR B2 y N igual a 15,
  y los ajustes de `pt-PT` no cambian

## ADDED Requirements

### Requirement: Elegir un idioma destino lo activa de inmediato

Al elegir un idioma destino en el desplegable, el sistema MUST activarlo de inmediato como idioma
destino activo de la aplicación, sin esperar ninguna confirmación del usuario ni que el usuario salga
de la pantalla de ajustes.

#### Scenario: Elegir un idioma destino lo activa sin salir de ajustes

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` como idioma destino activo
- **WHEN** el usuario elige `pt-BR` en el desplegable
- **THEN** una lectura posterior del idioma destino activo devuelve `pt-BR`, aunque el usuario siga
  en la pantalla de ajustes

### Requirement: Estilo de tarjeta del selector de idioma destino

El selector de idioma destino MUST mostrarse con el mismo estilo de tarjeta (fondo blanco, esquinas
redondeadas y relleno interior) que las tarjetas de nivel CEFR y de tamaño de tanda, en vez de quedar
directamente sobre el fondo de la pantalla.

#### Scenario: El selector de idioma destino usa el mismo estilo que las demás tarjetas

- **GIVEN** la pantalla de ajustes abierta
- **WHEN** la pantalla se muestra
- **THEN** el selector de idioma destino tiene el mismo fondo, forma y relleno que las tarjetas de
  nivel CEFR y de tamaño de tanda

## REMOVED Requirements

### Requirement: Navegación a práctica al cambiar ajustes

**Reason**: La navegación a práctica deja de ser una consecuencia de guardar: se elimina el botón
"Guardar" y es el propio usuario quien navega pulsando la pestaña de práctica. Si procede recomponer
la tanda activa, ocurre al obtenerla (capacidad `practice-persistence`), no como parte de un gesto de
guardado en Ajustes.

**Migration**: Ver el requisito añadido "Ajustes aplicados a la tanda activa" (`user-settings`) y el
requisito modificado de `practice-persistence` sobre `ObtainActiveBatch`.

### Requirement: Regeneración sin tocar el progreso al cambiar los ajustes

**Reason**: Esta regeneración deja de depender de un gesto de guardado en Ajustes: pasa a ser una
propiedad de obtener la tanda activa de un idioma destino (capacidad `practice-persistence`), que
compara los ajustes aplicados persistidos con los ajustes guardados vigentes cada vez que se entra en
práctica, sobreviva o no la app de por medio entre la edición y la entrada en práctica.

**Migration**: Ver el requisito modificado correspondiente en `practice-persistence`.

### Requirement: Guardar sin cambios no recompone ni navega

**Reason**: Ya no existe una acción de guardar separada que "no navegue". La parte de "no recompone
sin cambios" pasa a `practice-persistence`, como contrapartida del requisito anterior.

**Migration**: Ver el requisito modificado correspondiente en `practice-persistence`.

### Requirement: Confirmación visual al guardar

**Reason**: El guardado deja de ser una acción explícita confirmada por el usuario y pasa a ser
automático en cada edición válida; ya no hay un evento de guardado puntual que confirmar.

**Migration**: Ninguna. No se sustituye por otra confirmación visual.

### Requirement: Edición descartada al salir sin guardar

**Reason**: Toda edición válida del nivel CEFR o de N se autoguarda según se produce; ya no existe un
borrador en memoria que se pueda descartar al salir de la pantalla.

**Migration**: Ver el requisito modificado "Guardado de los ajustes editados".

### Requirement: Cambio de idioma destino descartado al salir sin guardar

**Reason**: El idioma destino elegido en el desplegable pasa a activarse de inmediato al elegirlo, no
solo al confirmar un guardado que ya no existe.

**Migration**: Ver el requisito añadido "Elegir un idioma destino lo activa de inmediato".
