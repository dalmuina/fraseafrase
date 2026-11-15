# Design

## Context

Ver `proposal.md` - Why. ADR-0019 ya fija las decisiones de arquitectura de fondo (catálogo en Room
sembrado desde JSON por idioma destino, idioma de origen derivado del dispositivo con `es-ES` por
defecto, independencia por idioma destino de CEFR/N/progreso/tanda activa/contador de sesión,
`PracticeDatabase` v3→v4 con migración destructiva). Este documento fija cómo se implementa eso
dentro de la arquitectura existente (ADR-0010): dónde vive cada clase nueva, cómo se nombran los
data sources/repositories, y la estrategia de test.

Estado actual relevante:
- `AssetsCatalogDataSource` (`:core:data`) implementa `CatalogRepository` leyendo
  `catalog/phrases.json` una sola vez y memoizando el resultado.
- `RoomPracticeDataSource` implementa `PracticeRepository` sobre `PracticeDatabase` (v3): una única
  fila de `active_batch` (`id = 1`), una única fila de `session_counter`, y `phrase_progress` con
  `phrase_id` como clave primaria.
- `DataStoreSettingsDataSource` implementa `SettingsRepository` con las claves `cefr_level` y
  `batch_size`, un único valor global.
- No existe ningún puerto de locale del dispositivo ni ninguna base de datos de catálogo.
- No existe ningún componente `Ds*` de desplegable ni de icono/insignia circular genérico, ni ninguna
  cabecera compartida entre pantallas (cada pantalla pinta su propio título).

## Goals / Non-Goals

**Goals:**
- Más de un idioma destino con contenido propio, consultable por idioma.
- Progreso, tanda activa, contador de sesión, nivel CEFR y N completamente independientes por
  idioma destino.
- Idioma de origen derivado del dispositivo, con `es-ES` como valor por defecto, sin literales de
  idioma en el dominio.
- Selector de idioma en Ajustes e icono circular del idioma activo en las tres pantallas.

**Non-Goals:**
- Traducir el contenido a un idioma de origen distinto de `es-ES` (sin contenido disponible, fuera
  de alcance - ver proposal.md).
- Un segundo selector de idioma duplicado en la cabecera (navega a Ajustes en su lugar).
- Conservar el progreso existente al subir `PracticeDatabase` a v4.
- Cambiar las reglas de racha, certificación, composición por cuotas o intervalos de repaso
  (`practice-batch`, `spaced-repetition` no cambian: ya son agnósticas del idioma, solo pasan a
  invocarse una vez por idioma destino).

## Decisions

### Catálogo: nueva `CatalogDatabase` en `:core:data`, independiente de `PracticeDatabase`

Se crea una base de datos Room nueva y separada de `PracticeDatabase`, en vez de añadir las tablas de
catálogo a `PracticeDatabase`:

- `CatalogDatabase` (Room 3, `BundledSQLiteDriver`, fichero `catalog.db`, `exportSchema = true`,
  versión 1): tabla `catalog_phrase` (`CatalogPhraseEntity(id, targetLocale, sourceLocale, level,
  topic, target, source)`, PK `(id, targetLocale, sourceLocale)` — la PK incluye `sourceLocale` ya
  desde ahora para no necesitar una migración el día que un idioma destino tenga más de un origen) y
  tabla `catalog_seed_version` (`CatalogSeedVersionEntity(targetLocale: String` PK`, schemaVersion:
  Int)`), que registra qué `schemaVersion` de cada idioma destino ya se sembró.
- **Alternativa descartada**: añadir las tablas de catálogo a `PracticeDatabase`. Se descarta porque
  acoplaría el versionado de un contenido reseedable (el catálogo) al de un estado crítico del
  usuario (progreso), con el riesgo de que una migración de una afecte a la otra. Mantenerlas en
  bases de datos separadas dentro de `:core:data` respeta igual de bien ADR-0010 (ambas siguen siendo
  "Room, estado de la app") y aísla el radio de cualquier fallo de siembra del progreso del usuario.
- El sembrado es una única fuente: `RoomCatalogDataSource : CatalogRepository` (`:core:data`), que en
  su inicialización compara el `schemaVersion` de cada documento JSON empaquetado
  (`catalog/phrases-pt-PT.json`, `catalog/phrases-pt-BR.json`) contra `catalog_seed_version`; si no
  coincide, reemplaza únicamente las filas de `catalog_phrase` de ese `targetLocale` dentro de una
  transacción, reutilizando `CatalogJson.parse` para la validación. No hay una fuente separada para
  "leer de assets" en tiempo de consulta: las consultas normales (`phrases(targetLocale)`,
  `availableTargetLocales()`) leen siempre de Room.

### `CatalogRepository`: nuevos métodos, mismo puerto

```kotlin
interface CatalogRepository {
    suspend fun availableTargetLocales(): List<LanguageTag>
    suspend fun availableSourceLocales(targetLocale: LanguageTag): List<LanguageTag>
    suspend fun phrases(targetLocale: LanguageTag, sourceLocale: LanguageTag): CatalogLoadResult
}
```

`availableTargetLocales()` deriva de los idiomas destino cuyo sembrado tuvo éxito (un error de
validación de un idioma destino no MUST impedir que otro se sirva - requisito ya cubierto en la
spec). `availableSourceLocales(targetLocale)` deriva de los `sourceLocale` distintos presentes en
`catalog_phrase` para ese `targetLocale` (hoy siempre `[es-ES]`).

### Nuevo puerto `DeviceLocaleProvider` y función pura `ResolveSourceLocale`

```kotlin
// :core:domain
interface DeviceLocaleProvider {
    fun current(): LanguageTag
}

object ResolveSourceLocale {
    operator fun invoke(deviceLocale: LanguageTag, available: List<LanguageTag>): LanguageTag =
        available.firstOrNull { it == deviceLocale } ?: LanguageTag("es-ES")
}
```

`AndroidDeviceLocaleProvider` (`:core:platform`) implementa el puerto leyendo el locale del
dispositivo (mismo seam que `TextToSpeechPlayer` usa para convertir `LanguageTag` ↔ `Locale`, pero en
sentido inverso). Es una única fuente, sin estado ni corrutinas: no necesita dispatcher.

Un nuevo caso de uso `ObtainCatalogPhrasesForActiveLanguage` (`:core:domain`) compone
`DeviceLocaleProvider`, `ResolveSourceLocale` y `CatalogRepository` para resolver, dado un
`targetLocale`, qué `sourceLocale` usar y devolver sus frases. Los casos de uso existentes que leen
el catálogo (`ComposeNextBatch` vía `GenerateSession`/`ObtainActiveBatch`/`RegenerateActiveBatchForLevel`,
`ObtainPracticeSession`, `ObtainProgressOverview`) pasan a depender de este caso de uso en vez de
llamar a `CatalogRepository.phrases(...)` directamente con un locale fijo.

### `PracticeRepository`: mismo puerto, con `targetLocale` en cada operación

```kotlin
interface PracticeRepository {
    suspend fun read(targetLocale: LanguageTag): PracticeSnapshot
    suspend fun <T> transact(targetLocale: LanguageTag, change: (PracticeSnapshot) -> PracticeChange<T>): T
}
```

Sigue siendo una única fuente, `RoomPracticeDataSource`, sobre `PracticeDatabase` v4. `PhraseProgress`
y `PracticeSnapshot` no cambian de forma en el dominio: el `targetLocale` es un parámetro de la
operación de persistencia, no un campo del agregado en memoria (cada snapshot sigue representando el
estado de un único idioma destino, igual que hoy representa el único estado que existe).

`PracticeDatabase` v4 (migración destructiva, como ADR-0019 autoriza):
- `phrase_progress`: PK pasa de `(phrase_id)` a `(phrase_id, target_locale)`.
- `active_batch`: PK pasa de `id` fijo a `target_locale` (una fila por idioma destino con tanda
  activa, en vez de una fila fija `id = 1`).
- `session_counter`: PK pasa de `id` fijo a `target_locale`.
- El trigger de unicidad de `batch_position` (`active_batch_singleton`/`active_batch_capacity` y el
  índice único de `batch_position`) se reescribe para ser único dentro de `(target_locale,
  batch_position)`, no globalmente.

### `SettingsRepository`: mismo puerto, con `targetLocale` en CEFR/N y un ajuste global nuevo

```kotlin
interface SettingsRepository {
    suspend fun settings(targetLocale: LanguageTag): UserSettings
    suspend fun save(targetLocale: LanguageTag, settings: UserSettings)
    suspend fun activeTargetLocale(): LanguageTag
    suspend fun setActiveTargetLocale(targetLocale: LanguageTag)
}
```

`UserSettings(cefrLevel, batchSize)` no cambia de forma. Sigue siendo una única fuente,
`DataStoreSettingsDataSource`, con claves `cefr_level_<targetLocale>`, `batch_size_<targetLocale>` y
`active_target_locale`. Se descarta crear un repositorio nuevo solo para el idioma activo: es un
único valor adicional sobre el mismo almacén, no justifica un puerto propio.

`ObtainUserSettings`/`SaveUserSettings` ganan `targetLocale`; se añaden `ObtainAvailableLanguages`
(delegа en `CatalogRepository.availableTargetLocales()`), `ObtainActiveLanguage` y
`SelectActiveLanguage(targetLocale)` (delegan en los métodos nuevos de `SettingsRepository`,
validando que `targetLocale` esté en `availableTargetLocales()`).

### Presentación: cabecera compartida y nuevos componentes `Ds*`

- Nuevo `DsLanguageBadge(languageCode: String, modifier)` en `:core:designsystem`: icono circular con
  la bandera del idioma, siguiendo el estilo de `DsLevelBadge`/`SinRangoBadgeIcon`
  (`Canvas`/`Icon` dentro de un `Box` con `CircleShape`). **No recibe `LanguageTag`** (tipo de
  `:core:domain`): ADR-0010 exige que `:core:designsystem` no dependa de ningún módulo del proyecto y
  que sus componentes sean agnósticos del dominio, reciban primitivos o tipos propios del design
  system (igual que `DsLevelBadge` recibe `Int`, no el tipo de dominio de nivel de consolidación).
  `languageCode` es un código corto propio del design system (p. ej. `"pt-PT"`/`"pt-BR"` como texto
  plano, sin el value class de dominio) que decide qué bandera dibujar; el mapeo desde
  `LanguageTag` se hace en la feature (`SettingsViewModel`/`PracticeSessionViewModel`/
  `ProgressViewModel` o en el `Screen` sin estado), igual que ya ocurre con otros datos de dominio que
  llegan a componentes `Ds*`.
- Nuevo drawable vectorial por idioma destino (Portugal, Brasil) en `:core:designsystem`.
- Nuevo `DsLanguageOptions(options: List<DsLanguageOption>, selectedCode: String, onSelect: (String)
  -> Unit, modifier)` (nombre provisional), con `DsLanguageOption(code: String, displayName: String)`
  como tipo propio del design system (ninguno de los dos recibe `LanguageTag`): lista de opciones de
  idioma con bandera + nombre, usada dentro de Ajustes. Se implementa con
  `ExposedDropdownMenuBox`/`DropdownMenu` de Material 3 (ya en el version catalog vía Compose
  Material 3; no añade dependencia nueva), estilizada con tokens de `AppTheme` como el resto de
  `Ds*`. No se reutiliza el patrón de `CefrLevelButton` (grid de pills) porque el mock pide
  explícitamente un desplegable, no una rejilla de opciones.
- Cada pantalla (`PracticeSessionScreen`, `ProgressScreen`, `SettingsScreen`) añade `DsLanguageBadge`
  a su fila de cabecera existente (`PracticeHeader` ya usa `Row` + `Arrangement.SpaceBetween`;
  `ProgressScreen`/`SettingsScreen` pasan su `Text` de título a una `Row` equivalente). No se crea un
  `TopAppBar` de Material 3 compartido en `:app`: se sigue el patrón ya establecido de cabecera propia
  por pantalla (ver KDoc de `SettingsScreen`), añadiendo el badge en vez de introducir un mecanismo
  nuevo de navegación. Tocar el badge invoca una lambda `onNavigateToSettings` que cada `Route` cablea
  al cambio de pestaña de `:app` (mismo mecanismo que ya usa `SettingsRoute.onNavigateToPractice`).
- `SettingsUiState.Loaded` gana `activeTargetLocale: LanguageTag` y
  `availableTargetLocales: List<LanguageTag>`; `SettingsAction` gana `SelectLanguage(LanguageTag)`.

### Contenido pt-BR

Se entrega `catalog/phrases-pt-BR.json` nuevo, mismo `id`/`level`/`topic` que
`catalog/phrases-pt-PT.json` (que se renombra desde `catalog/phrases.json`), traduciendo `target` al
portugués de Brasil. Se genera por lotes y se revisa antes de incluirse (ADR-0004): la tarea de
contenido en `tasks.md` documenta el lote y una muestra revisada, no solo la validación estructural
automática (que ya cubre `CatalogJsonTest`/`RealCatalogContractTest`, mecánicos, no lingüísticos).

## Risks / Trade-offs

- [Riesgo] Traducir 1000 frases a pt-BR puede introducir errores lingüísticos que la validación
  estructural no detecta → Mitigación: revisión manual por muestreo documentada en `tasks.md`,
  además del test de contrato de ids que seguirá protegiendo la permanencia una vez publicado.
- [Riesgo] La migración destructiva de `PracticeDatabase` v3→v4 descarta el progreso de quien ya
  tenga la app instalada → Mitigación: ya aceptado en ADR-0013/ADR-0019 (proyecto pre-1.0.0, sin
  usuarios en producción).
- [Riesgo] Sembrar `CatalogDatabase` en cada arranque si se compara mal el `schemaVersion` podría
  recomputar innecesariamente → Mitigación: comparación por `targetLocale` contra
  `catalog_seed_version`, con test que cubre "sin cambio de schemaVersion no se resiembra" (ya en la
  spec de `phrase-catalog`).
- [Riesgo] Añadir la cabecera con badge a tres pantallas por separado puede divergir en espaciado si
  no se comparte el layout de la fila → Mitigación: aunque no hay `TopAppBar` compartido, se define un
  único patrón de `Row` con tokens de `AppTheme.spacing` que las tres pantallas replican, cubierto por
  `@PreviewLightDark` en cada una.

## Migration Plan

Se publica como una versión nueva de la app, sin ruta de migración de datos de usuario (ver riesgo
de migración destructiva arriba). El sembrado de `CatalogDatabase` ocurre en el primer arranque tras
actualizar, antes de que cualquier pantalla consulte el catálogo. No hay estrategia de rollback más
allá de la ya existente del proyecto (reinstalar una versión anterior), igual que en migraciones
previas de `PracticeDatabase`.

## Open Questions

- Iconografía exacta de bandera para pt-PT y pt-BR (qué asset de bandera usar) - se resuelve al
  implementar el design system a partir del mock, sin afectar a las specs ni a las tareas.
