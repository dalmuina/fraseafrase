# Tasks

## 1. Dominio: puertos y entidades de idioma

- [ ] 1.1 Test: `ResolveSourceLocale` devuelve el locale del dispositivo cuando está entre los
  disponibles, y `es-ES` en caso contrario (escenarios de "Resolución del idioma de origen por
  defecto", `phrase-catalog`). Falla porque `ResolveSourceLocale` no existe.
- [ ] 1.2 Implementar `ResolveSourceLocale` y el puerto `DeviceLocaleProvider` en `:core:domain`.
  Verificar `./gradlew :core:domain:test`.
- [ ] 1.3 Test: `CatalogRepository.availableTargetLocales()`/`availableSourceLocales(targetLocale)`
  con fakes, cubriendo "Idiomas destino disponibles" (`phrase-catalog`). Falla porque la interfaz no
  tiene esos métodos.
- [ ] 1.4 Ampliar `CatalogRepository` con `availableTargetLocales()`, `availableSourceLocales(...)` y
  `phrases(targetLocale, sourceLocale)`; actualizar fakes de `:core:testing`. Verificar
  `./gradlew :core:domain:test :core:testing:test`.
- [ ] 1.5 Test: `ObtainCatalogPhrasesForActiveLanguage` compone `DeviceLocaleProvider` +
  `ResolveSourceLocale` + `CatalogRepository` y devuelve las frases del origen resuelto. Falla porque
  el caso de uso no existe.
- [ ] 1.6 Implementar `ObtainCatalogPhrasesForActiveLanguage`; adaptar `ComposeNextBatch`/
  `GenerateSession`/`ObtainActiveBatch`/`RegenerateActiveBatchForLevel`/`ObtainPracticeSession`/
  `ObtainProgressOverview` para usarlo en vez de un locale fijo. Verificar
  `./gradlew :core:domain:test`.
- [ ] 1.7 Test: `PracticeRepository.read(targetLocale)`/`transact(targetLocale, ...)` con el fake de
  `:core:testing`, cubriendo que dos idiomas destino no comparten estado. Falla porque la interfaz no
  tiene `targetLocale`.
- [ ] 1.8 Añadir `targetLocale` a `PracticeRepository`; actualizar `AnswerPhrase`,
  `RevertLastAnswer`, `GenerateSession`, `ObtainActiveBatch`, `RegenerateActiveBatchForLevel`,
  `ObtainPracticeSession`, `ObtainProgressOverview` y el fake de `:core:testing`. Verificar
  `./gradlew :core:domain:test`.
- [ ] 1.9 Test: `SettingsRepository` con `targetLocale` en `settings`/`save`, y
  `activeTargetLocale`/`setActiveTargetLocale`, cubriendo "Idioma destino activo" y "Cambio del
  idioma destino activo" (`user-settings`). Falla porque la interfaz no tiene esos métodos.
- [ ] 1.10 Ampliar `SettingsRepository`; implementar `ObtainUserSettings`/`SaveUserSettings` con
  `targetLocale`, y los casos de uso nuevos `ObtainAvailableLanguages`, `ObtainActiveLanguage`,
  `SelectActiveLanguage` (valida que el idioma elegido esté disponible). Verificar
  `./gradlew :core:domain:test`.

## 2. Datos: catálogo en Room

- [ ] 2.1 Test: `CatalogJson.parse` sigue validando un documento por idioma destino sin cambios de
  comportamiento (regresión sobre `CatalogJsonTest` existente, ahora con `catalog/phrases-pt-PT.json`
  como fixture). Falla porque el fichero todavía se llama `phrases.json`.
- [ ] 2.2 Renombrar `core/data/src/main/assets/catalog/phrases.json` a
  `catalog/phrases-pt-PT.json`, actualizar referencias. Verificar `./gradlew :core:data:test`.
- [ ] 2.3 Test: `CatalogDatabase`/`CatalogPhraseEntity`/`CatalogSeedVersionEntity` (JVM, SQLite
  embebido como `PracticeDatabase`) - insertar y consultar por `(targetLocale, sourceLocale)`. Falla
  porque la base de datos no existe.
- [ ] 2.4 Implementar `CatalogDatabase`, `CatalogPhraseEntity`, `CatalogSeedVersionEntity`,
  `CatalogDao`, exportar schema. Verificar `./gradlew :core:data:test`.
- [ ] 2.5 Test: `RoomCatalogDataSource` siembra `pt-PT` en el primer arranque, no resiembra si
  `schemaVersion` no cambió, y resiembra sin tocar `PracticeDatabase` si cambió - cubre "Persistencia
  del catálogo en Room" (`phrase-catalog`). Falla porque `RoomCatalogDataSource` no existe.
- [ ] 2.6 Implementar `RoomCatalogDataSource : CatalogRepository` (siembra + consultas). Verificar
  `./gradlew :core:data:test`.
- [ ] 2.7 Test: dos idiomas destino conviven de forma independiente y un error de validación en uno
  no afecta al otro, cubriendo "Dos idiomas destino conviven..." y "Un idioma destino con catálogo
  inválido no está disponible" (`phrase-catalog`). Falla antes de la implementación anterior o con un
  caso añadido ex profeso.
- [ ] 2.8 Ajustar `RoomCatalogDataSource` para sembrar cada idioma destino de forma independiente
  (ningún fallo de uno bloquea a otro). Verificar `./gradlew :core:data:test`.
- [ ] 2.9 Test de contrato: extender `RealCatalogContractTest`/`released-ids.txt` para ejecutarse por
  idioma destino, con `pt-BR` sin ids publicados todavía - cubre los escenarios de "Permanencia de los
  ids" por idioma destino (`phrase-catalog`). Falla porque el test actual solo cubre un idioma.
- [ ] 2.10 Implementar la extensión del contrato por idioma destino (un `released-ids-<locale>.txt`
  por idioma). Verificar `./gradlew :core:data:test`.

## 3. Datos: progreso por idioma destino

- [ ] 3.1 Test: migración destructiva de `PracticeDatabase` v3→v4 - tras migrar, las tablas quedan
  con las columnas `target_locale` nuevas y vacías (sin intentar preservar datos). Falla porque la
  versión sigue en 3.
- [ ] 3.2 Subir `PracticeDatabase` a v4: añadir `target_locale` a la PK de `phrase_progress`,
  `active_batch` y `session_counter`; reescribir los triggers de unicidad de `batch_position` sobre
  `(target_locale, batch_position)`; mantener `fallbackToDestructiveMigration()`. Exportar schema v4.
  Verificar `./gradlew :core:data:test`.
- [ ] 3.3 Test: `RoomPracticeDataSource.read(targetLocale)`/`transact(targetLocale, ...)` - cubre
  "Posición de la tanda activa", "Tanda activa única", "Orden de la tanda activa", "Persistencia del
  contador de sesiones generadas", "Primer uso" con dos idiomas destino en paralelo
  (`practice-persistence`). Falla porque la implementación actual no recibe `targetLocale`.
- [ ] 3.4 Implementar `RoomPracticeDataSource` con `targetLocale` en cada operación. Verificar
  `./gradlew :core:data:test`.
- [ ] 3.5 Test: "Composición atómica de la tanda siguiente" y "Lectura coherente del estado" por
  idioma destino, incluyendo que un fallo en un idioma no deja estado parcial en otro
  (`practice-persistence`). Falla si la transacción no está acotada al `targetLocale`.
- [ ] 3.6 Ajustar la transacción de `RoomPracticeDataSource.transact` para acotarse al `targetLocale`
  recibido. Verificar `./gradlew :core:data:test`.

## 4. Datos: ajustes por idioma destino

- [ ] 4.1 Test: `DataStoreSettingsDataSource.settings(targetLocale)`/`save(targetLocale, ...)` no
  mezclan ajustes de dos idiomas destino, y usan A1/20 por defecto por idioma - cubre los escenarios
  de `user-settings` (valores por defecto, persistencia, fallback) por idioma. Falla porque la
  implementación actual usa claves sin idioma.
- [ ] 4.2 Implementar las claves `cefr_level_<targetLocale>`/`batch_size_<targetLocale>` en
  `DataStoreSettingsDataSource`. Verificar `./gradlew :core:data:test`.
- [ ] 4.3 Test: `activeTargetLocale()`/`setActiveTargetLocale(...)` - primera ejecución usa el primer
  idioma disponible, valor guardado no disponible cae al primero disponible (escenarios de "Idioma
  destino activo", `user-settings`). Falla porque esos métodos no existen en la implementación.
- [ ] 4.4 Implementar la clave `active_target_locale` en `DataStoreSettingsDataSource`. Verificar
  `./gradlew :core:data:test`.

## 5. Plataforma: locale del dispositivo

- [ ] 5.1 Test: `AndroidDeviceLocaleProvider.current()` devuelve el locale del dispositivo como
  `LanguageTag`, inyectando el locale igual que `TextToSpeechPlayerTest` inyecta su seam. Falla
  porque la clase no existe.
- [ ] 5.2 Implementar `AndroidDeviceLocaleProvider : DeviceLocaleProvider` en `:core:platform`.
  Verificar `./gradlew :core:platform:test`.

## 6. Design system: badge y selector de idioma

- [ ] 6.1 Añadir los drawables vectoriales de bandera (Portugal, Brasil) a `:core:designsystem`, sin
  test de comportamiento (recurso estático).
- [ ] 6.2 Crear `DsLanguageBadge(languageCode: String, modifier)` con `@PreviewLightDark` cubriendo
  `"pt-PT"` y `"pt-BR"`, siguiendo el estilo de `DsLevelBadge`. El componente MUST NOT depender de
  `LanguageTag` (`:core:domain`) ni de ningún otro tipo de dominio, según ADR-0010. Verificar
  visualmente con las previews y `./gradlew :core:designsystem:check`.
- [ ] 6.3 Test instrumentado: `DsLanguageOptions` (desplegable), recibiendo `List<DsLanguageOption>`
  (tipo propio del design system, sin `LanguageTag`), lista los idiomas destino disponibles y marca
  el activo como seleccionado - cubre "Selección del idioma destino" (`settings-screen`). Falla
  porque el componente no existe.
- [ ] 6.4 Implementar `DsLanguageOption(code: String, displayName: String)` y `DsLanguageOptions`
  sobre `ExposedDropdownMenuBox`/`DropdownMenu` de Material 3, con tokens de `AppTheme`. Verificar
  `./gradlew :core:designsystem:check` (incluye el test instrumentado filtrado por clase).

## 7. Presentación: Ajustes

- [ ] 7.1 Test: `SettingsViewModel` muestra el idioma destino activo y los idiomas disponibles al
  abrir, y al elegir un idioma distinto actualiza el CEFR/N mostrados sin guardar - cubre "Ajustes
  vigentes al abrir la pantalla" y "Cambiar el idioma destino actualiza el CEFR y la N mostrados"
  (`settings-screen`). Falla porque `SettingsUiState`/`SettingsAction` no tienen idioma.
- [ ] 7.2 Añadir `activeTargetLocale`/`availableTargetLocales` a `SettingsUiState.Loaded` y
  `SelectLanguage` a `SettingsAction`; cablear `SettingsViewModel` a `ObtainActiveLanguage`/
  `ObtainAvailableLanguages`. Verificar `./gradlew :feature:settings:test`.
- [ ] 7.3 Test: guardar persiste el idioma elegido en el desplegable (y su CEFR/N) sin tocar los
  ajustes de otro idioma destino; salir sin guardar no cambia el idioma activo - cubre "Guardado de
  los ajustes editados", "Cambio de idioma destino descartado al salir sin guardar"
  (`settings-screen`). Falla porque `SettingsViewModel.save` no usa `SelectActiveLanguage`.
- [ ] 7.4 Cablear el guardado de `SettingsViewModel` a `SelectActiveLanguage` + `SaveUserSettings`
  del idioma elegido. Verificar `./gradlew :feature:settings:test`.
- [ ] 7.5 Test: cambiar de idioma sin cambiar CEFR/N no recompone la tanda del idioma anterior; sí
  recompone la del idioma activo si su CEFR/N cambió - cubre los escenarios de "Regeneración sin
  tocar el progreso al cambiar los ajustes" (`settings-screen`). Falla porque `RegenerateActiveBatchForLevel`
  no se invoca con el `targetLocale` correcto.
- [ ] 7.6 Añadir `DsLanguageOptions` a `SettingsScreen`, mapeando `LanguageTag` ↔ `DsLanguageOption`
  en el propio `SettingsScreen`/`SettingsViewModel` (nunca pasando `LanguageTag` al componente), y
  verificar con test instrumentado (`SettingsScreenTest`) que aparece con el idioma activo
  preseleccionado. Verificar `./gradlew :feature:settings:check`.

## 8. Presentación: icono de idioma en la cabecera

- [ ] 8.1 Test: `PracticeSessionViewModel`/`ProgressViewModel` leen `ObtainActiveLanguage` para
  mostrar el badge correcto - cubre "El icono muestra el idioma activo..." (`language-indicator`).
  Falla porque esos ViewModels no exponen el idioma activo.
- [ ] 8.2 Exponer el idioma activo en `PracticeSessionUiState`/`ProgressUiState`. Verificar
  `./gradlew :feature:session:test :feature:progress:test`.
- [ ] 8.3 Test instrumentado: `DsLanguageBadge` aparece en la cabecera de `PracticeSessionScreen`,
  `ProgressScreen` y `SettingsScreen`, y tocarlo navega a Ajustes - cubre "Icono del idioma activo en
  la cabecera" y "Tocar el icono navega a Ajustes" (`language-indicator`). Falla porque el badge no
  está en la cabecera todavía.
- [ ] 8.4 Añadir `DsLanguageBadge` a la fila de cabecera de las tres pantallas (extendiendo el
  `Row`/`Arrangement.SpaceBetween` ya usado por `PracticeHeader`), pasándole el código de idioma como
  `String` (mapeado desde `LanguageTag` en cada `Screen`/ViewModel, nunca dentro del componente), con
  `onNavigateToSettings` cableado en `:app`. Verificar `./gradlew :feature:session:check
  :feature:progress:check :feature:settings:check`.

## 9. Contenido: catálogo pt-BR

- [ ] 9.1 Generar `catalog/phrases-pt-BR.json` traduciendo las 1000 frases de
  `catalog/phrases-pt-PT.json` al portugués de Brasil (mismo `id`/`level`/`topic`, `targetLocale`
  `pt-BR`, `sourceLocale` `es-ES`). Verificar que `CatalogJson.parse` lo acepta
  (`./gradlew :core:data:test` con un test de fixture añadido para `pt-BR`).
- [ ] 9.2 Revisar por muestreo una selección representativa de las traducciones (al menos una frase
  por nivel CEFR y tema) y documentar la revisión en este fichero o en un comentario del PR, según la
  regla de revisión manual del catálogo de ADR-0013 (heredada de ADR-0004). Verificar que la muestra
  revisada no requiere cambios o que los cambios se aplican antes de seguir.
- [ ] 9.3 Crear `released-ids-pt-BR.txt` vacío (sin ids publicados todavía) y comprobar que el test de
  contrato de `pt-BR` pasa. Verificar `./gradlew :core:data:test`.

## 10. Integración en `:app`

- [ ] 10.1 Test: el grafo de Hilt de producción resuelve `CatalogDatabase`,
  `DeviceLocaleProvider` y los casos de uso nuevos sin ciclos - cubre "Existe una única instancia de
  `CatalogRepository`" extendido a la nueva implementación. Falla porque `PersistenceModule` no los
  declara.
- [ ] 10.2 Añadir a `PersistenceModule`/un módulo de plataforma: `@Provides` de `CatalogDatabase`,
  `@Binds` de `RoomCatalogDataSource` a `CatalogRepository`, `@Binds` de `AndroidDeviceLocaleProvider`
  a `DeviceLocaleProvider`. Cablear la navegación `onNavigateToSettings` del badge en
  `FraseAFraseApp`. Verificar `./gradlew :app:check`.

## 11. Verificación final

- [ ] 11.1 Ejecutar `./gradlew check` completo (tests, ktlint, detekt) y corregir cualquier fallo.
- [ ] 11.2 Ejecutar `./gradlew koverVerify` y ajustar cobertura si algún módulo queda por debajo de
  su mínimo (dominio 90 %, presentación 80 %, `:core:data` 80 % según ADR-0013, global 70 %).

## Workflow follow-up

- Revisar con `code-reviewer` antes de `/opsx:verify`.
- Ejecutar `/opsx:verify` contra las specs de este cambio.
- Archivar con `/opsx:archive` en la rama del cambio, antes de la PR.
