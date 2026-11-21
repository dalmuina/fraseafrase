# Proposal

## Why

El catálogo solo tiene 3 idiomas destino (`pt-PT`, `pt-BR`, `es-ES`) y cada uno admite un único
`sourceLocale` fijo, porque cada documento JSON mezcla el contenido del idioma destino (`target`,
`level`, `topic`) con el texto de un único origen (`source`). El usuario quiere ampliar los idiomas
disponibles (italiano, inglés británico, alemán, francés, ruso) y que, con el tiempo, cualquier
idioma pueda servir de origen para cualquier otro destino, sin perder el progreso de una frase al
cambiar el idioma de origen (p. ej. seguir aprendiendo `pt-PT` con racha y nivel intactos si el
usuario pasa de usar español a inglés como idioma de origen).

`PhraseProgressEntity` ya tiene clave `(phrase_id, target_locale)`, sin `source_locale` (ADR-0019):
el progreso ya es independiente del origen. `CatalogPhraseEntity` ya tiene clave
`(id, target_locale, source_locale)` (ADR-0019), pensada para admitir más de un origen por destino
sin migración. Lo que falta es separar el contenido del destino de su traducción en documentos
distintos: hoy `RoomCatalogDataSource.replaceSeed` borra todas las filas de un `target_locale` al
resembrar, así que un segundo documento con el mismo destino y otro origen borraría las frases del
primero, y además duplicaría `target`/`level`/`topic` por cada origen nuevo.

## What Changes

- **BREAKING** (contenido, no API pública): el formato del catálogo JSON se divide en dos tipos de
  documento:
  - Documento de frases destino `phrases-<target>.json`: `schemaVersion`, `targetLocale`,
    `phrases[{id, level, topic, target}]`. Ya no lleva `sourceLocale` ni `source`.
  - Documento de traducción `translations-<target>-from-<source>.json`: `schemaVersion`,
    `targetLocale`, `sourceLocale`, `translations[{id, source}]`. Un id sin traducción para un
    origen no aparece en ese documento.
  - Los 3 documentos existentes (`phrases-pt-PT.json`, `phrases-pt-BR.json`, `phrases-es-ES.json`)
    se migran a este formato: el `source` que tenían pasa a su propio documento de traducción
    (`translations-pt-PT-from-es-ES.json`, `translations-pt-BR-from-es-ES.json`,
    `translations-es-ES-from-pt-PT.json`), sin tocar ids, orden ni texto.
- `CatalogPhraseEntity` pierde `source`/`sourceLocale`, clave `(id, target_locale)`. Se añade
  `CatalogTranslationEntity`, clave `(id, target_locale, source_locale)`. `CatalogSeedVersionEntity`
  pasa a versionar por separado el documento de frases destino y cada documento de traducción de un
  idioma destino, para que resembrar una traducción no borre ni las frases destino ni las
  traducciones de otro origen. `CatalogDatabase` sube de versión con
  `fallbackToDestructiveMigration()` (ya vigente, ADR-0019): sin pérdida de datos de usuario, el
  catálogo es una caché sembrada desde assets.
- `CatalogRepository.phrases(targetLocale, sourceLocale)` combina internamente la fila de frase
  destino con la fila de traducción del origen pedido; su contrato público (firma, `CatalogPhrase`
  del dominio) no cambia.
- Se añaden 5 idiomas destino nuevos con `es-ES` como único origen (mismo patrón que `pt-BR` hoy):
  `it-IT` (italiano), `en-GB` (inglés británico), `de-DE` (alemán), `fr-FR` (francés) y `ru-RU`
  (ruso), cada uno con 1000 frases traducidas desde el contenido ya revisado de `pt-PT`/`es-ES`.
  Ningún idioma existente gana un origen nuevo en este cambio: la combinación de orígenes y destinos
  más allá de `es-ES` queda para cambios futuros, ya sin necesidad de tocar el modelo de datos.
- `DsLanguageBadge` (icono) y `toDsLanguageOption()` (nombre visible en Ajustes) ganan una rama por
  idioma nuevo, siguiendo el mismo patrón que `es-ES` hoy.
- Nuevo ADR-0022 que sustituye la parte de formato del catálogo de ADR-0019 (separación frase
  destino / traducción; ADR-0013 ya está completamente sustituido por ADR-0019 y no se vuelve a
  tocar) y confirma que ADR-0019/ADR-0020 (orígenes múltiples por destino, respaldo de origen) no
  cambian de decisión, solo de representación en disco y en Room.

## Fuera de alcance

- Traducir el contenido existente a más combinaciones origen→destino además de
  `<idioma nuevo>→es-ES`. En particular, no se añade `it-IT`/`en-GB`/`de-DE`/`fr-FR`/`ru-RU` como
  origen de `pt-PT`, `pt-BR` o `es-ES`, ni entre sí. El rediseño del catálogo deja esto listo para
  añadirse frase a frase en cambios futuros (un documento de traducción nuevo, sin migración).
- Inglés americano (`en-US`): el usuario ha elegido solo `en-GB` en este cambio.
- Selección de variante de idioma en Ajustes más allá de elegir un idioma destino entre los
  disponibles (ADR-0003 ya la difiere; no se toca aquí).
- Cambiar `ResolveSourceLocale`, `ObtainCatalogPhrasesForActiveLanguage` o cualquier independencia
  por idioma destino de ADR-0019/ADR-0020: su lógica ya es correcta para N orígenes por destino, solo
  faltaba que el almacenamiento lo permitiera sin duplicar contenido.
- Auditoría lingüística profesional de las traducciones nuevas: se revisan por muestreo como ya se
  hizo con `pt-BR`/`es-ES`, no por un hablante nativo certificado.

## Capabilities

### Modified Capabilities

- `phrase-catalog`: el catálogo pasa de "un documento por idioma destino, con un único origen
  embebido" a "un documento de frases destino por idioma destino, más un documento de traducción
  independiente por cada combinación destino×origen". Añade requisitos sobre el documento de
  traducción (formato, validación, versionado independiente del sembrado) y amplía las reglas de
  permanencia de ids e idiomas destino disponibles a 8 idiomas destino.

## Impact

- `:core:data`: `CatalogJson` (parseo de dos tipos de documento), `CatalogPhraseEntity`,
  `CatalogTranslationEntity` (nueva), `CatalogSeedVersionEntity`, `CatalogDao`, `CatalogMapping`,
  `RoomCatalogDataSource`, `CatalogDatabase` (sube de versión). Assets:
  `core/data/src/main/assets/catalog/` (migración de 3 documentos existentes + 10 documentos
  nuevos: 5 de frases destino, 5 de traducción).
- `:core:data` tests: `CatalogJsonTest`, `RoomCatalogDataSourceTest`, `RealCatalogContractTest` y
  `released-ids-<target>.txt` nuevos para los 5 idiomas destino nuevos.
- `:core:designsystem`: `DsLanguageBadge`/`languageBadgeSpec` (5 ramas e iconos de bandera nuevos).
- `:feature:settings`: `SettingsScreen.toDsLanguageOption()` (5 nombres visibles nuevos).
- `docs/adr/0022-...md` (nuevo), línea de estado de ADR-0019 actualizada a "Sustituido por
  ADR-0022 (solo formato de documento)"; ADR-0013 no se toca (ya está sustituido por ADR-0019).
- `openspec/specs/phrase-catalog/spec.md` (delta de requisitos).
