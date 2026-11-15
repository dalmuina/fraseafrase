# Proposal

## Why

Hoy la app solo enseña un idioma (pt-PT desde es-ES). El usuario quiere empezar a ofrecer más de un
idioma destino, empezando por pt-BR, elegible desde Ajustes, con progreso, racha, nivel CEFR y
tamaño de tanda independientes por idioma, y un indicador visible del idioma activo en la cabecera
de cada pantalla. ADR-0019 (que sustituye la parte de almacenamiento de ADR-0013 y modifica
ADR-0003) ya autoriza esto: mueve el catálogo a Room, deja de diferir la selección de variante y
deriva el idioma de origen del dispositivo, con es-ES como valor por defecto.

## What Changes

- El catálogo pasa de un único JSON en memoria a tablas Room sembradas desde JSON por idioma
  destino (`catalog/phrases-pt-PT.json`, `catalog/phrases-pt-BR.json`), con las mismas reglas de
  permanencia de ids de ADR-0013, ahora verificadas por idioma destino.
- Se añade contenido completo en pt-BR: traducción de las 1000 frases existentes (mismo id, nivel y
  tema; solo cambia `target`).
- El idioma de origen deja de fijarse a `es-ES` en código: se deriva del locale del dispositivo, con
  `es-ES` como valor por defecto cuando el catálogo no tiene contenido para ese locale.
- El nivel CEFR, el tamaño de tanda N, la tanda activa, el contador de sesiones y el progreso por
  frase pasan a ser independientes por idioma destino. **BREAKING**: `PracticeDatabase` sube de
  versión 3 a 4 con migración destructiva (proyecto pre-1.0.0, sin usuarios en producción, mismo
  criterio que ADR-0013).
- Ajustes añade un selector del idioma destino activo (desplegable con bandera y nombre, según el
  mock) y muestra el nivel CEFR y N del idioma seleccionado.
- Las pantallas de Practicar, Progreso y Ajustes muestran un icono circular con el idioma destino
  activo en su cabecera; tocarlo navega a Ajustes (no abre un segundo desplegable duplicado).

## Capabilities

### New Capabilities

- `language-indicator`: icono circular del idioma activo en la cabecera de Practicar, Progreso y
  Ajustes, y su navegación a Ajustes al tocarlo.

### Modified Capabilities

- `phrase-catalog`: el catálogo pasa de JSON en memoria a Room sembrado desde JSON por idioma
  destino; conviven varios idiomas destino con su propio contenido; el idioma de origen se deriva
  del locale del dispositivo con es-ES como valor por defecto.
- `user-settings`: el nivel CEFR y N pasan a guardarse por idioma destino; se añade el ajuste del
  idioma destino activo (persistido, con valor por defecto y fallback ante valores no disponibles).
- `practice-persistence`: el progreso por frase, la tanda activa y el contador de sesiones pasan a
  persistirse por idioma destino en vez de como estado único global.
- `settings-screen`: se añade el selector del idioma destino activo; el nivel CEFR y N mostrados y
  editados son los del idioma seleccionado.

## Out of scope

- Traducir el contenido a ningún idioma de origen distinto de es-ES: el puerto de locale del
  dispositivo y su fallback quedan listos, pero solo hay contenido en es-ES.
- Un segundo desplegable de idioma en la cabecera de Practicar/Progreso: el icono circular navega a
  Ajustes, donde ya existe el selector completo.
- Direcciones de práctica distintas de origen→destino, reconocimiento de voz, u otros modos de
  práctica (fuera de alcance de ADR-0012, sin cambios aquí).
- Migrar o conservar el progreso existente al subir `PracticeDatabase` a v4: se descarta, igual que
  en migraciones anteriores de este proyecto.

## Impact

- `:core:domain`: `CatalogRepository`, `PracticeRepository`, `UserSettings`/`SettingsRepository` y
  los casos de uso de práctica/ajustes ganan un parámetro o dimensión de idioma destino; nuevo
  puerto `DeviceLocaleProvider`.
- `:core:data`: nuevas tablas Room de catálogo y siembra; `PracticeDatabase` v3→v4 con nuevas claves
  por idioma en `phrase_progress`, `active_batch` y `session_counter`; `DataStoreSettingsDataSource`
  con claves por idioma; nuevo contenido `catalog/phrases-pt-BR.json`.
- `:core:platform`: implementación de `DeviceLocaleProvider`.
- `:core:designsystem`: nuevo componente de desplegable de idioma, nuevo `DsLanguageBadge`, nuevos
  drawables de bandera (Portugal, Brasil).
- `:feature:settings`: nuevo selector de idioma en `SettingsUiState`/`SettingsAction`.
- `:feature:session`, `:feature:progress`: cabecera con el icono de idioma; lectura del idioma
  activo para resolver su propia tanda/progreso.
- `:app`: wiring de Hilt para los puertos nuevos.
