# Spec Delta

## MODIFIED Requirements

### Requirement: Formato del catálogo

El catálogo MUST almacenarse como documentos JSON empaquetados con la app, de dos tipos:

- **Documento de frases destino**, uno por idioma destino: su raíz MUST contener `schemaVersion`
  con valor `1`, `targetLocale` como etiqueta BCP-47, y `phrases` como lista en orden de catálogo.
  No MUST contener `sourceLocale` ni texto de origen.
- **Documento de traducción**, uno por combinación de idioma destino y origen: su raíz MUST
  contener `schemaVersion` con valor `1`, `targetLocale` y `sourceLocale` como etiquetas BCP-47, y
  `translations` como lista.

Documentos de idiomas destino distintos, o de combinaciones destino/origen distintas, MUST poder
convivir sin que uno afecte la carga de otro.

#### Scenario: Versión del esquema

- **GIVEN** el catálogo empaquetado con `schemaVersion` igual a 1
- **WHEN** se carga el catálogo
- **THEN** se acepta sin error

#### Scenario: Versión del esquema de un documento de traducción

- **GIVEN** un documento de traducción empaquetado con `schemaVersion` igual a 1
- **WHEN** se carga el catálogo
- **THEN** se acepta sin error

#### Scenario: Esquema desconocido se rechaza

- **GIVEN** un documento, de frases destino o de traducción, con `schemaVersion` igual a 2
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con un error que indica la versión no soportada

#### Scenario: Dos idiomas destino conviven de forma independiente

- **GIVEN** un documento de frases destino `pt-PT` válido y un documento de frases destino `pt-BR`
  con un error de validación
- **WHEN** se cargan ambos
- **THEN** el catálogo `pt-PT` se carga correctamente y el error de `pt-BR` no afecta a su carga

#### Scenario: Dos orígenes del mismo destino conviven de forma independiente

- **GIVEN** un documento de frases destino `pt-PT` válido, un documento de traducción
  `pt-PT`/`es-ES` válido y un documento de traducción `pt-PT`/`en-GB` con un error de validación
- **WHEN** se cargan los tres
- **THEN** las frases de `pt-PT` con origen `es-ES` se cargan correctamente y el error del
  documento de traducción `pt-PT`/`en-GB` no afecta a su carga

### Requirement: Locales por frase

Cada frase destino MUST heredar `targetLocale` de la raíz de su documento de frases destino. Cada
traducción MUST heredar `targetLocale` y `sourceLocale` de la raíz de su documento de traducción.
Los locales MUST ser etiquetas BCP-47 no vacías. Un locale vacío o ausente, en cualquiera de los dos
tipos de documento, MUST rechazar la carga de ese documento.

#### Scenario: Frase hereda los locales de la raíz

- **GIVEN** un documento de frases destino con `targetLocale` `pt-PT`
- **WHEN** se cargan sus frases
- **THEN** cada frase tiene `targetLocale` `pt-PT`

#### Scenario: Traducción hereda los locales de la raíz

- **GIVEN** un documento de traducción con `targetLocale` `pt-PT` y `sourceLocale` `es-ES`
- **WHEN** se cargan sus traducciones
- **THEN** cada traducción tiene `targetLocale` `pt-PT` y `sourceLocale` `es-ES`

#### Scenario: Locale vacío rechaza la carga

- **GIVEN** un documento de frases destino con `targetLocale` vacío
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `InvalidLocale` y no se devuelve ninguna frase de él

#### Scenario: Locale con formato no BCP-47 rechaza la carga

- **GIVEN** un documento de frases destino con `targetLocale` igual a `pt_PT`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `InvalidLocale`

#### Scenario: Locale ausente rechaza la carga

- **GIVEN** un documento de traducción sin el campo `sourceLocale` en la raíz
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `InvalidLocale`

### Requirement: Campos obligatorios de cada frase

Cada frase destino MUST tener `id`, `level`, `topic` y `target`. `level` MUST ser uno de A1, A2,
B1, B2 o C1. `topic` y `target` MUST ser textos no vacíos. Cada traducción MUST tener `id` y
`source`; `source` MUST ser un texto no vacío.

#### Scenario: Nivel CEFR inválido rechaza la carga

- **GIVEN** una frase destino con `level` igual a `D1`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica la frase afectada

#### Scenario: Texto vacío rechaza la carga

- **GIVEN** una frase destino con `target` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica la frase afectada

#### Scenario: Topic vacío rechaza la carga

- **GIVEN** una frase destino con `topic` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica la frase afectada

#### Scenario: Texto de traducción vacío rechaza la carga

- **GIVEN** una traducción con `source` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento de traducción falla e indica la traducción afectada

### Requirement: Ids únicos y no vacíos

Cada frase destino MUST tener un `id` no vacío, y ningún `id` MUST aparecer más de una vez dentro
del mismo documento de frases destino. Cada traducción MUST tener un `id` no vacío que referencia
una frase destino de ese idioma destino, y ningún `id` MUST aparecer más de una vez dentro del
mismo documento de traducción. Un `id` de traducción sin frase destino correspondiente MUST
rechazar la carga de ese documento de traducción.

#### Scenario: Ids duplicados rechazan la carga

- **GIVEN** un documento de frases destino con dos frases de id `a1-saludos-001`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica el id duplicado

#### Scenario: Ids duplicados rechazan la carga de un documento de traducción

- **GIVEN** un documento de traducción con dos traducciones de id `a1-saludos-001`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica el id duplicado

#### Scenario: Id vacío rechaza la carga

- **GIVEN** una frase destino con `id` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla

#### Scenario: Traducción sin frase destino correspondiente rechaza la carga

- **GIVEN** un documento de frases destino `pt-PT` sin el id `a1-saludos-999`, y un documento de
  traducción `pt-PT`/`es-ES` que incluye una traducción con ese id
- **WHEN** se carga el catálogo
- **THEN** la carga del documento de traducción falla e indica el id sin frase destino
  correspondiente

### Requirement: Permanencia de los ids

Un `id` de frase destino MUST NOT reutilizarse para otra frase ni borrarse en una versión
posterior de la app, **dentro del mismo idioma destino**. Las frases destino nuevas MUST añadirse
al final de la lista `phrases` de su idioma destino. La permanencia de ids de un idioma destino
MUST verificarse de forma independiente de la de cualquier otro idioma destino, y con
independencia de cuántos documentos de traducción tenga ese idioma destino.

#### Scenario: Alta de frase nueva al final

- **GIVEN** un documento de frases destino de la versión anterior con las frases A y B
- **WHEN** una versión posterior añade la frase C
- **THEN** la lista queda A, B, C y los ids de A y B no cambian

#### Scenario: Ids publicados siguen presentes y en el mismo orden

- **GIVEN** un fichero de referencia versionado con los ids publicados en versiones anteriores, en
  orden: A, B
- **WHEN** se carga el catálogo actual, que contiene A, B, C
- **THEN** los ids publicados son un prefijo de la lista actual y ningún id publicado falta

#### Scenario: Catálogo que quita un id publicado devuelve el id retirado

- **GIVEN** el fichero de referencia con los ids A, B y un catálogo actual que solo contiene A
- **WHEN** se ejecuta el test de contrato
- **THEN** la comprobación de contrato devuelve el id B como retirado

#### Scenario: Un idioma destino nuevo sin ids publicados no bloquea su primera carga

- **GIVEN** un idioma destino `it-IT` que no tiene ningún id publicado todavía
- **WHEN** se ejecuta el test de contrato para `it-IT`
- **THEN** la comprobación de contrato pasa sin exigir ningún id previo

#### Scenario: Retirar un id de un idioma destino no afecta al contrato de otro

- **GIVEN** `pt-PT` con sus ids publicados intactos y `it-IT` que retira uno de los suyos
- **WHEN** se ejecuta el test de contrato de cada idioma destino
- **THEN** el contrato de `pt-PT` pasa y el de `it-IT` devuelve el id retirado, sin que uno influya
  en el otro

#### Scenario: Añadir un documento de traducción nuevo no afecta la permanencia de ids de frases destino

- **GIVEN** `pt-PT` con sus ids publicados intactos y un documento de traducción `pt-PT`/`en-GB`
  añadido en la misma versión
- **WHEN** se ejecuta el test de contrato de `pt-PT`
- **THEN** el contrato pasa igual que si no se hubiera añadido ese documento de traducción

### Requirement: Asset de catálogo no legible

Si el asset de un documento de catálogo, de frases destino o de traducción, no puede leerse, la
carga de ese documento MUST fallar y no MUST devolver ninguna frase ni traducción de él.

#### Scenario: Asset no legible falla la carga

- **GIVEN** un asset de documento de frases destino que produce un error de lectura
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `AssetUnreadable` y no se devuelve ninguna frase de
  él

#### Scenario: Asset de traducción no legible falla la carga

- **GIVEN** un asset de documento de traducción que produce un error de lectura
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `AssetUnreadable` y no se devuelve ninguna
  traducción de él

### Requirement: Versión del esquema ausente

Un documento de catálogo, de frases destino o de traducción, sin `schemaVersion` MUST rechazar la
carga de ese documento.

#### Scenario: Sin schemaVersion rechaza la carga

- **GIVEN** un documento de frases destino sin el campo `schemaVersion`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con un error de versión ausente

### Requirement: Documento malformado

Si el contenido de un documento de catálogo no es un documento JSON válido, la carga de ese
documento MUST fallar con `MalformedDocument`. Este error es distinto de `AssetUnreadable`: el
asset se leyó, pero su contenido no se puede interpretar.

#### Scenario: Texto que no es JSON rechaza la carga

- **GIVEN** un documento de frases destino cuyo contenido no es JSON válido
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `MalformedDocument` y no se devuelve ninguna frase
  de él

### Requirement: Existe una única instancia de `CatalogRepository` en el grafo de la aplicación

En el grafo de la aplicación, toda obtención de `CatalogRepository` MUST devolver la misma
instancia. La carga única del asset la cubre `AssetsCatalogDataSourceTest` con contadores de
lecturas, no este requisito.

#### Scenario: Dos obtenciones del catálogo desde el grafo de producción devuelven la misma instancia

- **GIVEN** la aplicación con su grafo de Hilt de producción (`PersistenceModule`)
- **WHEN** se obtiene `CatalogRepository` dos veces
- **THEN** ambas referencias son la misma instancia

### Requirement: Idiomas destino disponibles

El sistema MUST ofrecer la lista de idiomas destino disponibles a partir de los documentos de
frases destino que se cargaron correctamente. Un idioma destino cuyo documento de frases destino
falló al cargar MUST NOT aparecer en esa lista, aunque alguno de sus documentos de traducción haya
cargado correctamente.

#### Scenario: Dos idiomas destino cargados correctamente están disponibles

- **GIVEN** los documentos de frases destino `pt-PT` y `it-IT`, ambos válidos
- **WHEN** se consultan los idiomas destino disponibles
- **THEN** la lista contiene `pt-PT` e `it-IT`

#### Scenario: Un idioma destino con catálogo inválido no está disponible

- **GIVEN** el documento de frases destino `pt-PT` válido y el documento de frases destino `it-IT`
  con un error de validación
- **WHEN** se consultan los idiomas destino disponibles
- **THEN** la lista contiene solo `pt-PT`

### Requirement: Resolución del idioma de origen por defecto

El idioma de origen de las frases MUST NOT fijarse en código a un valor concreto. Para un idioma
destino dado, el sistema MUST usar como origen el locale del dispositivo si existe un documento de
traducción con ese `sourceLocale` para ese idioma destino. En caso contrario, el sistema MUST usar
como respaldo un idioma de origen disponible para ese idioma destino, y ese respaldo MUST NOT ser
igual al propio idioma destino que se está resolviendo.

#### Scenario: El locale del dispositivo coincide con el origen disponible

- **GIVEN** un documento de traducción de `pt-PT` cuyo `sourceLocale` es `es-ES` y un dispositivo
  con locale `es-ES`
- **WHEN** se resuelve el idioma de origen para `pt-PT`
- **THEN** el idioma de origen resuelto es `es-ES`

#### Scenario: El locale del dispositivo no tiene contenido disponible

- **GIVEN** un documento de traducción de `pt-PT` cuyo único `sourceLocale` disponible es `es-ES`
  y un dispositivo con locale `en-GB`
- **WHEN** se resuelve el idioma de origen para `pt-PT`
- **THEN** el idioma de origen resuelto es `es-ES`

#### Scenario: El respaldo no coincide con el propio idioma destino

- **GIVEN** un documento de traducción de `es-ES` cuyo único `sourceLocale` disponible es `pt-PT` y
  un dispositivo con locale `es-ES`
- **WHEN** se resuelve el idioma de origen para `es-ES`
- **THEN** el idioma de origen resuelto es `pt-PT`, no `es-ES`

### Requirement: Persistencia del catálogo en Room

El contenido de catálogo validado MUST persistirse en tablas de consulta locales, sembradas a
partir de los documentos JSON empaquetados: las frases destino de un idioma, y las traducciones de
cada combinación destino/origen, se siembran y versionan de forma independiente entre sí. El
sembrado de un documento, de cualquiera de los dos tipos, MUST NOT modificar el progreso del
usuario de ningún idioma destino, ni las frases destino o traducciones sembradas de otro idioma
destino u otra combinación destino/origen.

#### Scenario: Primer arranque siembra el catálogo

- **GIVEN** una instalación nueva sin catálogo sembrado y los documentos de frases destino de
  `pt-PT` e `it-IT` junto con sus documentos de traducción
- **WHEN** la app arranca
- **THEN** las frases de ambos idiomas destino quedan disponibles para consulta con su traducción

#### Scenario: Un cambio de `schemaVersion` vuelve a sembrar sin tocar el progreso

- **GIVEN** un idioma destino ya sembrado, con progreso guardado para varias de sus frases, y una
  versión nueva de la app cuyo documento de frases destino de ese idioma cambia de `schemaVersion`
- **WHEN** la app arranca
- **THEN** las frases destino sembradas de ese idioma se actualizan, sus traducciones sembradas no
  cambian, y el progreso guardado de sus frases no cambia

#### Scenario: Un cambio de `schemaVersion` en un documento de traducción vuelve a sembrarlo sin tocar las frases destino ni otros orígenes

- **GIVEN** un idioma destino con dos orígenes sembrados (`es-ES` y `en-GB`) y una versión nueva
  de la app cuyo documento de traducción `es-ES` cambia de `schemaVersion`
- **WHEN** la app arranca
- **THEN** las traducciones sembradas de `es-ES` para ese idioma destino se actualizan, las frases
  destino sembradas de ese idioma no cambian, y las traducciones sembradas de `en-GB` no cambian

#### Scenario: Sin cambio de `schemaVersion` no se resiembra

- **GIVEN** un idioma destino ya sembrado cuyos documentos de frases destino y de traducción no
  cambiaron de `schemaVersion` desde el último arranque
- **WHEN** la app arranca
- **THEN** ninguno de los documentos sembrados de ese idioma destino se vuelve a sembrar
