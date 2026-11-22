# Spec Delta: settings-screen

## MODIFIED Requirements

### Requirement: Selección del idioma destino

La pantalla de ajustes MUST mostrar un desplegable con los idiomas destino disponibles excepto el idioma de
origen, con el idioma destino activo preseleccionado. El usuario MUST poder elegir cualquiera de las opciones
mostradas; la pantalla MUST NOT permitir seleccionar un idioma destino que no esté disponible ni el idioma de
origen.

#### Scenario: Elegir un idioma destino de la lista

- **GIVEN** la pantalla de ajustes abierta con `pt-PT` y `pt-BR` disponibles y `pt-PT` como idioma
  destino activo
- **WHEN** el usuario elige `pt-BR` en el desplegable
- **THEN** la pantalla muestra `pt-BR` como idioma destino seleccionado

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
