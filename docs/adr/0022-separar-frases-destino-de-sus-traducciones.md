# ADR-0022: Separar las frases destino de sus traducciones en documentos y tablas independientes

- **Estado:** Sustituido por ADR-0023 (documentos de traducción)
- **Fecha:** 2026-11-21
- **Sustituye a:** ADR-0019 (solo la parte de formato de documento)

## Contexto

ADR-0019 llevó el catálogo a Room sembrado desde un único documento JSON por idioma destino
(`phrases-<target>.json`), con `schemaVersion`, `targetLocale`, `sourceLocale` y
`phrases[{id, level, topic, target, source}]` en el mismo documento y la misma fila de
`CatalogPhraseEntity`. Ese formato mezcla el contenido propio del idioma destino (`target`, `level`,
`topic`) con el texto de un único origen (`source`), porque en ese momento cada idioma destino solo
necesitaba admitir un origen.

`CatalogPhraseEntity` ya tiene clave `(id, target_locale, source_locale)` y
`RoomCatalogDataSource.replaceSeed` borra por `target_locale` al resembrar: un segundo documento del
mismo idioma destino con otro `sourceLocale` borraría las filas del primero en vez de convivir con
ellas, porque el borrado no distingue origen. Añadir un origen nuevo a un destino existente exigiría
además repetir `target`/`level`/`topic` en cada documento, duplicando el contenido propio del destino
por cada origen que se quiera ofrecer. Este borrado por `target_locale` es el problema que impide
ampliar los orígenes de un idioma destino sin perder contenido ya sembrado.

El cambio `add-multilingual-sources` necesita que un idioma destino pueda tener, con el tiempo, más de
un origen (y que un mismo origen sirva a varios destinos) sin migrar Room cada vez ni duplicar el
contenido del destino. El formato de documento único de ADR-0019 no lo permite.

## Decisión

El documento de catálogo se divide en dos tipos, distinguidos por su cabecera (la presencia de
`sourceLocale`, sin un campo de tipo explícito):

- **Documento de frases destino** (`phrases-<target>.json`): `schemaVersion`, `targetLocale`,
  `phrases[{id, level, topic, target}]`. Ya no lleva `sourceLocale` ni `source`.
- **Documento de traducción** (`translations-<target>-from-<source>.json`): `schemaVersion`,
  `targetLocale`, `sourceLocale`, `translations[{id, source}]`.

En Room, cada tipo de documento se siembra y versiona de forma independiente:

- `CatalogPhraseEntity` (tabla `catalog_phrase`) pierde `source`/`sourceLocale`; clave
  `(id, target_locale)`.
- `CatalogTranslationEntity` (tabla `catalog_translation`, nueva): clave
  `(id, target_locale, source_locale)`, con la columna `source`.
- `CatalogSeedVersionEntity` (tabla `catalog_seed_version`) pasa a clave compuesta
  `(target_locale, source_locale)`, con una marca dedicada (`source_locale = ""`) para la fila de
  frases destino, de modo que una sola tabla siga versionando ambos tipos de documento.

`RoomCatalogDataSource` borra e inserta solo por la clave exacta de cada documento: resembrar un
documento de traducción nunca borra las frases destino ni las traducciones de otro origen ya
sembradas. La consulta de frases (`CatalogDao.phrasesFor`) hace el JOIN entre ambas tablas; el
contrato público de `CatalogRepository.phrases()` y el `CatalogPhrase` del dominio no cambian.
`CatalogDatabase` sube de versión y se recrea con `fallbackToDestructiveMigration()` (ya vigente desde
ADR-0019): sin pérdida de datos de usuario, porque el catálogo es una caché sembrada desde assets y el
progreso vive en `PracticeDatabase`.

Esta decisión **no cambia** la independencia por idioma destino de ADR-0019 (progreso, nivel CEFR, N,
tanda activa, contador de sesión siguen por `target_locale`) ni la regla de respaldo de origen de
ADR-0020 (`ResolveSourceLocale` sigue excluyendo el propio destino de sus candidatos de origen).
Sustituye únicamente la parte de ADR-0019 que describía el catálogo como "un documento por idioma
destino, con `sourceLocale` y `source` en el mismo documento".

## Consecuencias

- **Positivas:**
  - Añadir un origen nuevo a un idioma destino existente pasa a ser sembrar un documento de
    traducción nuevo, sin migración de Room y sin duplicar `target`/`level`/`topic` del destino.
  - Un origen puede servir a varios destinos y un destino puede tener varios orígenes sin que
    resembrar uno afecte a otro: cada borrado e inserción queda acotado a su propia clave
    `(target_locale, source_locale)`.
  - El contrato público de `CatalogRepository`, `ResolveSourceLocale` y `PhraseProgressEntity` no
    cambia: su diseño en ADR-0019/ADR-0020 ya era correcto para N orígenes por destino.
- **Negativas:**
  - La consulta de frases (`CatalogDao.phrasesFor`) pasa de una lectura directa a un JOIN entre
    `catalog_phrase` y `catalog_translation`.
  - `CatalogDatabase` sube de versión y se recrea vacía con `fallbackToDestructiveMigration()`: no
    pierde progreso de usuario (vive en `PracticeDatabase`), pero sí obliga a resembrar el catálogo
    completo desde assets en el primer arranque tras la actualización.
- **Descartado:**
  - Mantener un documento por combinación destino-origen con `target`/`level`/`topic` duplicados en
    cada uno: habría permitido más de un origen por destino sin tocar Room, pero duplicaría el
    contenido propio del destino en cada documento y arrastraría ese duplicado si una frase destino
    cambiara, sin ninguna ventaja sobre separar ambos tipos de documento.
