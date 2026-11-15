# Tasks

## 1. Dominio: puertos y entidades de idioma

- [x] 1.1 Test: `ResolveSourceLocale` devuelve el locale del dispositivo cuando está entre los
  disponibles, y `es-ES` en caso contrario (escenarios de "Resolución del idioma de origen por
  defecto", `phrase-catalog`). Falla porque `ResolveSourceLocale` no existe.
- [x] 1.2 Implementar `ResolveSourceLocale` y el puerto `DeviceLocaleProvider` en `:core:domain`.
  Verificar `./gradlew :core:domain:test`.
- [x] 1.3 Test: `CatalogRepository.availableTargetLocales()`/`availableSourceLocales(targetLocale)`
  con fakes, cubriendo "Idiomas destino disponibles" (`phrase-catalog`). Falla porque la interfaz no
  tiene esos métodos.
- [x] 1.4 Ampliar `CatalogRepository` con `availableTargetLocales()`, `availableSourceLocales(...)` y
  `phrases(targetLocale, sourceLocale)`; actualizar fakes de `:core:testing`. Verificar
  `./gradlew :core:domain:test :core:testing:test`.
- [x] 1.5 Test: `ObtainCatalogPhrasesForActiveLanguage` compone `DeviceLocaleProvider` +
  `ResolveSourceLocale` + `CatalogRepository` y devuelve las frases del origen resuelto. Falla porque
  el caso de uso no existe.
- [x] 1.6 Implementar `ObtainCatalogPhrasesForActiveLanguage`; adaptar `ComposeNextBatch`/
  `GenerateSession`/`ObtainActiveBatch`/`RegenerateActiveBatchForLevel`/`ObtainPracticeSession`/
  `ObtainProgressOverview` para usarlo en vez de un locale fijo. Verificar
  `./gradlew :core:domain:test`.
- [x] 1.7 Test: `PracticeRepository.read(targetLocale)`/`transact(targetLocale, ...)` con el fake de
  `:core:testing`, cubriendo que dos idiomas destino no comparten estado. Falla porque la interfaz no
  tiene `targetLocale`.
- [x] 1.8 Añadir `targetLocale` a `PracticeRepository`; actualizar `AnswerPhrase`,
  `RevertLastAnswer`, `GenerateSession`, `ObtainActiveBatch`, `RegenerateActiveBatchForLevel`,
  `ObtainPracticeSession`, `ObtainProgressOverview` y el fake de `:core:testing`. Verificar
  `./gradlew :core:domain:test`.
- [x] 1.9 Test: `SettingsRepository` con `targetLocale` en `settings`/`save`, y
  `activeTargetLocale`/`setActiveTargetLocale`, cubriendo "Idioma destino activo" y "Cambio del
  idioma destino activo" (`user-settings`). Falla porque la interfaz no tiene esos métodos.
- [x] 1.10 Ampliar `SettingsRepository`; implementar `ObtainUserSettings`/`SaveUserSettings` con
  `targetLocale`, y los casos de uso nuevos `ObtainAvailableLanguages`, `ObtainActiveLanguage`,
  `SelectActiveLanguage` (valida que el idioma elegido esté disponible). Verificar
  `./gradlew :core:domain:test`.

## 2. Datos: catálogo en Room

- [x] 2.1 Test: `CatalogJson.parse` sigue validando un documento por idioma destino sin cambios de
  comportamiento (regresión sobre `CatalogJsonTest` existente, ahora con `catalog/phrases-pt-PT.json`
  como fixture). Falla porque el fichero todavía se llama `phrases.json`.
- [x] 2.2 Renombrar `core/data/src/main/assets/catalog/phrases.json` a
  `catalog/phrases-pt-PT.json`, actualizar referencias. Verificar `./gradlew :core:data:test`.
- [x] 2.3 Test: `CatalogDatabase`/`CatalogPhraseEntity`/`CatalogSeedVersionEntity` (JVM, SQLite
  embebido como `PracticeDatabase`) - insertar y consultar por `(targetLocale, sourceLocale)`. Falla
  porque la base de datos no existe.
- [x] 2.4 Implementar `CatalogDatabase`, `CatalogPhraseEntity`, `CatalogSeedVersionEntity`,
  `CatalogDao`, exportar schema. Verificar `./gradlew :core:data:test`.
- [x] 2.5 Test: `RoomCatalogDataSource` siembra `pt-PT` en el primer arranque, no resiembra si
  `schemaVersion` no cambió, y resiembra sin tocar `PracticeDatabase` si cambió - cubre "Persistencia
  del catálogo en Room" (`phrase-catalog`). Falla porque `RoomCatalogDataSource` no existe.
- [x] 2.6 Implementar `RoomCatalogDataSource : CatalogRepository` (siembra + consultas). Verificar
  `./gradlew :core:data:test`.
- [x] 2.7 Test: dos idiomas destino conviven de forma independiente y un error de validación en uno
  no afecta al otro, cubriendo "Dos idiomas destino conviven..." y "Un idioma destino con catálogo
  inválido no está disponible" (`phrase-catalog`). Falla antes de la implementación anterior o con un
  caso añadido ex profeso.
- [x] 2.8 Ajustar `RoomCatalogDataSource` para sembrar cada idioma destino de forma independiente
  (ningún fallo de uno bloquea a otro). Verificar `./gradlew :core:data:test`.
- [x] 2.9 Test de contrato: extender `RealCatalogContractTest`/`released-ids.txt` para ejecutarse por
  idioma destino, con `pt-BR` sin ids publicados todavía - cubre los escenarios de "Permanencia de los
  ids" por idioma destino (`phrase-catalog`). Falla porque el test actual solo cubre un idioma.
- [x] 2.10 Implementar la extensión del contrato por idioma destino (un `released-ids-<locale>.txt`
  por idioma). Verificar `./gradlew :core:data:test`.

## 3. Datos: progreso por idioma destino

- [x] 3.1 Test: migración destructiva de `PracticeDatabase` v3→v4 - tras migrar, las tablas quedan
  con las columnas `target_locale` nuevas y vacías (sin intentar preservar datos). Falla porque la
  versión sigue en 3.
- [x] 3.2 Subir `PracticeDatabase` a v4: añadir `target_locale` a la PK de `phrase_progress`,
  `active_batch` y `session_counter`; reescribir los triggers de unicidad de `batch_position` sobre
  `(target_locale, batch_position)`; mantener `fallbackToDestructiveMigration()`. Exportar schema v4.
  Verificar `./gradlew :core:data:test`.
- [x] 3.3 Test: `RoomPracticeDataSource.read(targetLocale)`/`transact(targetLocale, ...)` - cubre
  "Posición de la tanda activa", "Tanda activa única", "Orden de la tanda activa", "Persistencia del
  contador de sesiones generadas", "Primer uso" con dos idiomas destino en paralelo
  (`practice-persistence`). Falla porque la implementación actual no recibe `targetLocale`.
- [x] 3.4 Implementar `RoomPracticeDataSource` con `targetLocale` en cada operación. Verificar
  `./gradlew :core:data:test`.
- [x] 3.5 Test: "Composición atómica de la tanda siguiente" y "Lectura coherente del estado" por
  idioma destino, incluyendo que un fallo en un idioma no deja estado parcial en otro
  (`practice-persistence`). Falla si la transacción no está acotada al `targetLocale`.
- [x] 3.6 Ajustar la transacción de `RoomPracticeDataSource.transact` para acotarse al `targetLocale`
  recibido. Verificar `./gradlew :core:data:test`.

## 4. Datos: ajustes por idioma destino

- [x] 4.1 Test: `DataStoreSettingsDataSource.settings(targetLocale)`/`save(targetLocale, ...)` no
  mezclan ajustes de dos idiomas destino, y usan A1/20 por defecto por idioma - cubre los escenarios
  de `user-settings` (valores por defecto, persistencia, fallback) por idioma. Falla porque la
  implementación actual usa claves sin idioma.
- [x] 4.2 Implementar las claves `cefr_level_<targetLocale>`/`batch_size_<targetLocale>` en
  `DataStoreSettingsDataSource`. Verificar `./gradlew :core:data:test`.
- [x] 4.3 Test: `activeTargetLocale()`/`setActiveTargetLocale(...)` - primera ejecución usa el primer
  idioma disponible, valor guardado no disponible cae al primero disponible (escenarios de "Idioma
  destino activo", `user-settings`). Falla porque esos métodos no existen en la implementación.
- [x] 4.4 Implementar la clave `active_target_locale` en `DataStoreSettingsDataSource`. Verificar
  `./gradlew :core:data:test`.

## 5. Plataforma: locale del dispositivo

- [x] 5.1 Test: `AndroidDeviceLocaleProvider.current()` devuelve el locale del dispositivo como
  `LanguageTag`, inyectando el locale igual que `TextToSpeechPlayerTest` inyecta su seam. Falla
  porque la clase no existe.
- [x] 5.2 Implementar `AndroidDeviceLocaleProvider : DeviceLocaleProvider` en `:core:platform`.
  Verificar `./gradlew :core:platform:test`.

## 6. Design system: badge y selector de idioma

- [x] 6.1 Añadir los drawables vectoriales de bandera (Portugal, Brasil) a `:core:designsystem`, sin
  test de comportamiento (recurso estático).
- [x] 6.2 Crear `DsLanguageBadge(languageCode: String, modifier)` con `@PreviewLightDark` cubriendo
  `"pt-PT"` y `"pt-BR"`, siguiendo el estilo de `DsLevelBadge`. El componente MUST NOT depender de
  `LanguageTag` (`:core:domain`) ni de ningún otro tipo de dominio, según ADR-0010. Verificar
  visualmente con las previews y `./gradlew :core:designsystem:check`.
- [x] 6.3 Test instrumentado: `DsLanguageOptions` (desplegable), recibiendo `List<DsLanguageOption>`
  (tipo propio del design system, sin `LanguageTag`), lista los idiomas destino disponibles y marca
  el activo como seleccionado - cubre "Selección del idioma destino" (`settings-screen`). Falla
  porque el componente no existe.
- [x] 6.4 Implementar `DsLanguageOption(code: String, displayName: String)` y `DsLanguageOptions`
  sobre `ExposedDropdownMenuBox`/`DropdownMenu` de Material 3, con tokens de `AppTheme`. Verificar
  `./gradlew :core:designsystem:check` (incluye el test instrumentado filtrado por clase).

## 7. Presentación: Ajustes

- [x] 7.1 Test: `SettingsViewModel` muestra el idioma destino activo y los idiomas disponibles al
  abrir, y al elegir un idioma distinto actualiza el CEFR/N mostrados sin guardar - cubre "Ajustes
  vigentes al abrir la pantalla" y "Cambiar el idioma destino actualiza el CEFR y la N mostrados"
  (`settings-screen`). Falla porque `SettingsUiState`/`SettingsAction` no tienen idioma.
- [x] 7.2 Añadir `activeTargetLocale`/`availableTargetLocales` a `SettingsUiState.Loaded` y
  `SelectLanguage` a `SettingsAction`; cablear `SettingsViewModel` a `ObtainActiveLanguage`/
  `ObtainAvailableLanguages`. Verificar `./gradlew :feature:settings:test`.
- [x] 7.3 Test: guardar persiste el idioma elegido en el desplegable (y su CEFR/N) sin tocar los
  ajustes de otro idioma destino; salir sin guardar no cambia el idioma activo - cubre "Guardado de
  los ajustes editados", "Cambio de idioma destino descartado al salir sin guardar"
  (`settings-screen`). Falla porque `SettingsViewModel.save` no usa `SelectActiveLanguage`.
- [x] 7.4 Cablear el guardado de `SettingsViewModel` a `SelectActiveLanguage` + `SaveUserSettings`
  del idioma elegido. Verificar `./gradlew :feature:settings:test`.
- [x] 7.5 Test: cambiar de idioma sin cambiar CEFR/N no recompone la tanda del idioma anterior; sí
  recompone la del idioma activo si su CEFR/N cambió - cubre los escenarios de "Regeneración sin
  tocar el progreso al cambiar los ajustes" (`settings-screen`). Falla porque `RegenerateActiveBatchForLevel`
  no se invoca con el `targetLocale` correcto.
- [x] 7.6 Añadir `DsLanguageOptions` a `SettingsScreen`, mapeando `LanguageTag` ↔ `DsLanguageOption`
  en el propio `SettingsScreen`/`SettingsViewModel` (nunca pasando `LanguageTag` al componente), y
  verificar con test instrumentado (`SettingsScreenTest`) que aparece con el idioma activo
  preseleccionado. Verificar `./gradlew :feature:settings:check`.

## 8. Presentación: icono de idioma en la cabecera

- [x] 8.1 Test: `PracticeSessionViewModel`/`ProgressViewModel` leen `ObtainActiveLanguage` para
  mostrar el badge correcto - cubre "El icono muestra el idioma activo..." (`language-indicator`).
  Falla porque esos ViewModels no exponen el idioma activo.
- [x] 8.2 Exponer el idioma activo en `PracticeSessionUiState`/`ProgressUiState`. Verificar
  `./gradlew :feature:session:test :feature:progress:test`.
- [x] 8.3 Test instrumentado: `DsLanguageBadge` aparece en la cabecera de `PracticeSessionScreen`,
  `ProgressScreen` y `SettingsScreen`, y tocarlo navega a Ajustes - cubre "Icono del idioma activo en
  la cabecera" y "Tocar el icono navega a Ajustes" (`language-indicator`). Falla porque el badge no
  está en la cabecera todavía.
- [x] 8.4 Añadir `DsLanguageBadge` a la fila de cabecera de las tres pantallas (extendiendo el
  `Row`/`Arrangement.SpaceBetween` ya usado por `PracticeHeader`), pasándole el código de idioma como
  `String` (mapeado desde `LanguageTag` en cada `Screen`/ViewModel, nunca dentro del componente), con
  `onNavigateToSettings` cableado en `:app`. Verificar `./gradlew :feature:session:check
  :feature:progress:check :feature:settings:check`.

## 9. Contenido: catálogo pt-BR

- [x] 9.1 Generar `catalog/phrases-pt-BR.json` traduciendo las 1000 frases de
  `catalog/phrases-pt-PT.json` al portugués de Brasil (mismo `id`/`level`/`topic`, `targetLocale`
  `pt-BR`, `sourceLocale` `es-ES`). Verificar que `CatalogJson.parse` lo acepta
  (`./gradlew :core:data:test` con un test de fixture añadido para `pt-BR`).
- [x] 9.2 Revisar por muestreo una selección representativa de las traducciones (al menos una frase
  por nivel CEFR y tema) y documentar la revisión en este fichero o en un comentario del PR, según la
  regla de revisión manual del catálogo de ADR-0013 (heredada de ADR-0004). Verificar que la muestra
  revisada no requiere cambios o que los cambios se aplican antes de seguir.
- [x] 9.3 Crear `released-ids-pt-BR.txt` vacío (sin ids publicados todavía) y comprobar que el test de
  contrato de `pt-BR` pasa. Verificar `./gradlew :core:data:test`.

## 10. Integración en `:app`

- [x] 10.1 Test: el grafo de Hilt de producción resuelve `CatalogDatabase`,
  `DeviceLocaleProvider` y los casos de uso nuevos sin ciclos - cubre "Existe una única instancia de
  `CatalogRepository`" extendido a la nueva implementación. Falla porque `PersistenceModule` no los
  declara.
- [x] 10.2 Añadir a `PersistenceModule`/un módulo de plataforma: `@Provides` de `CatalogDatabase`,
  `@Binds` de `RoomCatalogDataSource` a `CatalogRepository`, `@Binds` de `AndroidDeviceLocaleProvider`
  a `DeviceLocaleProvider`. Cablear la navegación `onNavigateToSettings` del badge en
  `FraseAFraseApp`. Verificar `./gradlew :app:check`.

## 11. Verificación final

- [x] 11.1 Ejecutar `./gradlew check` completo (tests, ktlint, detekt) y corregir cualquier fallo.
- [x] 11.2 Ejecutar `./gradlew koverVerify` y ajustar cobertura si algún módulo queda por debajo de
  su mínimo (dominio 90 %, presentación 80 %, `:core:data` 80 % según ADR-0013, global 70 %).

## Workflow follow-up

- Revisar con `code-reviewer` antes de `/opsx:verify`.
- Ejecutar `/opsx:verify` contra las specs de este cambio.
- Archivar con `/opsx:archive` en la rama del cambio, antes de la PR.

## Revisión por muestreo de la tarea 9.2

Traducción generada por lotes de 50 frases (20 lotes, 1000 frases en total), traduciendo siempre
desde `source` (es-ES) y no desde `target` (pt-PT), para evitar un doble filtro de traducción. Tras
ensamblar `catalog/phrases-pt-BR.json` se validó automáticamente (script ad hoc) que: los 1000 ids
coinciden exactamente en orden con `phrases-pt-PT.json`; `id`/`level`/`topic`/`source` son idénticos
entre ambos ficheros; no hay ids duplicados; ningún campo (`target`, `source`, `topic`) está vacío; y
los 5 niveles CEFR (A1-C1) son válidos.

Sobre esa base, se revisó manualmente una muestra representativa: una frase por cada uno de los 114
`topic` distintos del catálogo (cubre de forma natural los 5 niveles CEFR, ya que cada tema pertenece
a un nivel fijo). Para cada id se comparó `source` (español) con el `target` (pt-BR) generado,
comprobando sentido, registro y uso de vocabulario/gramática propios de pt-BR (p. ej. "você" en vez de
"tu", "cachorro" en vez de "cão", "celular" en vez de "telemóvel", gerundio en vez de "a + infinitivo",
colocación pronominal antes del verbo) frente a pt-PT.

Ids revisados (uno por tema, en el orden en que aparecen por primera vez en el catálogo):
`a1-saludos-001`, `a1-comida-001`, `a1-familia-001`, `a1-numeros-001`, `a1-tiempo-001`,
`a1-casa-001`, `a1-colores-001`, `a1-animales-001`, `a2-viajes-001`, `a2-compras-001`,
`a2-trabajo-001`, `a2-salud-001`, `a2-rutina-001`, `a2-ocio-001`, `a2-transporte-001`,
`a2-tecnologia-001`, `b1-relaciones-001`, `b1-medioambiente-001`, `b1-cultura-001`,
`b1-deportes-001`, `b1-educacion-001`, `b2-politica-001`, `b2-economia-001`, `c1-filosofia-001`,
`c1-ciencia-001`, `c1-literatura-001`, `c1-sociedad-001`, `c1-medios-001`, `c1-derecho-001`,
`a1-ropa-001`, `a1-cuerpo-001`, `a1-escuela-001`, `a1-profesiones-001`, `a1-dias_semana-001`,
`a1-meses-001`, `a1-estaciones-001`, `a1-ciudad-001`, `a1-direcciones-001`,
`a1-deportes_basico-001`, `a1-musica-001`, `a1-mascotas-001`, `a1-naturaleza-001`,
`a1-emociones-001`, `a1-horario-001`, `a1-dinero-001`, `a1-telefono-001`, `a2-restaurante-001`,
`a2-hogar-001`, `a2-citas_medicas-001`, `a2-banco-001`, `a2-correos-001`, `a2-vecinos-001`,
`a2-fiestas-001`, `a2-cine-001`, `a2-supermercado-001`, `a2-hoteles-001`, `a2-aeropuerto-001`,
`a2-ropa_compras-001`, `a2-clima-001`, `a2-internet-001`, `a2-mascotas_cuidado-001`,
`a2-vida_social-001`, `a2-planes_futuro-001`, `b1-politica_local-001`, `b1-voluntariado-001`,
`b1-inmigracion-001`, `b1-medios_comunicacion-001`, `b1-nuevas_tecnologias-001`,
`b1-emprendimiento-001`, `b1-ocio_cultural-001`, `b1-alimentacion_saludable-001`,
`b1-psicologia-001`, `b1-urbanismo-001`, `b1-transporte_publico-001`, `b1-igualdad-001`,
`b1-familia_moderna-001`, `b1-redes_sociales-001`, `b1-mercado_laboral-001`,
`b1-turismo_sostenible-001`, `b1-bienestar-001`, `b2-sanidad_publica-001`, `b2-union_europea-001`,
`b2-cambio_climatico-001`, `b2-inteligencia_artificial-001`, `b2-mercado_inmobiliario-001`,
`b2-desigualdad_social-001`, `b2-globalizacion-001`, `b2-energias_renovables-001`,
`b2-politica_internacional-001`, `b2-crisis_economica-001`, `b2-demografia-001`,
`b2-innovacion-001`, `b2-etica_empresarial-001`, `b2-seguridad_informatica-001`,
`b2-migraciones-001`, `b2-justicia_social-001`, `b2-sostenibilidad-001`, `c1-epistemologia-001`,
`c1-geopolitica-001`, `c1-bioetica-001`, `c1-filosofia_moral-001`, `c1-teoria_economica-001`,
`c1-critica_literaria-001`, `c1-sociologia-001`, `c1-antropologia-001`, `c1-linguistica-001`,
`c1-historiografia-001`, `c1-neurociencia-001`, `c1-teoria_politica-001`, `c1-estetica-001`,
`c1-logica-001`, `c1-metafisica-001`, `c1-retorica-001`, `c1-hermeneutica-001`.

**Problema encontrado y corregido:** en `c1-medios-001` el `target` generado inicialmente era
"A desinformação se prolifera mais rápido do que a correção dos fatos.", con un "se" reflexivo
incorrecto (en pt-BR, "proliferar" no es pronominal). Se corrigió a "A desinformação prolifera mais
rápido do que a correção dos fatos." directamente en `catalog/phrases-pt-BR.json`, y se volvió a
ejecutar `./gradlew :core:data:test` para confirmar que el cambio no rompe la validación estructural.

En el resto de la muestra no se encontraron problemas: el sentido de cada frase en español se
mantiene en la traducción a pt-BR, el registro (coloquial en A1/A2, más formal y abstracto en B2/C1)
es coherente, y el vocabulario/gramática usados son reconociblemente de pt-BR y no una copia de
`phrases-pt-PT.json`.
