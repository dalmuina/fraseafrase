# Tasks

Escenarios: `specs/<capacidad>/spec.md` de este cambio. Decisiones: `design.md` (D1–D7). ADR-0023 ya está
redactado en `docs/adr/0023-origen-del-dispositivo-y-texto-de-origen-desde-frases.md` (Propuesto).

## 1. Resolución de origen y destino (`:core:domain`, `:core:testing`, `:app`)

> Nota: `ObtainSourceLanguage` necesita `DefaultSourceLocale`, así que 1.4 añade también su `@Provides` en
> `app/.../di/PersistenceModule.kt` para que el grafo de Hilt compile desde este commit (design D1). El
> constructor nuevo de `ObtainActiveLanguage` obliga a adaptar en 1.6 todos los sitios de test que lo construyen a
> mano (design D2); solo cambia la construcción, no los tests:
> - `feature/session/src/test/.../PracticeSessionFixtures.kt`
> - `feature/progress/src/test/.../ProgressFixtures.kt`
> - `feature/settings/src/test/.../SettingsFixtures.kt`
> - `feature/session/src/androidTest/.../PracticeSessionRouteTest.kt`
> - `feature/settings/src/androidTest/.../SettingsRouteTest.kt`

- [x] 1.1 Test: `ResolveSourceLocaleTest` reescrito con la firma `(device, default, available)`: dispositivo
  disponible → dispositivo; dispositivo no disponible → por defecto (`phrase-catalog`: "El idioma del dispositivo
  está disponible", "El idioma del dispositivo no está disponible"). Debe fallar por la firma.
- [x] 1.2 Implementación: `ResolveSourceLocale` sin literal y `DefaultSourceLocale` (design D1). Verificación: 1.1
  en verde.
- [x] 1.3 Test: `ObtainSourceLanguageTest` - combina `FakeDeviceLocaleProvider` (pasa a mutable, `var locale`,
  design D4), `availableTargetLocales()` y `DefaultSourceLocale`; el resultado no cambia con el destino activo y
  sigue al dispositivo cuando este cambia (`phrase-catalog`: "El idioma de origen no depende del idioma destino
  activo", "Un cambio del idioma del dispositivo cambia el origen"). Debe fallar por clase inexistente.
- [x] 1.4 Implementación: `ObtainSourceLanguage` (design D1) y `@Provides DefaultSourceLocale(LanguageTag("es-ES"))`
  en `app/.../di/PersistenceModule.kt`. Verificación: 1.3 en verde, `:app:compileDebugKotlin`.
- [x] 1.5 Test: `ObtainActiveLanguageTest` - destino guardado distinto del origen se recupera; primera ejecución que
  salta el origen; guardado igual al origen usa el primero distinto (`user-settings`, requisito "Idioma destino
  activo": "Primera ejecución salta el idioma de origen", "Un idioma destino guardado igual al origen usa el primero
  distinto del origen"). Debe fallar porque hoy delega sin excluir.
- [x] 1.6 Implementación: `ObtainActiveLanguage` según design D2 y adaptación de los cinco sitios de la nota.
  Verificación: 1.5 en verde, `:core:domain:check`, `:core:testing:check`, `:app:check` y `check` de
  `:feature:session`, `:feature:progress`, `:feature:settings` (incluye la compilación de `androidTest`).

## 2. Selección de destino y frases con el origen resuelto (`:core:domain`, `:core:testing`)

> Nota: 2.4 cambia el constructor de `ObtainCatalogPhrasesForActiveLanguage` (de `deviceLocaleProvider` a
> `obtainSourceLanguage`) y obliga a adaptar su construcción en estos tests; solo cambia la construcción:
> - `core/domain/src/test/.../practice/GenerateSessionTest.kt`, `CloseThenObtainTest.kt`,
>   `ObtainActiveBatchTest.kt`, `ObtainPracticeSessionTest.kt`, `RegenerateActiveBatchForLevelTest.kt`
> - `core/domain/src/test/.../progress/ObtainProgressOverviewTest.kt`
> - `core/data/src/test/.../practice/RoomPracticeCycleTest.kt`
> - `feature/session/src/test/.../PracticeSessionFixtures.kt`, `feature/progress/src/test/.../ProgressFixtures.kt`
> - `feature/session/src/androidTest/.../PracticeSessionRouteTest.kt`
>
> `FakeCatalogRepository` (`:core:testing`) toma el `source` por id del catálogo de origen solo si lo contiene; si
> no, conserva el `source` de las frases del destino (design D4), para que los tests de un solo idioma no cambien.

- [x] 2.1 Test: `SelectActiveLanguageTest` - elegir el idioma de origen devuelve `RejectedSourceLocale` sin
  excepción y sin guardar nada; elegir otro disponible devuelve `Selected` y lo guarda (`user-settings`: "No se
  puede elegir como destino el idioma de origen"). Debe fallar porque hoy lo acepta.
- [x] 2.2 Implementación: `SelectLanguageResult` y `SelectActiveLanguage` con `ObtainSourceLanguage` (design D3).
  Verificación: 2.1 en verde.
- [x] 2.3 Test: `ObtainCatalogPhrasesForActiveLanguageTest` reescrito - con el dispositivo en `en-GB` y `en-GB`
  disponible, cada frase de `pt-PT` lleva como `source` el texto `en-GB` con su mismo id; con el dispositivo en
  `ja-JP`, el de `es-ES` (`phrase-catalog`: "El origen sale del texto destino del otro idioma"); con un fake de un
  solo idioma, el `source` se conserva. Debe fallar porque hoy consulta `availableSourceLocales`.
- [x] 2.4 Implementación: `ObtainCatalogPhrasesForActiveLanguage` con `ObtainSourceLanguage`,
  `FakeCatalogRepository` con `source` por id (design D3, D4) y adaptación de los sitios de la nota. Verificación:
  2.3 en verde, `:core:domain:check`, `:core:testing:check` y `test` de `:core:data`, `:feature:session`,
  `:feature:progress`.

## 3. Catálogo con un solo tipo de documento (`:core:data`)

> Nota: retirar `availableSourceLocales` del puerto `CatalogRepository` (`:core:domain`) en 3.2 obliga a quitar en
> el mismo paso su `override` en `RoomCatalogDataSource`, `FakeCatalogRepository`,
> `SwitchableCatalogRepository` (`feature/session/src/test/.../PracticeSessionFixtures.kt`) y los
> `CatalogRepository` anónimos de `PracticeSessionRouteTest.kt` y `SettingsRouteTest.kt` (androidTest), y borrar
> los tests de esa función (`core/domain/src/test/.../catalog/CatalogRepositoryTest.kt`,
> `RoomCatalogDataSourceTest.kt`). Los tests de traducción de `CatalogJsonTest`, `CatalogDaoTest`,
> `RoomCatalogDataSourceTest` y `CatalogDatabaseTest` se eliminan con el código que cubrían (requisitos retirados
> en el delta de `phrase-catalog`).

- [x] 3.1 Test: `CatalogDaoTest` - `phrasesFor(pt-PT, es-ES)` devuelve el `target` de `es-ES` como `source` por id con
  nivel y tema de `pt-PT`; un par sin fichero propio (`de-DE` desde `fr-FR`) devuelve todas las frases; un origen sin
  frases sembradas devuelve lista vacía (`phrase-catalog`: "El origen sale del texto destino del otro idioma", "Un
  par sin fichero propio está disponible", "Un idioma de origen con documento inválido no devuelve frases"). Debe
  fallar porque la consulta une con `catalog_translation`.
- [x] 3.2 Implementación: self-JOIN en `CatalogDao.phrasesFor`; `CatalogSeedVersionEntity` con clave
  `target_locale`; `CatalogDatabase` v3 con `3.json` exportado; quitar `availableSourceLocales` del puerto y de los
  sitios de la nota (design D4). La entidad de traducción y sus métodos de DAO se borran en 3.4, junto con la
  siembra que los usa. Verificación: 3.1 en verde.
- [x] 3.3 Test: `RoomCatalogDataSourceTest` - primera siembra de `pt-PT`, `it-IT` y `es-ES` deja cada idioma
  consultable con origen en cualquiera de los otros; resiembra por `schemaVersion` solo del idioma afectado; sin
  cambio no se resiembra; `CatalogDatabaseTest`: abrir una base v2 con la v3 la recrea y `PracticeDatabase` no
  cambia (`phrase-catalog`, requisito "Persistencia del catálogo de frases en Room", sus cuatro escenarios). Debe
  fallar por la siembra en dos pasadas.
- [x] 3.4 Implementación: siembra de una sola pasada en `RoomCatalogDataSource`; borrar `CatalogTranslationEntity`,
  los métodos de DAO `insertTranslations`, `deleteTranslationsFor`, `sourceLocalesFor` y `phraseIdsFor`,
  `TranslationParsing`, `parseTranslations`, `CatalogTranslation`, `TranslationsParsed` y
  `CatalogHeader.sourceLocale`/`isTranslation` de `CatalogJson`/`CatalogParseResult` (design D4). Verificación: 3.3
  en verde.
- [x] 3.5 Test: `CatalogContractTest` - `parallelIdMismatches` con ids iguales, con un id que falta y con un id de más
  (`phrase-catalog`, requisito "Ids paralelos entre idiomas", tres escenarios). Debe fallar por función inexistente.
- [x] 3.6 Implementación: `parallelIdMismatches` en `CatalogContract` y su aplicación a los `phrases-*.json` reales en
  `RealCatalogContractTest` (design D5). Verificación: 3.5 en verde, `:core:data:check` y compilación de
  `androidTest` de `:feature:session` y `:feature:settings`.

## 4. Contenido: retirar los documentos de traducción (sesión principal, sin `tdd-implementer`)

- [x] 4.1 Test de guarda en `RealCatalogContractTest`: ningún asset de `catalog/` tiene `sourceLocale` en la raíz
  (`phrase-catalog`: "El catálogo empaquetado no contiene documentos de traducción"). Falla mientras existan los
  `translations-*.json`.
- [x] 4.2 Borrar los 8 `core/data/src/main/assets/catalog/translations-*.json` con `git rm` (sin leerlos).
  Verificación: 4.1 y el contrato de ids paralelos en verde, `:core:data:check`.

## 5. Ajustes: destino sin el idioma de origen (`:feature:settings`)

- [x] 5.1 Test: `SettingsViewModelTest` - con `pt-PT`, `es-ES` y `en-GB` disponibles y el dispositivo en `es-ES`,
  `Loaded.availableTargetLocales` es `pt-PT` y `en-GB` (`settings-screen`: "El desplegable de destino excluye el
  idioma de origen"); con el dispositivo cambiado a `en-GB` después de cargar, elegir `en-GB` no lanza, el destino
  activo sigue en `pt-PT` y las opciones pasan a `pt-PT` y `es-ES` (`settings-screen`: "Elegir un idioma que ha
  pasado a ser el origen recarga las opciones sin cerrar la app"). Debe fallar porque hoy incluye `es-ES` y no
  recarga.
- [x] 5.2 Implementación: `reload()` filtra el origen con `ObtainSourceLanguage` y `RejectedSourceLocale` lanza
  `reload()` (design D6). Verificación: 5.1 en verde.
- [x] 5.3 Test: `SettingsScreenTest` (instrumentado, `givenX_whenY_thenZ`) - el desplegable de destino muestra solo
  las opciones del estado y no el idioma de origen (`settings-screen`: "El desplegable de destino excluye el idioma
  de origen"). Si pasa sin cambios de producción (la pantalla ya pinta la lista del estado), se deja como test de
  regresión. Verificación: `:feature:settings:check`.

## 6. Práctica: reflejar el cambio de origen (`:feature:session`)

- [x] 6.1 Test: `PracticeSessionViewModelTest` - `sessionViewModelWith` gana un parámetro `deviceLocaleProvider` y
  usa `FakeCatalogRepository.withCatalogs` con `pt-PT`, `es-ES` y `en-GB`; con la tanda cargada con el dispositivo
  en `es-ES`, pasar el `FakeDeviceLocaleProvider` a `en-GB` y lanzar `Resume` muestra el texto `en-GB` sin revelar,
  mismas frases y rachas (`practice-session`: "Volver a Practicar tras cambiar el idioma del dispositivo muestra el
  nuevo texto de origen"). Debe fallar porque `Resume` solo compara ids.
- [x] 6.2 Implementación: `loadedBatch` con `LoadedItem(id, source)`, `hasSameBatchAs` compara ambos (design D7).
  Verificación: 6.1 en verde y el test existente de `Resume` sin cambios que conserva `revealed` sigue en verde
  (`practice-session`: "Volver a Practicar sin cambios conserva el estado"), `:feature:session:check`.

## 7. Integración (`:app`)

- [ ] 7.1 Test de regresión: `PersistenceWiringTest` (instrumentado, filtrado por clase) - el grafo de producción
  resuelve `ObtainSourceLanguage` y `DefaultSourceLocale` vale `es-ES` (`phrase-catalog`: "El idioma del dispositivo
  no está disponible"). La provisión ya existe desde 1.4, así que pasa sin cambios de producción. Verificación:
  `:app:check`.

## 8. Cierre y verificación (sesión principal)

- [ ] 8.1 Líneas de estado (ADR-0001): ADR-0023 → `Aceptado`; ADR-0019 → `Sustituido por ADR-0022 (formato de
  documento) y ADR-0023 (regla de origen)`; ADR-0020 (Propuesto) → `Sustituido por ADR-0023`; ADR-0022 (Propuesto)
  → `Sustituido por ADR-0023 (documentos de traducción)`; añadir 0022 y 0023 (`Aceptado`) a la tabla de
  `docs/adr/README.md`.
- [ ] 8.2 `./gradlew check` y `./gradlew koverVerify` en verde (tests, detekt/ktlint, lint y cobertura: dominio 90 %,
  presentación 80 %, global 70 %).
- [ ] 8.3 Instalar la app de depuración y comprobar a mano: con el móvil en inglés las frases salen en inglés; con
  el móvil en japonés, en español; el desplegable de destino nunca ofrece el idioma de origen.

## Workflow follow-up

- `code-reviewer` (hasta dos rondas), `/opsx:verify`, `/opsx:archive` y commit de archive (abre la PR por hook).
