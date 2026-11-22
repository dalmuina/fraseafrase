# Spec Delta: user-settings

## MODIFIED Requirements

### Requirement: Idioma destino activo

El sistema MUST persistir cuál es el idioma destino activo entre los idiomas destino disponibles. El idioma
destino activo MUST ser distinto del idioma de origen. Si no hay ninguno guardado, el guardado ya no está
disponible o coincide con el idioma de origen, el sistema MUST usar como activo el primer idioma disponible
distinto del idioma de origen y MUST NOT lanzar error.

#### Scenario: Primera ejecución usa el primer idioma destino disponible

- **GIVEN** que no hay ningún idioma destino activo guardado, con `pt-PT`, `pt-BR` y `es-ES` disponibles
  en ese orden y `es-ES` como idioma de origen
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: Primera ejecución salta el idioma de origen

- **GIVEN** que no hay ningún idioma destino activo guardado, con `es-ES` y `pt-PT` disponibles en ese orden y
  `es-ES` como idioma de origen
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: El idioma destino activo guardado se recupera

- **GIVEN** `pt-BR` guardado como idioma destino activo y `es-ES` como idioma de origen
- **WHEN** la app se reinicia y se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-BR`

#### Scenario: Un idioma destino activo guardado que ya no está disponible usa el primero disponible

- **GIVEN** `pt-BR` guardado como idioma destino activo, `es-ES` como idioma de origen, y un catálogo
  donde `pt-BR` ya no está disponible pero `pt-PT` y `es-ES` sí, en ese orden
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `pt-PT`

#### Scenario: Un idioma destino guardado igual al origen usa el primero distinto del origen

- **GIVEN** `es-ES` guardado como idioma destino activo y `es-ES` como idioma de origen, con `es-ES`, `de-DE` y
  `pt-PT` disponibles en ese orden
- **WHEN** se lee el idioma destino activo
- **THEN** el idioma destino activo es `de-DE`

### Requirement: Cambio del idioma destino activo

El sistema MUST ofrecer una forma de cambiar el idioma destino activo a cualquiera de los idiomas
destino disponibles distinto del idioma de origen. Cambiarlo MUST NOT modificar los ajustes, el progreso ni la
tanda activa de ningún idioma destino.

#### Scenario: Cambiar el idioma destino activo

- **GIVEN** `pt-PT` como idioma destino activo
- **WHEN** se cambia el idioma destino activo a `pt-BR`
- **THEN** una lectura posterior del idioma destino activo devuelve `pt-BR`

#### Scenario: Cambiar de idioma destino no toca el progreso de ninguno

- **GIVEN** `pt-PT` activo con progreso guardado, y `pt-BR` con su propio progreso guardado
- **WHEN** se cambia el idioma destino activo de `pt-PT` a `pt-BR`
- **THEN** el progreso guardado de `pt-PT` y de `pt-BR` quedan exactamente igual que antes del cambio

#### Scenario: No se puede elegir como destino el idioma de origen

- **GIVEN** `es-ES` como idioma de origen
- **WHEN** se intenta cambiar el idioma destino activo a `es-ES`
- **THEN** el cambio se rechaza sin error (resultado de rechazo, no una excepción) y el idioma destino activo
  guardado no cambia
