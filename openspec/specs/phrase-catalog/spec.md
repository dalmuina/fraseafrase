# phrase-catalog Specification

## Purpose

Define el contenido del catálogo de frases precargado en la app: su formato, sus locales BCP-47, los campos obligatorios de cada frase y las reglas de ampliación (ids permanentes, sin reutilización ni borrado, alta al final), según ADR-0003 y ADR-0013. El catálogo es inmutable en tiempo de ejecución y solo cambia con una nueva versión de la app.

## Requirements

### Requirement: Versión del esquema ausente

Un documento de frases sin `schemaVersion` MUST rechazar la carga de ese documento.

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

El sistema MUST ofrecer la lista de idiomas disponibles a partir de los documentos de frases que se cargaron
correctamente. Esa misma lista MUST usarse para elegir el idioma destino y el idioma de origen. Un idioma cuyo
documento de frases falló al cargar MUST NOT aparecer en esa lista.

#### Scenario: Dos idiomas destino cargados correctamente están disponibles

- **GIVEN** los documentos de frases destino `pt-PT` y `it-IT`, ambos válidos
- **WHEN** se consultan los idiomas destino disponibles
- **THEN** la lista contiene `pt-PT` e `it-IT`

#### Scenario: Un idioma destino con catálogo inválido no está disponible

- **GIVEN** el documento de frases destino `pt-PT` válido y el documento de frases destino `it-IT`
  con un error de validación
- **WHEN** se consultan los idiomas destino disponibles
- **THEN** la lista contiene solo `pt-PT`

### Requirement: Texto de origen desde el documento de frases del idioma de origen

Para un idioma destino y un idioma de origen distintos, el texto de origen de cada frase MUST ser el `target` de
la frase con el mismo `id` en el documento de frases del idioma de origen. El nivel CEFR, el tema y el texto
destino MUST salir del documento del idioma destino. Cualquier idioma disponible MUST poder usarse como origen
de cualquier otro idioma disponible.

#### Scenario: El origen sale del texto destino del otro idioma

- **GIVEN** un documento de frases `pt-PT` con la frase `a1-saludos-001` de texto "Bom dia" y nivel A1, y un
  documento de frases `es-ES` con la frase `a1-saludos-001` de texto "Buenos días" y nivel A2
- **WHEN** se consultan las frases de `pt-PT` con origen `es-ES`
- **THEN** la frase `a1-saludos-001` tiene texto destino "Bom dia", texto de origen "Buenos días", nivel A1,
  `targetLocale` `pt-PT` y `sourceLocale` `es-ES`

#### Scenario: Un par sin fichero propio está disponible

- **GIVEN** documentos de frases `fr-FR` y `de-DE` válidos con los mismos ids
- **WHEN** se consultan las frases de `de-DE` con origen `fr-FR`
- **THEN** se devuelven todas las frases de `de-DE`, cada una con el texto `fr-FR` de su mismo id como origen

#### Scenario: Un idioma de origen con documento inválido no devuelve frases

- **GIVEN** un documento de frases `pt-PT` válido y un documento de frases `it-IT` con un error de validación
- **WHEN** se consultan las frases de `pt-PT` con origen `it-IT`
- **THEN** la consulta devuelve una lista vacía de frases

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

### Requirement: Ids paralelos entre idiomas

Todos los documentos de frases empaquetados MUST contener exactamente el mismo conjunto de ids. Un test de
contrato sobre los assets empaquetados MUST fallar e indicar el idioma y los ids que faltan o sobran si algún
documento no cumple esta regla.

#### Scenario: Documentos con los mismos ids cumplen el contrato

- **GIVEN** los documentos de frases `pt-PT` y `es-ES`, ambos con los ids A, B y C
- **WHEN** se ejecuta el contrato de ids paralelos
- **THEN** el contrato pasa

#### Scenario: Un documento al que le falta un id incumple el contrato

- **GIVEN** el documento de frases `pt-PT` con los ids A, B y C y el documento de frases `es-ES` con los ids A y B
- **WHEN** se ejecuta el contrato de ids paralelos
- **THEN** el contrato falla e indica que a `es-ES` le falta el id C

#### Scenario: Un documento con un id de más incumple el contrato

- **GIVEN** el documento de frases `pt-PT` con los ids A y B y el documento de frases `es-ES` con los ids A, B y D
- **WHEN** se ejecuta el contrato de ids paralelos
- **THEN** el contrato falla e indica que `es-ES` tiene el id D que no está en `pt-PT`

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

### Requirement: Locale de cada frase

Cada frase MUST heredar `targetLocale` de la raíz de su documento de frases. Los locales MUST ser etiquetas
BCP-47 no vacías. Un locale vacío, ausente o mal formado MUST rechazar la carga de ese documento.

#### Scenario: Frase hereda los locales de la raíz

- **GIVEN** un documento de frases destino con `targetLocale` `pt-PT`
- **WHEN** se cargan sus frases
- **THEN** cada frase tiene `targetLocale` `pt-PT`

#### Scenario: Locale vacío rechaza la carga

- **GIVEN** un documento de frases destino con `targetLocale` vacío
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `InvalidLocale` y no se devuelve ninguna frase de él

#### Scenario: Locale con formato no BCP-47 rechaza la carga

- **GIVEN** un documento de frases destino con `targetLocale` igual a `pt_PT`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `InvalidLocale`

#### Scenario: Locale ausente rechaza la carga

- **GIVEN** un documento de frases sin el campo `targetLocale` en la raíz
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `InvalidLocale`

### Requirement: Campos obligatorios de cada frase destino

Cada frase destino MUST tener `id`, `level`, `topic` y `target`. `level` MUST ser uno de A1, A2,
B1, B2 o C1. `topic` y `target` MUST ser textos no vacíos.

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

### Requirement: Ids únicos y no vacíos por documento de frases

Cada frase destino MUST tener un `id` no vacío, y ningún `id` MUST aparecer más de una vez dentro
del mismo documento de frases.

#### Scenario: Ids duplicados rechazan la carga

- **GIVEN** un documento de frases destino con dos frases de id `a1-saludos-001`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica el id duplicado

#### Scenario: Id vacío rechaza la carga

- **GIVEN** una frase destino con `id` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla

### Requirement: Permanencia de los ids por idioma

Un `id` de frase destino MUST NOT reutilizarse para otra frase ni borrarse en una versión
posterior de la app, **dentro del mismo idioma destino**. Las frases destino nuevas MUST añadirse
al final de la lista `phrases` de su idioma destino. La permanencia de ids de un idioma destino
MUST verificarse de forma independiente de la de cualquier otro idioma destino.

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

### Requirement: Documento de frases no legible

Si el asset de un documento de frases no puede leerse, la carga de ese documento MUST fallar y no MUST
devolver ninguna frase de él.

#### Scenario: Asset no legible falla la carga

- **GIVEN** un asset de documento de frases destino que produce un error de lectura
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla con `AssetUnreadable` y no se devuelve ninguna frase de
  él

### Requirement: Portugués europeo como única variante de portugués

El catálogo empaquetado MUST contener un único documento de portugués, con `targetLocale` igual a `pt-PT`.
El catálogo empaquetado MUST NOT contener ningún documento con `targetLocale` igual a `pt-BR`.

#### Scenario: El catálogo empaquetado solo tiene portugués europeo

- **GIVEN** los assets de catálogo empaquetados con la app
- **WHEN** se enumeran los `targetLocale` de sus documentos
- **THEN** `pt-PT` aparece una vez y `pt-BR` no aparece

### Requirement: Persistencia y resembrado del catálogo en Room

El contenido de catálogo validado MUST persistirse en tablas de consulta locales, sembradas desde los documentos de
frases empaquetados. Cada idioma MUST sembrarse de forma independiente y volver a sembrarse cuando cambie el contenido
de su documento, aunque no cambie `schemaVersion`. El sembrado MUST NOT modificar el progreso de ningún idioma destino
ni las frases sembradas de otro idioma.

#### Scenario: Primer arranque siembra el catálogo

- **GIVEN** una instalación nueva sin catálogo sembrado y los documentos de frases de `pt-PT`, `it-IT` y `es-ES`
- **WHEN** la app arranca
- **THEN** las frases de los tres idiomas quedan disponibles para consulta, cada una con el texto de origen de
  cualquiera de los otros dos

#### Scenario: Una frase añadida llega a una instalación ya sembrada

- **GIVEN** los idiomas `pt-PT` y `es-ES` ya sembrados con una frase cada uno
- **WHEN** la app arranca con documentos de ambos idiomas que añaden una segunda frase al final y mantienen
  `schemaVersion` igual a `1`
- **THEN** la consulta de `pt-PT` con origen `es-ES` devuelve las dos frases, en el orden del documento

#### Scenario: Un texto corregido sustituye al anterior sin afectar a otros idiomas

- **GIVEN** los idiomas `pt-PT`, `it-IT` y `es-ES` ya sembrados
- **WHEN** la app arranca con un documento de `pt-PT` que cambia el texto de una frase, y los otros documentos iguales
- **THEN** `pt-PT` devuelve el texto corregido e `it-IT` conserva sus frases sin volver a sembrarse

#### Scenario: Sin cambio de contenido no se vuelve a sembrar

- **GIVEN** un idioma ya sembrado cuyo documento de frases es idéntico al del último arranque
- **WHEN** la app arranca
- **THEN** ese documento no se vuelve a sembrar

#### Scenario: Un documento nuevo inválido conserva lo sembrado

- **GIVEN** un idioma ya sembrado
- **WHEN** la app arranca con un documento de ese idioma cuyo contenido cambió y no supera la validación
- **THEN** las frases sembradas de ese idioma siguen disponibles sin cambios

#### Scenario: Resembrar un idioma no cambia el progreso guardado

- **GIVEN** `pt-PT` ya sembrado con la frase `a1-saludos-001` y progreso guardado para ella con nivel 2 y 1 acierto
- **WHEN** la app arranca con un documento de `pt-PT` que añade una segunda frase al final
- **THEN** el progreso guardado de `a1-saludos-001` sigue con nivel 2 y 1 acierto
