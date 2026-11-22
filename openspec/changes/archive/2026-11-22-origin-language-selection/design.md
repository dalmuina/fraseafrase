# Design: origin-language-selection

## Context

Motivación en `proposal.md`; decisión de arquitectura en ADR-0023. Estado actual relevante:

- `RoomCatalogDataSource` siembra dos tipos de documento (frases y traducciones) en dos pasadas;
  `CatalogDao.phrasesFor(target, source)` une `catalog_phrase` con `catalog_translation`.
- `ObtainCatalogPhrasesForActiveLanguage(target)` resuelve el origen con
  `ResolveSourceLocale(device, target, availableSourceLocales(target))` (con `es-ES` como literal de respaldo) y
  llama a `phrases(target, source)`. Lo usan `GenerateSession`, `ObtainPracticeSession`,
  `RegenerateActiveBatchForLevel` y `ObtainProgressOverview`.
- `SettingsRepository.activeTargetLocale()` (DataStore) devuelve el guardado o el primero de
  `availableTargetLocales()`. `ObtainActiveLanguage` lo delega sin más.
- `PracticeSessionViewModel.reload(onlyIfChanged = true)` (acción `Resume`, lanzada al montar la pantalla) solo
  compara los ids de la tanda. Al cambiar el idioma del móvil la Activity se recrea pero el ViewModel sobrevive, así
  que el texto de origen antiguo seguiría en pantalla.

## Goals / Non-Goals

**Goals:**
- Un único sitio de dominio que resuelve el origen (dispositivo o por defecto) y otro que ajusta el destino.
- Reducir la capa de catálogo a un solo tipo de documento y una consulta por self-JOIN.

**Non-Goals:**
- Persistir o elegir el origen: no se toca `SettingsRepository` ni DataStore.
- Cambiar las firmas de `GenerateSession`, `ObtainPracticeSession`, `RegenerateActiveBatchForLevel` u
  `ObtainProgressOverview`: siguen recibiendo solo el idioma destino.
- Renombrar `availableTargetLocales()` (pasa a significar "idiomas del catálogo"; se documenta en el KDoc).

## Decisions

### D1. Resolución del origen: dispositivo o por defecto

`ResolveSourceLocale` (`:core:domain`, `catalog/`) pasa a ser una función pura sin el destino:
`invoke(device: LanguageTag, default: LanguageTag, available: List<LanguageTag>): LanguageTag` → `device` si está en
`available`; si no, `default`. No hay más pasos: si `default` tampoco está disponible (catálogo roto), la consulta de
frases devuelve lista vacía, que la práctica ya trata como estado vacío.

Tipo nuevo `DefaultSourceLocale(val value: LanguageTag)` (`:core:domain`, `catalog/`), `data class` sin lógica.
`:app` lo provee en `di/PersistenceModule.kt` con `es-ES`, en el mismo grupo que crea `ObtainSourceLanguage`
(grupo 1 de `tasks.md`) para que el grafo de Hilt compile desde el primer commit. Así el dominio no contiene ningún literal de idioma (ADR-0003),
a diferencia del `preferredFallback` actual, que desaparece.

Caso de uso nuevo `ObtainSourceLanguage` (`:core:domain`, `catalog/`): `@Inject constructor(deviceLocaleProvider,
catalogRepository, defaultSourceLocale)`; aplica `ResolveSourceLocale` con `availableTargetLocales()`.

*Alternativas descartadas:* `@Named` sobre un `String` (sin tipo, y Dagger no maneja bien `value class`); mantener
el literal en `ResolveSourceLocale` (contradice ADR-0003 y CLAUDE.md).

### D2. Destino activo distinto del origen, en `ObtainActiveLanguage`

`ObtainActiveLanguage` pasa a depender de `SettingsRepository`, `ObtainSourceLanguage` y `CatalogRepository`:
`stored = settingsRepository.activeTargetLocale()` (el repositorio conserva su fallback actual al primero
disponible); si `stored != source`, lo devuelve; si no, el primer disponible distinto de `source`, o `stored` si no
existe ninguno (catálogo de un solo idioma: degenerado, sin error).

No se cambia el puerto ni `DataStoreSettingsDataSource`: la regla es de dominio y se compone encima. Las fixtures
de `:feature:session`, `:feature:progress` y `:feature:settings` que construyen `ObtainActiveLanguage(repo)` a mano
se adaptan al nuevo constructor.

*Alternativa descartada:* llevar la exclusión a `DataStoreSettingsDataSource`: exigiría conocer el locale del
dispositivo y la regla de origen en `:core:data`.

### D3. Casos de uso afectados

- `SelectActiveLanguage(target)`: pasa a devolver `SelectLanguageResult` (`sealed interface` en
  `:core:domain`, `settings/`): `Selected` tras guardar, o `RejectedSourceLocale` sin guardar nada si
  `target == obtainSourceLanguage()`. No lanza: el origen puede cambiar con Ajustes abiertos (el ViewModel sobrevive
  al cambio de idioma del sistema) y una lista ya pintada puede ofrecer el nuevo origen; una excepción en
  `viewModelScope` cerraría la app. El `require` de disponibilidad existente se mantiene.
- `ObtainCatalogPhrasesForActiveLanguage(target)`: depende de `ObtainSourceLanguage` en vez de
  `DeviceLocaleProvider`; llama a `catalogRepository.phrases(target, source)`.

*Alternativa descartada:* `require(target != source)` más una acción `Resume` en Ajustes que recargue al montar:
sigue dejando una ventana en la que la app se cierra.

### D4. Catálogo: un tipo de documento y self-JOIN

`:core:data`, paquete `catalog/`:
- Se borran `CatalogTranslationEntity`, `CatalogParseResult.TranslationsParsed`, `CatalogTranslation`,
  `TranslationParsing`/`parseTranslations`, `CatalogHeader.isTranslation`/`sourceLocale`, la siembra en dos pasadas
  y los métodos del DAO `insertTranslations`, `deleteTranslationsFor`, `sourceLocalesFor`, `phraseIdsFor`.
- `CatalogSeedVersionEntity` vuelve a clave `target_locale` (se quita `source_locale` y `NO_SOURCE_LOCALE`).
- `CatalogDao.phrasesFor(target, source)`:
  `SELECT p.*, s.target AS source, s.target_locale AS source_locale FROM catalog_phrase p JOIN catalog_phrase s ON
  s.id = p.id WHERE p.target_locale = :target AND s.target_locale = :source ORDER BY p.position`.
  Se conserva la proyección `CatalogPhraseWithSource` y el mapeo a `CatalogPhrase`.
- `CatalogDatabase` sube a versión 3 (se exporta `3.json`); `fallbackToDestructiveMigration()` ya está.
  `PracticeDatabase` no cambia.
- `CatalogRepository` pierde `availableSourceLocales`; `phrases(target, source)` no cambia de firma.
  `FakeCatalogRepository` (`:core:testing`) se adapta: si sus catálogos contienen `source`, `phrases(target,
  source)` devuelve las frases del destino con el `source` igual al `target` de la frase con el mismo id en el
  catálogo de origen (mismo comportamiento que el self-JOIN); si no lo contiene, devuelve las frases del destino con
  su `source` sin cambios (atajo para los tests existentes de un solo idioma, que no deben romperse).
  `FakeDeviceLocaleProvider` pasa a ser mutable (`var locale`) para simular el cambio de idioma del dispositivo.

*Alternativa descartada:* mantener `catalog_translation` sembrada desde `phrases-*.json`: duplicaría N·(N−1) filas
sin aportar nada sobre el self-JOIN.

### D5. Contrato de ids paralelos

`CatalogContract` (test, `:core:data`) gana una función pura `parallelIdMismatches(docs: Map<locale, List<id>>)`
que devuelve, por idioma, los ids que faltan y sobran respecto a la unión de todos. `RealCatalogContractTest` la
aplica a los `phrases-*.json` empaquetados. Los contratos de `released-ids-*.txt` por idioma no cambian.

### D6. Ajustes: opciones de destino sin el origen

`:feature:settings`: `SettingsUiState.Loaded.availableTargetLocales` pasa a contener los idiomas disponibles menos
el origen; el ViewModel lo calcula en `reload()` con `ObtainSourceLanguage`. Si `SelectActiveLanguage` devuelve
`RejectedSourceLocale`, el ViewModel llama a `reload()` (opciones recalculadas, destino activo sin cambios).
`SettingsScreen` no cambia de estructura (sigue pintando `availableTargetLocales`). No hay selector de origen.

### D7. Práctica: `Resume` detecta el cambio de texto de origen

`:feature:session`: `loadedBatchIds` pasa a guardar `id` y `source` de cada `PracticeItem`
(`loadedBatch: List<LoadedItem>`, con `private data class LoadedItem(val id: PhraseId, val source: String)`), y
`hasSameBatchAs` compara ambos. Si cambia el origen, el estado se
sustituye (frase sin revelar, deshacer limpiado, como cualquier cambio externo); si no, nada cambia.

*Alternativa descartada:* exponer el idioma de origen en `PracticeSessionResult` y compararlo: añade un campo de
dominio solo para esta comparación, cuando el texto ya refleja el cambio.

## Estrategia de test

| Componente | Tipo | Fakes |
|---|---|---|
| `ResolveSourceLocale`, `ObtainSourceLanguage`, `ObtainActiveLanguage`, `SelectActiveLanguage`, `ObtainCatalogPhrasesForActiveLanguage` | Unitario JUnit 6 | `FakeSettingsRepository`, `FakeCatalogRepository`, `FakeDeviceLocaleProvider` |
| `CatalogDao.phrasesFor`, `RoomCatalogDataSource`, `CatalogDatabase` v3 | Integración con Room en memoria (tests existentes de `:core:data`) | documentos JSON en línea |
| Contrato de ids paralelos | Unitario (función pura) + test sobre assets reales | — |
| `SettingsViewModel` | Unitario con `MainDispatcherExtension` y Turbine | fakes de `:core:testing` |
| `SettingsScreen` (opciones de destino) | Compose UI test instrumentado (`SettingsScreenTest`) | — |
| `PracticeSessionViewModel` `Resume` con cambio de origen | Unitario | `FakeDeviceLocaleProvider` mutable + `FakeCatalogRepository` con tres idiomas (`sessionViewModelWith` recibe el proveedor) |
| Provisión de `DefaultSourceLocale` | Instrumentado (`PersistenceWiringTest`) | — |

## Risks / Trade-offs

- [Un documento de frases sin algún id rompería la consulta de ese par (frase de la tanda sin texto → error de
  datos)] → contrato D5 en `check`; el grupo de contenido lo ejecuta antes de commitear.
- [Fixtures de tres features construyen `ObtainActiveLanguage` a mano] → se adaptan en el mismo grupo que cambia
  el constructor (nota en `tasks.md`) para que la build no quede rota entre commits.
- [Tests de `:core:data` que siembran traducciones] → se reescriben o eliminan en el grupo de datos.
- [Usuarios con destino `es-ES` guardado y móvil en español] → el destino pasa al primero distinto del origen;
  aceptado (pre-1.0, ADR-0023).
- [Móvil en `pt-PT`: el origen es `pt-PT` y el destino no puede ser `pt-PT`] → comportamiento esperado de la regla.

## Migration Plan

`CatalogDatabase` v2 → v3 destructiva: el primer arranque resiembra el catálogo desde los `phrases-*.json`. No hay
migración de `PracticeDatabase` ni de DataStore. Rollback: revertir la rama.
