## MODIFIED Requirements

### Requirement: Idioma de origen según el dispositivo

El idioma de origen MUST ser único para toda la app y MUST NOT elegirse ni guardarse. MUST resolverse con la lista
ordenada de idiomas del dispositivo: para cada idioma, en orden, el disponible idéntico y, si no, uno disponible con
el mismo idioma base (el primero por orden alfabético de etiqueta). Gana el primero que case; si ninguno, el idioma
de origen por defecto de la app, hoy `es-ES`, que MUST NOT fijarse en el dominio. Resolverlo MUST NOT lanzar error.

#### Scenario: El idioma del dispositivo está disponible

- **GIVEN** `pt-PT`, `fr-FR` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[fr-FR]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `fr-FR`

#### Scenario: El idioma del dispositivo no está disponible

- **GIVEN** `pt-PT` y `es-ES` disponibles, `es-ES` como idioma de origen por defecto y un dispositivo con la lista
  de idiomas `[ja-JP]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `es-ES`

#### Scenario: Otra región del mismo idioma usa el idioma disponible

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles, `es-ES` como idioma de origen por defecto y un dispositivo con
  la lista de idiomas `[en-US]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `en-GB`

#### Scenario: Español de otra región usa es-ES

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[es-MX]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `es-ES`

#### Scenario: Portugués de Brasil usa pt-PT

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles, sin `pt-BR`, y un dispositivo con la lista de idiomas `[pt-BR]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `pt-PT`

#### Scenario: Un idioma sin región usa la variante disponible

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[en]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `en-GB`

#### Scenario: La coincidencia exacta gana a la del mismo idioma

- **GIVEN** `pt-PT`, `pt-BR` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[pt-PT]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `pt-PT`

#### Scenario: Varias variantes del mismo idioma se desempatan por orden alfabético

- **GIVEN** `pt-PT`, `pt-BR` y `es-ES` disponibles, en ese orden, y un dispositivo con la lista de idiomas `[pt-AO]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `pt-BR`

#### Scenario: Se usa el siguiente idioma de la lista si el primero no casa

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles, `es-ES` como idioma de origen por defecto y un dispositivo con
  la lista de idiomas `[eu-ES, en-US]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `en-GB`

#### Scenario: El primer idioma de la lista gana aunque solo case por idioma base

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[en-US, es-ES]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `en-GB`

#### Scenario: Ningún idioma de la lista casa

- **GIVEN** `pt-PT` y `es-ES` disponibles, `es-ES` como idioma de origen por defecto y un dispositivo con la lista
  de idiomas `[ja-JP, eu-ES]`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `es-ES`

#### Scenario: Una lista de idiomas vacía usa el idioma por defecto

- **GIVEN** `pt-PT` y `es-ES` disponibles, `es-ES` como idioma de origen por defecto y un dispositivo con una lista
  de idiomas vacía
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `es-ES`, sin error

#### Scenario: El idioma de origen no depende del idioma destino activo

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[es-ES]`
- **WHEN** se resuelve el idioma de origen con `pt-PT` activo y, después, con `en-GB` activo
- **THEN** el idioma de origen es `es-ES` en ambos casos

#### Scenario: Un cambio del idioma del dispositivo cambia el origen

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con la lista de idiomas `[es-ES]`
- **WHEN** la lista de idiomas del dispositivo pasa a `[en-GB]` y se vuelve a resolver el idioma de origen
- **THEN** el idioma de origen es `en-GB`
