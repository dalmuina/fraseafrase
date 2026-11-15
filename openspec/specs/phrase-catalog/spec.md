# phrase-catalog Specification

## Purpose

Define el contenido del catálogo de frases precargado en la app: su formato, sus locales BCP-47, los campos obligatorios de cada frase y las reglas de ampliación (ids permanentes, sin reutilización ni borrado, alta al final), según ADR-0003 y ADR-0013. El catálogo es inmutable en tiempo de ejecución y solo cambia con una nueva versión de la app.

## Requirements

### Requirement: Formato del catálogo

El catálogo MUST almacenarse como uno o más documentos JSON empaquetados con la app, uno por idioma
destino. La raíz de cada documento MUST contener `schemaVersion` con valor `1`, `targetLocale` y
`sourceLocale` como etiquetas BCP-47, y `phrases` como lista. El orden de la lista `phrases` MUST ser
el orden de catálogo. Documentos de idiomas destino distintos MUST poder convivir sin que uno afecte
la carga del otro.

#### Scenario: Versión del esquema

- **GIVEN** el catálogo empaquetado con `schemaVersion` igual a 1
- **WHEN** se carga el catálogo
- **THEN** se acepta sin error

#### Scenario: Esquema desconocido se rechaza

- **GIVEN** un catálogo con `schemaVersion` igual a 2
- **WHEN** se carga el catálogo
- **THEN** la carga falla con un error que indica la versión no soportada

#### Scenario: Dos idiomas destino conviven de forma independiente

- **GIVEN** un documento de catálogo `pt-PT` válido y un documento de catálogo `pt-BR` con un error
  de validación
- **WHEN** se cargan ambos
- **THEN** el catálogo `pt-PT` se carga correctamente y el error de `pt-BR` no afecta a su carga

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

Un `id` del catálogo MUST NOT reutilizarse para otra frase ni borrarse en una versión posterior de la
app, **dentro del mismo idioma destino**. Las frases nuevas MUST añadirse al final de la lista
`phrases` de su idioma destino. La permanencia de ids de un idioma destino MUST verificarse de forma
independiente de la de cualquier otro idioma destino.

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

#### Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga

- **GIVEN** un idioma destino `pt-BR` que no tiene ningún id publicado todavía
- **WHEN** se ejecuta el test de contrato para `pt-BR`
- **THEN** la comprobación de contrato pasa sin exigir ningún id previo

#### Scenario: Retirar un id de un idioma destino no afecta al contrato de otro

- **GIVEN** `pt-PT` con sus ids publicados intactos y `pt-BR` que retira uno de los suyos
- **WHEN** se ejecuta el test de contrato de cada idioma destino
- **THEN** el contrato de `pt-PT` pasa y el de `pt-BR` devuelve el id retirado, sin que uno influya en el otro

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

### Requirement: Existe una única instancia de `CatalogRepository` en el grafo de la aplicación

En el grafo de la aplicación, toda obtención de `CatalogRepository` MUST devolver la misma instancia. La carga única del
asset la cubre `AssetsCatalogDataSourceTest` con contadores de lecturas, no este requisito.

#### Scenario: Dos obtenciones del catálogo desde el grafo de producción devuelven la misma instancia

- **GIVEN** la aplicación con su grafo de Hilt de producción (`PersistenceModule`)
- **WHEN** se obtiene `CatalogRepository` dos veces
- **THEN** ambas referencias son la misma instancia

### Requirement: Idiomas destino disponibles

El sistema MUST ofrecer la lista de idiomas destino disponibles a partir de los catálogos que se
cargaron correctamente. Un idioma destino cuyo catálogo falló al cargar MUST NOT aparecer en esa
lista.

#### Scenario: Dos idiomas destino cargados correctamente están disponibles

- **GIVEN** los catálogos `pt-PT` y `pt-BR`, ambos válidos
- **WHEN** se consultan los idiomas destino disponibles
- **THEN** la lista contiene `pt-PT` y `pt-BR`

#### Scenario: Un idioma destino con catálogo inválido no está disponible

- **GIVEN** el catálogo `pt-PT` válido y el catálogo `pt-BR` con un error de validación
- **WHEN** se consultan los idiomas destino disponibles
- **THEN** la lista contiene solo `pt-PT`

### Requirement: Resolución del idioma de origen por defecto

El idioma de origen de las frases MUST NOT fijarse en código a un valor concreto. Para un idioma
destino dado, el sistema MUST usar como origen el locale del dispositivo si existe contenido de
catálogo con ese `sourceLocale` para ese idioma destino, y MUST usar `es-ES` como valor por defecto
en caso contrario.

#### Scenario: El locale del dispositivo coincide con el origen disponible

- **GIVEN** un catálogo de `pt-PT` cuyo `sourceLocale` es `es-ES` y un dispositivo con locale `es-ES`
- **WHEN** se resuelve el idioma de origen para `pt-PT`
- **THEN** el idioma de origen resuelto es `es-ES`

#### Scenario: El locale del dispositivo no tiene contenido disponible

- **GIVEN** un catálogo de `pt-PT` cuyo único `sourceLocale` disponible es `es-ES` y un dispositivo
  con locale `en-US`
- **WHEN** se resuelve el idioma de origen para `pt-PT`
- **THEN** el idioma de origen resuelto es `es-ES`

### Requirement: Persistencia del catálogo en Room

El contenido de catálogo validado MUST persistirse en tablas de consulta locales, sembradas a partir
de los documentos JSON empaquetados. El sembrado de un idioma destino MUST NOT modificar el progreso
del usuario de ningún idioma destino.

#### Scenario: Primer arranque siembra el catálogo

- **GIVEN** una instalación nueva sin catálogo sembrado y los documentos JSON de `pt-PT` y `pt-BR`
- **WHEN** la app arranca
- **THEN** las frases de ambos idiomas destino quedan disponibles para consulta

#### Scenario: Un cambio de `schemaVersion` vuelve a sembrar sin tocar el progreso

- **GIVEN** un idioma destino ya sembrado, con progreso guardado para varias de sus frases, y una
  versión nueva de la app cuyo documento JSON de ese idioma cambia de `schemaVersion`
- **WHEN** la app arranca
- **THEN** el catálogo sembrado de ese idioma se actualiza y el progreso guardado de sus frases no
  cambia

#### Scenario: Sin cambio de `schemaVersion` no se resiembra

- **GIVEN** un idioma destino ya sembrado cuyo documento JSON no cambió de `schemaVersion` desde el
  último arranque
- **WHEN** la app arranca
- **THEN** el catálogo sembrado de ese idioma no se vuelve a sembrar
