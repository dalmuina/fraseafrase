# Tasks

Convención de este cambio: `pt-BR` → `en-GB` en los fixtures de tests donde era el "segundo idioma destino"
(design.md, decisión 2). Excepción: los casos de idioma guardado que ya no está disponible conservan `pt-BR`
(decisión 3). Al sustituir el literal, renombrar también los identificadores (`ptBr` → `enGb`). Los grupos 1, 3, 5 y 6 son ajustes mecánicos de fixtures con los tests existentes en verde
(sin comportamiento nuevo; candidatos a `model: haiku`).

## 1. Dominio: fixtures de tests (`:core:domain`)

- [x] 1.1 Sustituir `pt-BR` por `en-GB` como segundo idioma destino en `CatalogRepositoryTest` y
  `PracticeRepositoryTest` (specs `user-settings`/`practice-persistence`: aislamiento entre idiomas destino).
- [x] 1.2 Pasar a `en-GB` los fixtures `pt-BR` de `SelectActiveLanguageTest` y `SettingsRepositoryTest`. (El caso de
  idioma retirado vive en `:core:data`, tarea 2.5.)
- [x] 1.3 Ejecutar `./gradlew :core:domain:check` en verde.

## 2. Datos: catálogo sin `pt-BR` (`:core:data`)

Nota: el test rojo y el borrado del asset van en el mismo grupo porque el hook de commit exige `check` en verde.

- [ ] 2.1 Test: en `RealCatalogContractTest`, quitar `pt-BR` de `PACKAGED_LOCALES` y del `@ValueSource`, eliminar
  el test "given pt-BR with no released ids yet…" y añadir el test "given the packaged catalog when listing target
  locales then pt-PT appears once and pt-BR does not" (spec `phrase-catalog` → "El catálogo empaquetado solo
  tiene portugués europeo"). Ejecutar `./gradlew :core:data:test --tests "*RealCatalogContractTest"` y comprobar
  que el test nuevo falla porque `phrases-pt-BR.json` sigue empaquetado.
- [ ] 2.2 Contenido: borrar `core/data/src/main/assets/catalog/phrases-pt-BR.json` y
  `core/data/src/test/resources/catalog/released-ids-pt-BR.txt` (sin leer el asset). Verificar el contrato en verde.
- [ ] 2.3 Adaptar fixtures `pt-BR` → `en-GB` en `CatalogDaoTest` y `RoomCatalogDataSourceTest` (spec
  `phrase-catalog` → "Dos idiomas destino conviven de forma independiente").
- [ ] 2.4 Adaptar `PracticeSchemaTest`, `RoomPracticeCycleTest`, `RoomPracticeDataSourceReadTest` y
  `RoomPracticeDataSourceWriteTest` (specs `practice-persistence`: aislamiento de progreso, tanda y contador).
- [ ] 2.5 Adaptar `DataStoreSettingsDataSourceTest`, conservando `pt-BR` literal solo en el test "pt-BR is no longer available then returns the first available" (spec `user-settings` → "Un idioma destino activo guardado que ya no está disponible usa el primero disponible") (spec `user-settings` → "Los ajustes de dos idiomas destino no
  se mezclan").
- [ ] 2.6 Ejecutar `./gradlew :core:data:check` en verde.

## 3. Design system: sin icono `pt-BR` (`:core:designsystem`)

- [ ] 3.1 Quitar de `DsLanguageBadge` la rama `"pt-BR"` y su entrada en `AllLanguageCodes`, y borrar el drawable
  `ic_flag_pt_br.xml` (cambio visual, sin test rojo previo según CLAUDE.md).
- [ ] 3.2 Actualizar `DsLanguageBadgeTest` (escenario "El icono muestra el idioma activo en Progreso y Ajustes" con
  `en-GB`; el test "pt-PT y pt-BR no comparten representación" pasa a comparar `pt-PT` con `en-GB`) y
  `DsLanguageOptionsTest` (opción `en-GB`). Son `androidTest`: se compilan con `:core:designsystem:check`.
- [ ] 3.3 Ejecutar `./gradlew :core:designsystem:check` en verde.

## 4. Ajustes: sin etiqueta `pt-BR` (`:feature:settings`)

- [ ] 4.1 Test: en `LanguageTagDsLanguageOptionMappingTest`, sustituir el caso `pt-BR` por `en-GB` si falta y
  añadir que `pt-BR` ya no tiene etiqueta propia (cae al comportamiento de etiqueta desconocida existente;
  regresión de presentación asociada a spec `phrase-catalog` → "El catálogo empaquetado solo tiene portugués europeo").
  Ejecutar `./gradlew :feature:settings:test --tests "*LanguageTagDsLanguageOptionMappingTest"` y comprobar que falla.
- [ ] 4.2 Quitar `"pt-BR" -> "Português (Brasil)"` de `LanguageTagDsLanguageOptionMapping` y pasar las previews de
  `SettingsScreenPreviews` a `en-GB`. Verificar el test en verde.
- [ ] 4.3 Adaptar `SettingsViewModelTest` (specs `settings-screen`: selección, activación inmediata y guardado por
  idioma).
- [ ] 4.4 Adaptar los `androidTest` `SettingsScreenTest` y `SettingsRouteTest`.
- [ ] 4.5 Ejecutar `./gradlew :feature:settings:check` en verde.

## 5. Práctica: fixtures (`:feature:session`)

- [ ] 5.1 Adaptar `PracticeSessionViewModelTest` y el `androidTest` `PracticeSessionScreenTest` (spec
  `language-indicator`: icono del idioma activo).
- [ ] 5.2 Ejecutar `./gradlew :feature:session:check` en verde.

## 6. Progreso: fixtures (`:feature:progress`)

- [ ] 6.1 Adaptar `ProgressViewModelTest` y el `androidTest` `ProgressScreenTest` (spec `progress-overview` →
  "Volver a Progreso tras cambiar el idioma destino activo"; spec `language-indicator`).
- [ ] 6.2 Ejecutar `./gradlew :feature:progress:check` en verde.

## 7. Verificación final (la ejecuta la sesión principal)

- [ ] 7.1 `Grep` insensible a mayúsculas de `pt-?br|pt_br` fuera de `openspec/changes/archive`, `build/` y
  `.gradle/`: solo deben quedar los casos de idioma retirado de `DataStoreSettingsDataSourceTest` (tarea 2.5) y menciones en
  `docs/` y ADR. Revisar `docs/pendientes-mvp.md` y actualizarlo solo si lista `pt-BR` como idioma disponible.
- [ ] 7.2 `./gradlew check` (tests, ktlint, detekt, lint) y `./gradlew koverVerify`.
- [ ] 7.3 `openspec validate remove-pt-br --strict`.
