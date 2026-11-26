## ADDED Requirements

### Requirement: Formato de los ids

Cada `id` de frase destino MUST tener la forma `<bloque>-<nivel>-<tema>-<nnn>`: un bloque de 4 dígitos, un nivel
CEFR en minúsculas (`a1`, `a2`, `b1`, `b2` o `c1`), un tema de letras minúsculas con palabras separadas por `_`, y
un número de 3 dígitos entre `001` y `999`. El bloque MUST figurar en el registro de bloques y el tema MUST figurar
en la lista cerrada de temas. El test de contrato MUST comprobarlo en todos los documentos de frases empaquetados.

#### Scenario: Un id con formato válido cumple el contrato

- **GIVEN** el registro de bloques `0000`, la lista de temas `saludos` y un documento con el id `0000-a1-saludos-001`
- **WHEN** se ejecuta el test de contrato del formato
- **THEN** la comprobación no devuelve ningún id

#### Scenario: Un id sin bloque incumple el contrato

- **GIVEN** el registro de bloques `0000`, la lista de temas `saludos` y un documento con el id `a1-saludos-001`
- **WHEN** se ejecuta el test de contrato del formato
- **THEN** la comprobación devuelve `a1-saludos-001` como id con formato inválido

#### Scenario: Un id con número 000 incumple el contrato

- **GIVEN** el registro de bloques `0000`, la lista de temas `saludos` y un documento con el id `0000-a1-saludos-000`
- **WHEN** se ejecuta el test de contrato del formato
- **THEN** la comprobación devuelve `0000-a1-saludos-000` como id con formato inválido

#### Scenario: Un bloque fuera del registro incumple el contrato

- **GIVEN** el registro de bloques `0000`, la lista de temas `saludos` y un documento con el id `1000-a1-saludos-001`
- **WHEN** se ejecuta el test de contrato del formato
- **THEN** la comprobación devuelve `1000-a1-saludos-001` como id con bloque no registrado

#### Scenario: Un tema fuera de la lista incumple el contrato

- **GIVEN** el registro de bloques `0000`, la lista de temas `saludos` y un documento con el id `0000-a1-viajes-001`
- **WHEN** se ejecuta el test de contrato del formato
- **THEN** la comprobación devuelve `0000-a1-viajes-001` como id con tema no registrado

#### Scenario: Los documentos empaquetados cumplen el formato

- **GIVEN** los documentos de frases empaquetados de los 7 idiomas, el registro de bloques y la lista de temas del
  repositorio
- **WHEN** se ejecuta el test de contrato del formato en cada documento
- **THEN** ninguna comprobación devuelve ids

### Requirement: Nivel y tema del id en las frases no publicadas

En una frase destino cuyo `id` no figura en el fichero de ids publicados de su idioma, el nivel y el tema del `id`
MUST coincidir con sus campos `level` y `topic`. En una frase publicada, el `id` MUST NOT cambiar aunque cambien
`level` o `topic`: el nivel y el tema del id indican dónde nació la frase, y no se comprueban.

#### Scenario: Una frase nueva cuyo nivel no coincide con su id incumple el contrato

- **GIVEN** un documento con la frase `0000-a1-saludos-003`, de nivel A2 y tema `saludos`, que no está en los ids
  publicados
- **WHEN** se ejecuta el test de contrato de la etiqueta de nacimiento
- **THEN** la comprobación devuelve `0000-a1-saludos-003`

#### Scenario: Una frase nueva cuyo tema no coincide con su id incumple el contrato

- **GIVEN** un documento con la frase `0000-a1-saludos-003`, de nivel A1 y tema `comida`, que no está en los ids
  publicados
- **WHEN** se ejecuta el test de contrato de la etiqueta de nacimiento
- **THEN** la comprobación devuelve `0000-a1-saludos-003`

#### Scenario: Una frase publicada reclasificada conserva su id

- **GIVEN** un documento con la frase `0000-a1-saludos-001`, de nivel A2 y tema `comida`, que está en los ids
  publicados
- **WHEN** se ejecuta el test de contrato de la etiqueta de nacimiento
- **THEN** la comprobación no devuelve ningún id

#### Scenario: Las frases empaquetadas no publicadas coinciden con su id

- **GIVEN** los documentos de frases empaquetados de los 7 idiomas y sus ficheros de ids publicados
- **WHEN** se ejecuta el test de contrato de la etiqueta de nacimiento en cada documento
- **THEN** ninguna comprobación devuelve ids

## MODIFIED Requirements

### Requirement: Texto de origen desde el documento de frases del idioma de origen

Para un idioma destino y un idioma de origen distintos, el texto de origen de cada frase MUST ser el `target` de
la frase con el mismo `id` en el documento de frases del idioma de origen. El nivel CEFR, el tema y el texto
destino MUST salir del documento del idioma destino. Cualquier idioma disponible MUST poder usarse como origen
de cualquier otro idioma disponible.

#### Scenario: El origen sale del texto destino del otro idioma

- **GIVEN** un documento de frases `pt-PT` con la frase `0000-a1-saludos-001` de texto "Bom dia" y nivel A1, y un
  documento de frases `es-ES` con la frase `0000-a1-saludos-001` de texto "Buenos días" y nivel A2
- **WHEN** se consultan las frases de `pt-PT` con origen `es-ES`
- **THEN** la frase `0000-a1-saludos-001` tiene texto destino "Bom dia", texto de origen "Buenos días", nivel A1,
  `targetLocale` `pt-PT` y `sourceLocale` `es-ES`

#### Scenario: Un par sin fichero propio está disponible

- **GIVEN** documentos de frases `fr-FR` y `de-DE` válidos con los mismos ids
- **WHEN** se consultan las frases de `de-DE` con origen `fr-FR`
- **THEN** se devuelven todas las frases de `de-DE`, cada una con el texto `fr-FR` de su mismo id como origen

#### Scenario: Un idioma de origen con documento inválido no devuelve frases

- **GIVEN** un documento de frases `pt-PT` válido y un documento de frases `it-IT` con un error de validación
- **WHEN** se consultan las frases de `pt-PT` con origen `it-IT`
- **THEN** la consulta devuelve una lista vacía de frases

### Requirement: Ids únicos y no vacíos por documento de frases

Cada frase destino MUST tener un `id` no vacío, y ningún `id` MUST aparecer más de una vez dentro
del mismo documento de frases.

#### Scenario: Ids duplicados rechazan la carga

- **GIVEN** un documento de frases destino con dos frases de id `0000-a1-saludos-001`
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla e indica el id duplicado

#### Scenario: Id vacío rechaza la carga

- **GIVEN** una frase destino con `id` igual a una cadena vacía
- **WHEN** se carga el catálogo
- **THEN** la carga de ese documento falla

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

- **GIVEN** `pt-PT` ya sembrado con la frase `0000-a1-saludos-001` y progreso guardado para ella con nivel 2 y 1
  acierto
- **WHEN** la app arranca con un documento de `pt-PT` que añade una segunda frase al final
- **THEN** el progreso guardado de `0000-a1-saludos-001` sigue con nivel 2 y 1 acierto
