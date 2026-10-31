# phrase-catalog Specification

## Purpose

Define el contenido del catálogo de frases precargado en la app: su formato, sus locales BCP-47, los campos obligatorios de cada frase y las reglas de ampliación (ids permanentes, sin reutilización ni borrado, alta al final), según ADR-0003 y ADR-0013. El catálogo es inmutable en tiempo de ejecución y solo cambia con una nueva versión de la app.

## Requirements

### Requirement: Formato del catálogo

El catálogo MUST almacenarse como un único documento JSON empaquetado con la app. Su raíz MUST contener `schemaVersion` con valor `1`, `targetLocale` y `sourceLocale` como etiquetas BCP-47, y `phrases` como lista. El orden de la lista `phrases` MUST ser el orden de catálogo.

#### Scenario: Versión del esquema

- **GIVEN** el catálogo empaquetado con `schemaVersion` igual a 1
- **WHEN** se carga el catálogo
- **THEN** se acepta sin error

#### Scenario: Esquema desconocido se rechaza

- **GIVEN** un catálogo con `schemaVersion` igual a 2
- **WHEN** se carga el catálogo
- **THEN** la carga falla con un error que indica la versión no soportada

### Requirement: Locales por frase

Cada frase MUST heredar `targetLocale` y `sourceLocale` de la raíz del catálogo. Los locales MUST ser etiquetas BCP-47 no vacías. Un locale vacío o ausente MUST rechazar la carga.

#### Scenario: Frase hereda los locales de la raíz

- **GIVEN** un catálogo con `targetLocale` `pt-PT` y `sourceLocale` `es-ES`
- **WHEN** se cargan sus frases
- **THEN** cada frase tiene `targetLocale` `pt-PT` y `sourceLocale` `es-ES`

#### Scenario: Locale vacío rechaza la carga

- **GIVEN** un catálogo con `targetLocale` vacío
- **WHEN** se carga el catálogo
- **THEN** la carga falla con `InvalidLocale` y no se devuelve ninguna frase

#### Scenario: Locale con formato no BCP-47 rechaza la carga

- **GIVEN** un catálogo con `targetLocale` igual a `pt_PT`
- **WHEN** se carga el catálogo
- **THEN** la carga falla con `InvalidLocale`

#### Scenario: Locale ausente rechaza la carga

- **GIVEN** un catálogo sin el campo `sourceLocale` en la raíz
- **WHEN** se carga el catálogo
- **THEN** la carga falla con `InvalidLocale`

### Requirement: Campos obligatorios de cada frase

Cada frase MUST tener `id`, `level`, `topic`, `target` y `source`. `level` MUST ser uno de A1, A2, B1, B2 o C1. `topic` MUST ser un texto no vacío. `target` y `source` MUST ser textos no vacíos.

#### Scenario: Nivel CEFR inválido rechaza la carga

- **GIVEN** una frase con `level` igual a `D1`
- **WHEN** se carga el catálogo
- **THEN** la carga falla e indica la frase afectada

#### Scenario: Texto vacío rechaza la carga

- **GIVEN** una frase con `target` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga falla e indica la frase afectada

#### Scenario: Topic vacío rechaza la carga

- **GIVEN** una frase con `topic` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga falla e indica la frase afectada

### Requirement: Ids únicos y no vacíos

Cada frase MUST tener un `id` no vacío, y ningún `id` MUST aparecer más de una vez en el catálogo.

#### Scenario: Ids duplicados rechazan la carga

- **GIVEN** un catálogo con dos frases de id `a1-saludos-001`
- **WHEN** se carga el catálogo
- **THEN** la carga falla e indica el id duplicado

#### Scenario: Id vacío rechaza la carga

- **GIVEN** una frase con `id` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga falla

### Requirement: Permanencia de los ids

Un `id` del catálogo MUST NOT reutilizarse para otra frase ni borrarse en una versión posterior de la app. Las frases nuevas MUST añadirse al final de la lista `phrases`.

#### Scenario: Alta de frase nueva al final

- **GIVEN** un catálogo de la versión anterior con las frases A y B
- **WHEN** una versión posterior añade la frase C
- **THEN** la lista queda A, B, C y los ids de A y B no cambian

#### Scenario: Ids publicados siguen presentes y en el mismo orden

- **GIVEN** un fichero de referencia versionado con los ids publicados en versiones anteriores, en orden: A, B
- **WHEN** se carga el catálogo actual, que contiene A, B, C
- **THEN** los ids publicados son un prefijo de la lista actual y ningún id publicado falta

#### Scenario: Catálogo que quita un id publicado devuelve el id retirado

- **GIVEN** el fichero de referencia con los ids A, B y un catálogo actual que solo contiene A
- **WHEN** se ejecuta el test de contrato
- **THEN** la comprobación de contrato devuelve el id B como retirado

### Requirement: Asset de catálogo no legible

Si el asset del catálogo no puede leerse, la carga MUST fallar y no MUST devolver ninguna frase.

#### Scenario: Asset no legible falla la carga

- **GIVEN** un asset de catálogo que produce un error de lectura
- **WHEN** se carga el catálogo
- **THEN** la carga falla con `AssetUnreadable` y no se devuelve ninguna frase

### Requirement: Versión del esquema ausente

Un catálogo sin `schemaVersion` MUST rechazar la carga.

#### Scenario: Sin schemaVersion rechaza la carga

- **GIVEN** un catálogo sin el campo `schemaVersion`
- **WHEN** se carga el catálogo
- **THEN** la carga falla con un error de versión ausente

### Requirement: Documento malformado

Si el contenido del catálogo no es un documento JSON válido, la carga MUST fallar con `MalformedDocument`. Este error es distinto de `AssetUnreadable`: el asset se leyó, pero su contenido no se puede interpretar.

#### Scenario: Texto que no es JSON rechaza la carga

- **GIVEN** un catálogo cuyo contenido no es JSON válido
- **WHEN** se carga el catálogo
- **THEN** la carga falla con `MalformedDocument` y no se devuelve ninguna frase
