# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
