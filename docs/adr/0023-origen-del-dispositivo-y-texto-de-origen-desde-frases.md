# ADR-0023: Idioma de origen del dispositivo y texto de origen desde el documento de frases

- **Estado:** Propuesto
- **Fecha:** 2026-11-22
- **Sustituye a:** ADR-0022 (la parte de documentos y tabla de traducción), ADR-0020 (por completo) y ADR-0019
  (solo la regla "idioma de origen derivado del dispositivo")

## Contexto

El catálogo tiene ocho documentos `phrases-<idioma>.json` (es-ES, pt-PT, pt-BR, en-GB, fr-FR, it-IT, de-DE,
ru-RU) que comparten los mismos ids: son frases paralelas, y el `target` de cada documento es ya una traducción
válida del `target` de cualquier otro con el mismo id.

ADR-0022 separó el texto de origen en documentos `translations-<destino>-from-<origen>.json` sembrados en
`catalog_translation`, y ADR-0019/ADR-0020 derivan el origen del dispositivo entre los pares que tienen ese
documento. Resultado: el origen no se puede elegir, solo existen los pares con un fichero de traducción (casi
todos desde `es-ES`), y cada texto de origen está duplicado respecto al `target` del documento de su idioma.
Un usuario con el móvil en un idioma del catálogo distinto del español no puede ver las frases en su idioma, y
ofrecer N idiomas como origen y destino exigiría N·(N−1) ficheros de traducción.

## Decisión

**Texto de origen.** El texto de origen de una frase es el `target` del documento de frases del idioma de origen
con el mismo id. Se eliminan los documentos de traducción, la tabla `catalog_translation` y el concepto de "par"
en el catálogo. La consulta de frases une `catalog_phrase` consigo misma por id (destino × origen).
`CatalogDatabase` sube de versión con `fallbackToDestructiveMigration()`: es una caché sembrada desde assets y no
contiene datos del usuario.

**Ids paralelos.** Todos los documentos de frases MUST tener exactamente el mismo conjunto de ids. Un test de
contrato lo verifica sobre los assets empaquetados. Añadir una frase es añadirla al final de todos los documentos
(ADR-0013); añadir un idioma es añadir un `phrases-<idioma>.json` con todos los ids, sin código ni ficheros por par.

**Idioma de origen.** Es único para toda la app y no depende del idioma destino. Es el locale del dispositivo si
está entre los idiomas del catálogo; si no, el idioma de origen por defecto de la app (hoy `es-ES`). No se elige ni
se guarda: esa es toda la lógica. El valor por defecto lo provee `:app` en el grafo de DI, de modo que el dominio no
contiene ningún idioma fijo (ADR-0003).

**Origen ≠ destino.** El origen es el ancla: el idioma destino activo es el guardado (o el primero disponible) si
es distinto del origen; si coincide, el primer idioma disponible distinto del origen. La pantalla de ajustes nunca
ofrece como destino el idioma de origen.

**Sin cambios:** catálogo en Room y selección de idioma destino (ADR-0019); progreso, nivel CEFR, N, tanda activa
y contador de sesión independientes por idioma destino, y por tanto independientes del origen.

## Consecuencias

- **Positivas:**
  - Cualquier idioma del catálogo es origen y destino de cualquier otro, con una única copia de cada texto.
  - Añadir un idioma es añadir un fichero; no hay ficheros ni filas por par.
  - Menos código: desaparecen un tipo de documento, su parser, una tabla y la siembra en dos pasadas.
- **Negativas:**
  - Un documento de frases incompleto rompería todos los pares que lo usan: lo evita el test de contrato de ids
    paralelos, no el tiempo de ejecución.
  - La traducción ya no puede adaptarse a un par concreto (p. ej. una formulación distinta de una frase `es-ES`
    según se enseñe `pt-PT` o `ru-RU`); se acepta por ser frases paralelas por diseño.
  - Primer arranque tras la actualización: el catálogo se resiembra completo.
- **Descartado:**
  - Mantener documentos de traducción por par: N·(N−1) ficheros que duplican el `target` de otro documento.
  - Origen elegido por el usuario en Ajustes y guardado: el idioma del dispositivo ya expresa el idioma del
    usuario; un ajuste más añade superficie sin un caso de uso pedido.
  - Origen por idioma destino, o respaldo que excluye el destino (ADR-0020): haría depender el origen del destino.
    El origen representa el idioma del usuario y es más estable que el destino que practica.
  - `es-ES` como literal en el dominio: contradice ADR-0003; se inyecta desde `:app`.
