# Tasks

## 1. ADR

- [x] 1.1 Redactar `docs/adr/0022-separar-frases-destino-de-sus-traducciones.md` con cabecera
  `Sustituye a: ADR-0019 (solo la parte de formato de documento)` (contexto, decisión y
  consecuencias del design.md), y actualizar solo la línea de estado de ADR-0019 a
  `Sustituido por ADR-0022 (solo formato de documento)` (ADR-0001). ADR-0013 no se toca: ya está
  `Sustituido por 0019` por completo. Verificación: revisión manual de que ningún otro contenido de
  ADR-0013/ADR-0019 se edita.

## 2. `:core:data` - parseo de los dos tipos de documento

- [x] 2.1 Test: `CatalogJsonTest` - un documento de frases destino sin `sourceLocale` ni `source` se
  parsea y cada frase hereda `targetLocale` (spec `phrase-catalog`, escenario "Frase destino hereda
  el locale de la raíz"). Debe fallar porque `CatalogDocument` todavía exige `sourceLocale`.
- [x] 2.2 Implementación: `PhraseCatalogDocument`/`PhraseDocument` sin `sourceLocale`/`source`,
  `CatalogJson.parsePhrases()`, tipo nuevo `CatalogParseResult.PhrasesParsed` propio de
  `:core:data` (design.md - "Dos tipos de documento JSON"; **sin relación de herencia con**
  `CatalogLoadResult` del dominio, que es `sealed interface` en `:core:domain` y no admite
  subtipos declarados en otro módulo). Verificación: 2.1 en verde.
- [x] 2.3 Test: `CatalogJsonTest` - un documento de traducción válido se parsea y cada traducción
  hereda `targetLocale`/`sourceLocale` (spec, escenario "Traducción hereda los locales de la raíz");
  casos de error: `schemaVersion` ausente/no soportado, locale vacío/ausente/no BCP-47, `id`/`source`
  vacío, ids duplicados, documento no JSON (spec, escenarios correspondientes de documento de
  traducción). Deben fallar: no existe `parseTranslations`.
- [x] 2.4 Implementación: `TranslationCatalogDocument`/`TranslationDocument`,
  `CatalogJson.parseTranslations()`, `CatalogParseResult.TranslationsParsed`. Verificación: 2.3 en
  verde.
- [x] 2.5 Test: `CatalogJsonTest` - `headerOf()` distingue un documento de traducción de uno de
  frases destino por la presencia de `sourceLocale` en la cabecera. Debe fallar: `headerOf()` no
  expone el tipo todavía.
- [x] 2.6 Implementación: `CatalogHeader` gana el tipo de documento (o un campo derivado) y
  `headerOf()` lo calcula de `sourceLocale`. Verificación: 2.5 en verde.

## 3. `:core:data` - esquema de Room y sembrado independiente por documento

**Nota de secuenciación (añadida durante `/opsx:apply`):** 3.1-3.11 se implementan en una sola
invocación/sesión de TDD, sin commit intermedio. Cambiar las columnas de
`CatalogPhraseEntity`/`CatalogSeedVersionEntity` (3.2) rompe la compilación de
`RoomCatalogDataSource.kt` y `CatalogMapping.kt`, que leen/escriben esas columnas directamente; esos
dos ficheros solo vuelven a compilar con los cambios de las tareas 3.6-3.11 (bifurcación de
`seedDocument`, mapeos nuevos). `:core:data:compileDebugKotlin` solo puede estar en verde al final
de 3.11, no tras 3.2 ni 3.4 en solitario. El orden rojo/verde interno de cada tarea (3.1→3.2,
3.3→3.4, 3.5→3.6, 3.7→3.8, 3.9→3.10) se mantiene igual; esto era originalmente dos grupos (3 y 4)
que se fusionaron por esta razón de compilación compartida.

- [x] 3.1 Test: `CatalogPhraseEntityTest`/`CatalogTranslationEntityTest` o test de esquema - PK de
  `catalog_phrase` es `(id, target_locale)` sin `source_locale`; `catalog_translation` existe con PK
  `(id, target_locale, source_locale)`. Debe fallar: las entidades actuales no compilan con esa
  forma.
- [x] 3.2 Implementación: `CatalogPhraseEntity` sin `sourceLocale`/`source`; `CatalogTranslationEntity`
  nueva; `CatalogSeedVersionEntity` con PK `(target_locale, source_locale)` y `source_locale = ""`
  para frases destino (design.md - "Esquema de Room"); `CatalogDatabase` sube de versión.
  Verificación: 3.1 en verde (el módulo completo no compila todavía: ver nota de secuenciación).
- [x] 3.3 Test: `CatalogDaoTest` - `phrasesFor(targetLocale, sourceLocale)` hace el JOIN entre
  `catalog_phrase` y `catalog_translation` y devuelve el `source` correcto; `deletePhrasesForTargetLocale`
  y un nuevo `deleteTranslationsFor(targetLocale, sourceLocale)` borran solo su propia fila clave;
  `sourceLocalesFor(targetLocale)` lee de `catalog_translation`, no de `catalog_phrase` (design.md -
  "Esquema de Room"). Debe fallar: el DAO actual no tiene `catalog_translation` ni ese borrado, y
  `sourceLocalesFor` lee la columna que se retira de `catalog_phrase`.
- [x] 3.4 Implementación: `CatalogDao` con el JOIN, `insertTranslations`, `deleteTranslationsFor`,
  `seedVersionFor(targetLocale, sourceLocale)`, `sourceLocalesFor` sobre `catalog_translation`.
  Verificación: 3.3 en verde (el módulo completo sigue sin compilar: ver nota de secuenciación).

- [x] 3.5 Test: `RoomCatalogDataSourceTest` - sembrar un documento de traducción nuevo para un
  destino ya sembrado no borra sus frases destino ni las traducciones de otro origen ya sembrado
  (spec, escenario "Un cambio de `schemaVersion` en un documento de traducción vuelve a sembrarlo
  sin tocar las frases destino ni otros orígenes"). Debe fallar: `replaceSeed` actual borra por
  `target_locale` sin distinguir tipo de documento.
- [x] 3.6 Implementación: `RoomCatalogDataSource.seedDocument` se bifurca por el tipo de resultado
  del parseo (frases destino vs. traducción) y cada rama borra/inserta solo su propia clave
  (design.md - "Esquema de Room"). Verificación: 3.5 en verde.
- [x] 3.7 Test: `RoomCatalogDataSourceTest` - un documento de traducción con un id sin frase destino
  correspondiente para ese `targetLocale` no siembra nada de él (spec, escenario "Traducción sin
  frase destino correspondiente rechaza la carga"). Debe fallar: no hay esa validación todavía.
- [x] 3.8 Implementación: validación de ids huérfanos antes de `replaceSeed` de un documento de
  traducción. Verificación: 3.7 en verde.
- [x] 3.9 Test: `RoomCatalogDataSourceTest` - en una instalación nueva, sembrar un documento de
  traducción y el de frases destino de su mismo idioma destino, entregados en orden inverso (el de
  traducción antes que el de frases destino), siembra ambos igual que en el orden habitual, sin que
  la validación de ids huérfanos rechace la traducción por llegar antes (design.md - "Un id de
  traducción sin frase destino correspondiente... por eso `ensureSeeded()` siembra en dos pasadas").
  Debe fallar: `ensureSeeded()` siembra en el orden en que recibe los documentos, sin separar por
  tipo.
- [x] 3.10 Implementación: `RoomCatalogDataSource.ensureSeeded()` siembra en dos pasadas: primero
  todos los documentos de frases destino de `catalogDocuments()`, después todos los de traducción,
  sin depender del orden de `AssetManager.list()`. Verificación: 3.9 en verde.
- [x] 3.11 Implementación: `CatalogMapping.toDomain()`/`toEntity()` adaptados a `CatalogPhraseEntity`
  sin `source`, a la fila de proyección del JOIN (`CatalogPhraseWithSource`) y a
  `CatalogTranslationEntity`; el `CatalogPhrase` del dominio no cambia. Verificación:
  `:core:data:compileDebugKotlin` y `CatalogMappingTest` en verde con las firmas nuevas (este es el
  punto en el que el módulo completo vuelve a compilar: ver nota de secuenciación al inicio de este
  grupo).

## 5. Migración de contenido existente

- [ ] 5.1 Reescribir `phrases-pt-PT.json`, `phrases-pt-BR.json`, `phrases-es-ES.json` sin
  `sourceLocale` ni `source`, mismos `id`/`level`/`topic`/`target` y mismo orden. Verificación:
  `RealCatalogContractTest` (3.1-3.3 de la suite existente) sigue en verde para los tres, sin tocar
  `released-ids-pt-PT.txt`/`pt-BR.txt`/`es-ES.txt`.
- [ ] 5.2 Crear `translations-pt-PT-from-es-ES.json`, `translations-pt-BR-from-es-ES.json`,
  `translations-es-ES-from-pt-PT.json` con el `source` retirado en 5.1, mismos `id`. Verificación:
  `RoomCatalogDataSourceTest` (integración) carga las 1000 frases de cada idioma con su traducción
  igual que antes de la migración.

## 6. Contenido nuevo: 5 idiomas destino

- [ ] 6.1 Crear `phrases-it-IT.json`, `phrases-en-GB.json`, `phrases-de-DE.json`,
  `phrases-fr-FR.json`, `phrases-ru-RU.json`: 1000 frases cada uno, traducidas desde
  `phrases-pt-PT.json` (mismo `id`/`level`/`topic`, `target` traducido a su idioma), revisadas por
  muestreo (mismo criterio que `pt-BR`/`es-ES`, design.md - Risks). Verificación:
  `RealCatalogContractTest` nuevo (tarea 7.1) carga cada documento sin error de esquema.
- [ ] 6.2 Crear `translations-it-IT-from-es-ES.json`, `translations-en-GB-from-es-ES.json`,
  `translations-de-DE-from-es-ES.json`, `translations-fr-FR-from-es-ES.json`,
  `translations-ru-RU-from-es-ES.json`: `source` en español para cada `id` de 6.1 (el mismo `source`
  que `translations-pt-PT-from-es-ES.json`, ya que comparten `id`). Verificación: carga sin ids
  huérfanos (spec, escenario correspondiente).
- [ ] 6.3 Crear `released-ids-it-IT.txt`, `released-ids-en-GB.txt`, `released-ids-de-DE.txt`,
  `released-ids-fr-FR.txt`, `released-ids-ru-RU.txt` vacíos. Verificación: existen en
  `core/data/src/test/resources/catalog/`.

## 7. `:core:data` - contrato ampliado a 8 idiomas destino

- [ ] 7.1 Test: `RealCatalogContractTest` - `@ValueSource` con los 8 idiomas destino
  (`pt-PT, pt-BR, es-ES, it-IT, en-GB, de-DE, fr-FR, ru-RU`) y un test por idioma nuevo sin ids
  publicados (spec, escenario "Un idioma destino nuevo sin ids publicados no bloquea su primera
  carga"). Debe fallar hasta que 6.1-6.3 existan.
- [ ] 7.2 Verificación: `./gradlew :core:data:check` en verde con los 8 idiomas.

## 8. `:core:designsystem` - insignias de idioma

- [ ] 8.1 Test: `DsLanguageBadgeTest` - un escenario por idioma nuevo
  (`givenItItWhenPaintingThenItItIconIsShown`, `givenEnGbWhenPaintingThenEnGbIconIsShown`,
  `givenDeDeWhenPaintingThenDeDeIconIsShown`, `givenFrFrWhenPaintingThenFrFrIconIsShown`,
  `givenRuRuWhenPaintingThenRuRuIconIsShown`), mismo patrón que el de `es-ES`. Deben fallar:
  `languageBadgeSpec` no tiene esas ramas.
- [ ] 8.2 Implementación: drawables `ic_flag_it_it.xml`, `ic_flag_en_gb.xml`, `ic_flag_de_de.xml`,
  `ic_flag_fr_fr.xml`, `ic_flag_ru_ru.xml` (mismo estilo que los existentes) y 5 ramas nuevas en
  `languageBadgeSpec`; `AllLanguageCodes` del `@PreviewLightDark` ampliada. Verificación: 8.1 en
  verde.

## 9. `:feature:settings` - nombres visibles

- [ ] 9.1 Test: `SettingsScreenTest`/`SettingsViewModelTest` - con un fake de 8 idiomas disponibles,
  la lista de nombres visibles incluye los 5 nuevos (spec `settings-screen`, "Selección del idioma
  destino", sin cambiar su texto: solo se amplían los fakes del test existente). Debe fallar si el
  escenario ya enumeraba los idiomas de forma cerrada.
- [ ] 9.2 Implementación: 5 ramas nuevas en `LanguageTag.toDsLanguageOption()`
  (`"it-IT" -> "Italiano (Italia)"`, `"en-GB" -> "English (UK)"`, `"de-DE" -> "Deutsch (Deutschland)"`,
  `"fr-FR" -> "Français (France)"`, `"ru-RU" -> "Русский (Россия)"`). Verificación: 9.1 en verde.

## 10. Verificación final

- [ ] 10.1 `./gradlew check` en verde (tests, ktlint, detekt, lint) en todo el proyecto.
- [ ] 10.2 `./gradlew koverVerify` en verde (dominio 90 %, presentación 80 %, global 70 %).

## Workflow follow-up

- Ejecutar `/opsx:verify` contra la spec `phrase-catalog` antes de archivar.
- Archivar el cambio (`/opsx:archive`) en su rama, antes de la PR.
