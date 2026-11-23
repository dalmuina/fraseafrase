## MODIFIED Requirements

### Requirement: Idioma de origen según el dispositivo

El idioma de origen MUST ser único para toda la app y MUST NOT elegirse ni guardarse. MUST ser el locale del
dispositivo si es uno de los idiomas disponibles; si no, el idioma de origen por defecto de la app, que hoy es
`es-ES`. El valor por defecto MUST NOT fijarse en la lógica de dominio. Resolverlo MUST NOT lanzar error.

#### Scenario: El idioma del dispositivo está disponible

- **GIVEN** `pt-PT`, `fr-FR` y `es-ES` disponibles y un dispositivo con locale `fr-FR`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `fr-FR`

#### Scenario: El idioma del dispositivo no está disponible

- **GIVEN** `pt-PT` y `es-ES` disponibles, `es-ES` como idioma de origen por defecto y un dispositivo con locale
  `ja-JP`
- **WHEN** se resuelve el idioma de origen
- **THEN** el idioma de origen es `es-ES`

#### Scenario: El idioma de origen no depende del idioma destino activo

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con locale `es-ES`
- **WHEN** se resuelve el idioma de origen con `pt-PT` activo y, después, con `en-GB` activo
- **THEN** el idioma de origen es `es-ES` en ambos casos

#### Scenario: Un cambio del idioma del dispositivo cambia el origen

- **GIVEN** `pt-PT`, `en-GB` y `es-ES` disponibles y un dispositivo con locale `es-ES`
- **WHEN** el locale del dispositivo pasa a `en-GB` y se vuelve a resolver el idioma de origen
- **THEN** el idioma de origen es `en-GB`

### Requirement: Formato del catálogo de frases

El catálogo MUST almacenarse como documentos JSON de frases empaquetados con la app, uno por idioma. La raíz
de cada documento MUST contener `schemaVersion` con valor `1`, `targetLocale` como etiqueta BCP-47 del idioma
del documento, y `phrases` como lista en orden de catálogo. No hay documentos de traducción: el texto de origen
de un idioma es el `target` de su propio documento. Documentos de idiomas distintos MUST poder convivir sin que
uno afecte la carga de otro.

#### Scenario: Versión del esquema

- **GIVEN** el catálogo empaquetado con `schemaVersion` igual a 1
- **WHEN** se carga el catálogo
- **THEN** se acepta sin error

#### Scenario: Esquema desconocido se rechaza

- **GIVEN** un documento de frases con `schemaVersion` igual a 2
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con un error que indica la versión no soportada

#### Scenario: Dos idiomas destino conviven de forma independiente

- **GIVEN** un documento de frases destino `pt-PT` válido y un documento de frases destino `en-GB`
  con un error de validación
- **WHEN** se cargan ambos
- **THEN** el catálogo `pt-PT` se carga correctamente y el error de `en-GB` no afecta a su carga

#### Scenario: El catálogo empaquetado no contiene documentos de traducción

- **GIVEN** los assets de catálogo empaquetados con la app
- **WHEN** se enumeran sus documentos
- **THEN** todos son documentos de frases y ninguno tiene `sourceLocale` en la raíz

## ADDED Requirements

### Requirement: Portugués europeo como única variante de portugués

El catálogo empaquetado MUST contener un único documento de portugués, con `targetLocale` igual a `pt-PT`.
El catálogo empaquetado MUST NOT contener ningún documento con `targetLocale` igual a `pt-BR`.

#### Scenario: El catálogo empaquetado solo tiene portugués europeo

- **GIVEN** los assets de catálogo empaquetados con la app
- **WHEN** se enumeran los `targetLocale` de sus documentos
- **THEN** `pt-PT` aparece una vez y `pt-BR` no aparece
