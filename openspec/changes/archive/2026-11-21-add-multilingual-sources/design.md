# Design

## Context

Ver `proposal.md` - Why. Estado actual relevante (`:core:data`, `com.dalmuina.fraseafrase.core.data.catalog`):

- `CatalogJson` parsea un único tipo de documento (`CatalogDocument`): `schemaVersion`,
  `targetLocale`, `sourceLocale`, `phrases[{id, level, topic, target, source}]`. `headerOf()`
  decodifica solo `schemaVersion`/`targetLocale` para decidir si resembrar sin pagar el coste de
  parsear las ~1000 frases dos veces.
- `CatalogPhraseEntity` (Room, tabla `catalog_phrase`) tiene PK `(id, target_locale, source_locale)`
  — ya preparada para más de un origen por destino, según su propio comentario ("para no necesitar
  una migración el día que un idioma destino tenga más de un origen").
- `CatalogSeedVersionEntity` (tabla `catalog_seed_version`) tiene PK `target_locale` únicamente:
  una sola versión de siembra por idioma destino, sin distinguir documentos.
- `RoomCatalogDataSource.seedDocument`/`replaceSeed`: por cada `.json` en `assets/catalog/` (sin
  lista cerrada de nombres), compara `header.schemaVersion` contra `seedVersionFor(targetLocale)` y,
  si cambia, hace `deletePhrasesForTargetLocale(targetLocale)` + `insertPhrases(...)` +
  `upsertSeedVersion(...)` en una transacción. **Este borrado por `target_locale` es el problema**:
  un segundo documento del mismo destino con otro origen borraría las filas del primero al
  resembrar.
- `PhraseProgressEntity` (tabla `phrase_progress`) tiene PK `(phrase_id, target_locale)`, sin
  `source_locale`: el progreso ya es independiente del origen, sin cambios necesarios aquí.
- `CatalogDatabase` ya usa `fallbackToDestructiveMigration()` (`CatalogSchema.kt`): es una caché
  sembrada desde assets, sin datos de usuario, así que subir su versión no necesita una migración
  escrita a mano.
- `ResolveSourceLocale`/`ObtainCatalogPhrasesForActiveLanguage` (`:core:domain`) ya toman
  `available: List<LanguageTag>` genérico y ya excluyen el propio `targetLocale` del respaldo
  (ADR-0020): no necesitan cambios, solo que `availableSourceLocales(targetLocale)` pueda devolver
  más de un elemento real sin que uno pise al otro en Room.
- `DsLanguageBadge`/`languageBadgeSpec` (`:core:designsystem`) y `LanguageTag.toDsLanguageOption()`
  (`:feature:settings`) son los dos únicos sitios con un `when` cerrado sobre el código de idioma;
  ambos ya tienen rama `else` segura, así que añadir un idioma es añadir una rama más.
- `RealCatalogContractTest` (`:core:data`) tiene la lista de idiomas destino cubiertos como
  `@ValueSource(strings = ["pt-PT", "pt-BR", "es-ES"])` y lee `src/main/assets/catalog/phrases-<target>.json`
  directamente por nombre de fichero.

## Goals / Non-Goals

**Goals:**
- Separar el contenido de un idioma destino (`target`, `level`, `topic`) de su traducción desde un
  origen concreto (`source`), en documentos y tablas independientes, para que añadir un origen a un
  destino existente sea sembrar un documento nuevo, sin migración y sin tocar las frases destino ni
  otros orígenes ya sembrados.
- Migrar los 3 documentos existentes al nuevo formato sin cambiar ningún `id`, orden o texto.
- Añadir 5 idiomas destino nuevos (`it-IT`, `en-GB`, `de-DE`, `fr-FR`, `ru-RU`) con `es-ES` como
  único origen, usando ya el formato nuevo.
- Dejar `CatalogRepository`, `ResolveSourceLocale` y `PhraseProgressEntity` exactamente como están:
  su contrato y sus claves ya son correctos para N orígenes por destino.

**Non-Goals:**
- Traducir ningún contenido existente a un origen adicional (ver proposal.md - Fuera de alcance).
- Cambiar la independencia por idioma destino de nivel CEFR/N/tanda activa/progreso (ADR-0019): no
  se toca.
- Un endpoint o UI para elegir el idioma de origen manualmente: sigue derivándose del dispositivo
  (ADR-0019/ADR-0020).

## Decisions

### Dos tipos de documento JSON, distinguidos por su cabecera

```json
// phrases-pt-PT.json (frases destino)
{
  "schemaVersion": 1,
  "targetLocale": "pt-PT",
  "phrases": [
    { "id": "a1-saludos-001", "level": "A1", "topic": "saludos", "target": "Bom dia, tudo bem?" }
  ]
}
```

```json
// translations-pt-PT-from-es-ES.json (traducción)
{
  "schemaVersion": 1,
  "targetLocale": "pt-PT",
  "sourceLocale": "es-ES",
  "translations": [
    { "id": "a1-saludos-001", "source": "Buenos días, ¿qué tal?" }
  ]
}
```

`CatalogJson.headerOf(text)` distingue el tipo de documento por la presencia de `sourceLocale` en
la cabecera: con él, es un documento de traducción; sin él, de frases destino. No se añade un campo
de tipo explícito (`documentType`) porque `sourceLocale` ya es una marca suficiente y evita un campo
redundante que podría contradecirlo.

`CatalogJson.parse(text)` se bifurca en `parsePhrases(text)` y `parseTranslations(text)` según esa
misma cabecera, cada una con su propio documento serializable (`PhraseCatalogDocument`,
`TranslationCatalogDocument`) y su propio tipo de resultado, ya que las frases destino y las
traducciones ya no comparten forma. Ese resultado es un tipo nuevo, propio de `:core:data`
(`CatalogParseResult.PhrasesParsed`/`TranslationsParsed`/`Failed`), **sin relación de herencia con
`CatalogLoadResult`** del dominio: `CatalogLoadResult` es un `sealed interface` declarado en
`:core:domain`, y Kotlin exige que todo subtipo directo de un tipo sellado se declare en el mismo
módulo de compilación que la declaración — `CatalogJson` vive en `:core:data`, así que no puede
añadirle miembros. Además, una frase destino sin `source` violaría el invariante de `CatalogPhrase`
(`require(source.isNotBlank())`), así que ese estado intermedio no puede ser un `CatalogPhrase` del
dominio. `CatalogLoadResult.Loaded`/`Failed` solo se produce al final, en
`RoomCatalogDataSource.phrases()`, tras el JOIN que combina la fila de frase destino con la de
traducción — el contrato público de `CatalogRepository.phrases()` no cambia, tal como el resto de
este documento ya describe.

Un id de traducción sin frase destino correspondiente (requisito nuevo de la spec) se valida en
`RoomCatalogDataSource` al sembrar, no en `CatalogJson.parse`: `parse` valida un documento de forma
aislada, y esa comprobación necesita las frases destino ya sembradas de ese idioma destino. Por eso
`ensureSeeded()` siembra en dos pasadas, no en el orden en que `AssetManager.list()` devuelva los
ficheros (no documentado, y hoy sin garantía de orden): primero todos los documentos de frases
destino de `catalogDocuments()`, y solo después todos los de traducción. Así, en una instalación
nueva, una traducción nunca se procesa antes que las frases destino de su propio idioma, sin
importar en qué orden aparezcan los ficheros en los assets.

**Alternativa descartada**: un campo `documentType: "phrases" | "translation"` explícito. Añade un
campo que `sourceLocale` ya implica y que podría declararse de forma inconsistente con el resto de
la cabecera (p. ej. `documentType: "phrases"` con `sourceLocale` presente); se prefiere derivar el
tipo de un campo que ya es obligatorio para su tipo y ausente para el otro.

### Esquema de Room: `CatalogPhraseEntity` pierde columnas, `CatalogTranslationEntity` nueva

```kotlin
@Entity(tableName = "catalog_phrase", primaryKeys = ["id", "target_locale"])
data class CatalogPhraseEntity(
    val id: String,
    @ColumnInfo(name = "target_locale") val targetLocale: String,
    val position: Int,
    val level: String,
    val topic: String,
    val target: String,
)

@Entity(
    tableName = "catalog_translation",
    primaryKeys = ["id", "target_locale", "source_locale"],
)
data class CatalogTranslationEntity(
    val id: String,
    @ColumnInfo(name = "target_locale") val targetLocale: String,
    @ColumnInfo(name = "source_locale") val sourceLocale: String,
    val source: String,
)
```

`CatalogSeedVersionEntity` gana una columna nullable `source_locale` y cambia su PK a
`(target_locale, source_locale)`, usando una cadena vacía (o una constante dedicada, ver más abajo)
para la fila de frases destino, de modo que una tabla sola siga versionando ambos tipos de
documento sin otra tabla nueva:

```kotlin
@Entity(tableName = "catalog_seed_version", primaryKeys = ["target_locale", "source_locale"])
data class CatalogSeedVersionEntity(
    @ColumnInfo(name = "target_locale") val targetLocale: String,
    @ColumnInfo(name = "source_locale") val sourceLocale: String, // "" para el documento de frases destino
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
)
```

`RoomCatalogDataSource.seedDocument` se bifurca por el tipo de resultado del parseo:
- Frases destino: `deletePhrasesForTargetLocale(targetLocale)` + `insertPhrases(...)` (sin cambios
  de comportamiento, solo de columnas) + `upsertSeedVersion(targetLocale, sourceLocale = "", ...)`.
- Traducción: valida que cada id exista ya en `catalog_phrase` para ese `targetLocale` (requisito
  nuevo); si alguno no existe, falla la carga de ese documento sin sembrar nada de él.
  `deleteTranslationsFor(targetLocale, sourceLocale)` + `insertTranslations(...)` +
  `upsertSeedVersion(targetLocale, sourceLocale, ...)`.

Ningún borrado toca ya una fila fuera de su propio `(target_locale, source_locale)` exacto, lo que
resuelve el problema central: resembrar `pt-PT`/`en-GB` no borra `pt-PT`/`es-ES` ni las frases
destino de `pt-PT`.

`CatalogDao.phrasesFor(targetLocale, sourceLocale)` pasa de una consulta sobre una tabla a un JOIN:

```kotlin
@Query(
    "SELECT p.*, t.source FROM catalog_phrase p " +
        "JOIN catalog_translation t ON t.id = p.id AND t.target_locale = p.target_locale " +
        "WHERE p.target_locale = :targetLocale AND t.source_locale = :sourceLocale " +
        "ORDER BY p.position",
)
suspend fun phrasesFor(targetLocale: String, sourceLocale: String): List<CatalogPhraseWithSource>
```

con `CatalogPhraseWithSource` como fila de proyección Room (`@Embedded` + columna `source`), que
`CatalogMapping.toDomain()` convierte al mismo `CatalogPhrase` del dominio que hoy — el dominio no
cambia.

`CatalogDao.sourceLocalesFor(targetLocale)` también cambia de tabla: hoy lee `source_locale` de
`catalog_phrase` (columna que desaparece con este esquema); pasa a leer
`SELECT DISTINCT source_locale FROM catalog_translation WHERE target_locale = :targetLocale`.

**Alternativa descartada**: guardar `sourceLocale = null` (en vez de `""`) para la fila de frases
destino en `catalog_seed_version`. Room soporta columnas de PK nulas en SQLite, pero `null` en una
clave primaria compuesta complica las consultas (`source_locale = :sourceLocale` no coincide con
`NULL` sin `IS NULL` aparte); una cadena vacía, que nunca es un `sourceLocale` válido (la spec exige
locales no vacíos), es una marca más simple y ya usa el mismo tipo de columna.

### `CatalogDatabase` sube de versión, resembrado completo por `fallbackToDestructiveMigration()`

`CatalogSchema.kt` ya declara `fallbackToDestructiveMigration()` (sin datos de usuario que
preservar: es una caché). Subir la versión de 1 a 2 recrea las tablas vacías y
`RoomCatalogDataSource.ensureSeeded()` las vuelve a sembrar desde los assets en el siguiente
arranque, con el nombre de tabla y columnas nuevos. No se escribe una `Migration` manual.

### Nombrado de ficheros: `phrases-<target>.json` y `translations-<target>-from-<source>.json`

`RoomCatalogDataSource.readPackagedCatalogDocuments` ya lee todos los `.json` de `assets/catalog/`
sin lista cerrada de nombres, así que el nombrado no afecta al sembrado. Se fija por convención,
para que un desarrollador localice el documento correcto y para que `RealCatalogContractTest` pueda
seguir leyendo `phrases-<target>.json` por nombre:
- `phrases-<target>.json`: documento de frases destino.
- `translations-<target>-from-<source>.json`: documento de traducción.

### Migración de contenido: 3 documentos existentes + 5 idiomas destino nuevos

- `phrases-pt-PT.json`, `phrases-pt-BR.json`, `phrases-es-ES.json`: se reescriben sin `sourceLocale`
  ni `source`, conservando `id`/`level`/`topic`/`target` y el orden exacto.
- `translations-pt-PT-from-es-ES.json`, `translations-pt-BR-from-es-ES.json`,
  `translations-es-ES-from-pt-PT.json`: contienen el `source` que tenían los documentos originales,
  con los mismos `id`.
- 5 documentos de frases destino nuevos (`phrases-it-IT.json`, `phrases-en-GB.json`,
  `phrases-de-DE.json`, `phrases-fr-FR.json`, `phrases-ru-RU.json`) con 1000 frases cada uno,
  traducidas desde el contenido ya revisado de `phrases-pt-PT.json`/`translations-pt-PT-from-es-ES.json`
  (mismo criterio que `es-ES` usó con `pt-PT`: reutilizar un par ya revisado en vez de traducir dos
  textos nuevos a la vez). Ids propios e independientes por idioma destino, igual que los 3
  existentes.
- 5 documentos de traducción nuevos (`translations-it-IT-from-es-ES.json`, etc.) con el `source` en
  español para cada id de su idioma destino.
- `released-ids-<target>.txt` vacío para cada idioma destino nuevo, mismo patrón que `pt-BR`/`es-ES`.

### Estrategia de test

- `CatalogJsonTest`: casos existentes de frases destino adaptados al documento sin `source`; casos
  nuevos para el documento de traducción (parseo, validación de campos, `headerOf` distinguiendo
  ambos tipos).
- `RoomCatalogDataSourceTest`: siembra independiente de frases destino y traducciones (resembrar un
  origen no borra otro ni las frases destino), id de traducción sin frase destino correspondiente,
  JOIN de `phrasesFor`. Caso explícito de primera siembra con los documentos entregados en orden
  inverso (el de traducción antes que el de frases destino, del mismo idioma destino): ambos deben
  sembrarse igual, verificando que `ensureSeeded()` no depende del orden en que
  `catalogDocuments()` los entregue.
- `RealCatalogContractTest`: `@ValueSource` con los 8 idiomas destino; lee
  `phrases-<target>.json` (ya sin `source`) para la permanencia de ids, sin tocar los documentos de
  traducción.
- `DsLanguageBadgeTest`: 5 escenarios nuevos (`givenItItWhenPaintingThenItItIconIsShown`, etc.),
  mismo patrón que `es-ES`.
- `SettingsScreenTest`/`SettingsViewModelTest`: se extienden si ya cubren la lista de nombres
  visibles con un fake de N idiomas; no se necesita un test nuevo si el escenario ya es genérico.
- No hace falta ningún cambio en `:core:testing` (`FakeCatalogRepository`,
  `FakeDeviceLocaleProvider`): ya aceptan locales arbitrarios y no modelan el formato de documento.

### ADR nuevo: ADR-0022, sustituye la parte de formato de documento de ADR-0019

ADR-0013 ya tiene su línea de estado en `Sustituido por 0019` (supersesión completa); no se vuelve
a tocar, para no perder esa referencia. ADR-0022 declara en su cabecera
`Sustituye a: ADR-0019 (solo la parte de formato de documento)`, y solo la línea de estado de
ADR-0019 pasa a `Sustituido por ADR-0022 (solo la parte de formato de documento)`. Confirma que
ADR-0019 (catálogo en Room sembrado por idioma destino, independencia por idioma destino) y
ADR-0020 (respaldo de origen) no cambian de decisión en nada más: solo que el documento de catálogo
se divide en frases destino y traducción para admitir más de un origen por destino sin duplicar
contenido ni migrar Room en cada ampliación.

## Risks / Trade-offs

- [Riesgo] Olvidar la validación "traducción sin frase destino" dejaría ids huérfanos sembrados sin
  aviso → Mitigación: test explícito en `RoomCatalogDataSourceTest`, cubierto por el escenario de
  spec correspondiente.
- [Riesgo] Traducir 5000 frases nuevas (5 idiomas × 1000) puede introducir errores que la
  validación estructural no detecta → Mitigación: igual que `pt-BR`/`es-ES`, revisión por muestreo
  documentada en `tasks.md`, más el test de contrato de ids.
- [Riesgo] La migración de los 3 documentos existentes podría perder o reordenar un id al separar
  `source` → Mitigación: `released-ids-pt-PT.txt`/`pt-BR.txt`/`es-ES.txt` ya existentes siguen
  pasando el test de contrato tras la migración, sin tocar esos ficheros de referencia.
- [Riesgo] `CatalogDatabase` v1→v2 con `fallbackToDestructiveMigration()` no pierde progreso de
  usuario (vive en `PracticeDatabase`), pero si algún test asumiera persistencia de `catalog_phrase`
  entre versiones de la app, fallaría → Mitigación: no existe tal test hoy (el catálogo siempre se
  resiembra desde assets); se verifica por inspección en `code-reviewer`.

## Migration Plan

Se publica como contenido y esquema nuevos en una versión de la app: `CatalogDatabase` sube de
versión y se recrea vacía en el primer arranque tras actualizar (`fallbackToDestructiveMigration()`,
sin pérdida de progreso de usuario, que vive en `PracticeDatabase`), y `ensureSeeded()` la vuelve a
sembrar desde los assets nuevos. No hace falta ninguna migración de datos escrita a mano.
