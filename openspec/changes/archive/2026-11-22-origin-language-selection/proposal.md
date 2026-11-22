# Proposal: origin-language-selection

## Why

Los 8 documentos `phrases-<idioma>.json` comparten los mismos 1000 ids: cualquier idioma del catálogo puede
servir de origen de cualquier otro. Hoy, sin embargo, el origen solo puede ser uno de los pares que tienen un
`translations-<destino>-from-<origen>.json` (casi todos desde `es-ES`): un usuario con el móvil en inglés o francés
ve las frases en español aunque su idioma esté en el catálogo. Esa limitación está en el código y en ficheros por
par, no en el contenido, y obliga a mantener textos de origen duplicados que ya existen como `target` del documento
del otro idioma.

## What Changes

- **Idioma de origen:** el del móvil si está entre los idiomas del catálogo; si no, el idioma por defecto de la
  app (hoy `es-ES`). Esa es toda la lógica: el usuario no lo elige ni se guarda, y no depende del idioma destino.
  El valor por defecto se configura en `:app`, no en el dominio (ADR-0003).
- El idioma destino se puede elegir entre todos los idiomas del catálogo **excepto el de origen**: nunca
  origen = destino. Si el destino guardado (o el primero disponible) coincide con el origen, se usa el primer
  idioma disponible distinto del origen.
- **Única fuente de verdad del texto de origen:** sale del `target` de `phrases-<origen>.json` con el mismo id.
- **BREAKING (contenido y esquema interno):** se eliminan los 8 `translations-*.json`, la tabla
  `catalog_translation`, el tipo "documento de traducción" y su parser. `CatalogDatabase` sube de versión con
  recreación destructiva (es una caché sembrada; el progreso vive en `PracticeDatabase` y no se toca).
- Nuevo invariante de contenido: todos los documentos de frases tienen exactamente el mismo conjunto de ids
  (test de contrato), para que cualquier par tenga texto de origen para cada frase.
- La práctica refleja un cambio del idioma del móvil al volver a la pestaña, aunque la tanda activa no cambie.
- El progreso, la tanda activa y los ajustes siguen ligados al idioma destino, independientes del origen.
- ADR-0023 nuevo: sustituye la parte de traducciones de ADR-0022 y la regla de origen de ADR-0019/ADR-0020.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `phrase-catalog`: desaparece el documento de traducción; el texto de origen sale del documento de frases del
  idioma de origen; ids paralelos entre idiomas; el origen es el idioma del móvil o el de defecto, sin depender del
  destino.
- `user-settings`: el idioma destino activo nunca coincide con el origen.
- `settings-screen`: el selector de idioma destino excluye el idioma de origen.
- `practice-session`: un cambio del idioma de origen se refleja en la tanda activa al volver a Practicar.

## ADR afectados

- **ADR-0023 (nuevo, Propuesto):** origen = idioma del dispositivo o el de defecto, y texto de origen desde el
  documento de frases del idioma de origen.
- **ADR-0022:** sustituido en la parte de documentos y tabla de traducción.
- **ADR-0019:** sustituido en la regla de origen (catálogo en Room, selección de destino e independencia por
  destino siguen vigentes).
- **ADR-0020:** sustituido por completo (el respaldo deja de excluir el destino; es el destino el que se ajusta).
- **ADR-0003 / ADR-0013:** se respetan; ningún idioma fijo en el dominio, y los ids siguen siendo permanentes con
  altas al final, ahora en todos los documentos a la vez.
- **ADR-0012:** sin cambios en el método; el cambio solo afecta a qué texto de origen se muestra.

## Fuera de alcance

- Elegir el idioma de origen en Ajustes o guardarlo.
- Interfaz de la app traducida al idioma de origen (los textos de la UI siguen en español).
- Ajustes o progreso por par origen/destino.
- Contenido nuevo (idiomas o frases); TTS del texto de origen.
- Cambiar el indicador de idioma de la cabecera (`language-indicator` sigue mostrando solo el destino).

## Impact

- `:core:domain`: `CatalogRepository` (se retira `availableSourceLocales`), `ResolveSourceLocale`,
  `ObtainCatalogPhrasesForActiveLanguage`, `ObtainActiveLanguage`, `SelectActiveLanguage`, caso de uso nuevo
  `ObtainSourceLanguage`.
- `:core:data`: `CatalogDao`, `CatalogDatabase` (v3), `CatalogJson`, `CatalogParseResult`, `RoomCatalogDataSource`,
  `CatalogSeedVersionEntity`; se borran `CatalogTranslationEntity` y los `translations-*.json`; tests de contrato.
- `:core:testing`: `FakeCatalogRepository`.
- `:feature:settings`: `SettingsUiState`, `SettingsViewModel`, `SettingsScreen` (opciones de destino).
- `:feature:session`: comparación de `Resume` en `PracticeSessionViewModel`.
- `:app`: provisión del idioma de origen por defecto (`es-ES`) en el grafo de DI.
- `docs/adr/`: ADR-0023 y líneas de estado de 0019, 0020 y 0022.
