# Spec Delta

## MODIFIED Requirements

### Requirement: Ajustes vigentes al abrir la pantalla

Al abrir la pantalla de ajustes, el sistema MUST mostrar el idioma destino activo, y el nivel CEFR y
el tamaño de tanda N guardados para ese idioma destino en ese momento.

#### Scenario: Se muestran los ajustes guardados

- **GIVEN** el idioma destino activo `pt-PT` con nivel CEFR B1 y N igual a 25 guardados para `pt-PT`
- **WHEN** el usuario abre la pantalla de ajustes
- **THEN** la pantalla muestra `pt-PT` como idioma destino, el nivel CEFR B1 y N igual a 25

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

## ADDED Requirements

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
