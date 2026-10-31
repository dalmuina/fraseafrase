# Design

## Context

`:core:domain` ya contiene las reglas de tanda (`PracticeBatch`, `CarryQueue`, `ComposeNextBatch`, `ResolveBatchClosure`, `PhraseProgress`, `BatchSize`) y el puerto `Clock`, todo Kotlin puro. `:core:data`, `:core:platform` y `:core:testing` existen como módulos vacíos de dependencias de persistencia. `CatalogPhrase` solo tiene `id` y `level`. Las specs vigentes (`practice-batch`, `spaced-repetition`) no cambian. ADR-0004 queda sustituido por ADR-0013 (ver Decisiones, D1).

## Goals / Non-Goals

**Goals:**
- Persistir progreso, tanda activa y cola en Room 3, con cierre atómico.
- Catálogo JSON en assets, cargado en memoria, con contrato verificado en CI.
- Ajustes en DataStore con fallback a valores por defecto.
- Casos de uso que impongan el orden del cierre sin que la UI pueda equivocarse.
- Tests JVM locales con el mismo SQLite que el dispositivo.

**Non-Goals:**
- Migraciones de Room (versión 1 con esquema exportado).
- Política de descarte de frases del catálogo (los ids son permanentes).
- Pantallas de práctica o ajustes (features posteriores).

## Decisiones

### D1. ADR-0013 sustituye al ADR-0004

La sustitución es completa. ADR-0013 restablece offline-first, Room para el progreso, DataStore para los ajustes y el veto a backend e IA, y cambia el almacenamiento del catálogo: de "base de datos precargada" a JSON cargado en memoria. Objetivo de producto: ampliar el catálogo en futuras versiones editando solo el JSON, sin migraciones de Room.

Durante el cambio, ADR-0013 está en estado `Propuesto` y se edita libremente. ADR-0004 (`Aceptado`) no se toca hasta la tarea 11.1. Al cerrar, en un único commit, ADR-0013 pasa a `Aceptado` y la línea de estado de ADR-0004 pasa a `Sustituido por [ADR-0013](...)`, con el formato de ADR-0005 (tarea 11.1).

La base del estado `Sustituido por` es `docs/adr/README.md`, que dice que el antiguo pasa a ese estado. ADR-0001 aporta solo la regla general de no editar ADR aceptados. La excepción de la línea de estado se escribe en `CLAUDE.md`: un ADR `Propuesto` puede editarse, y uno `Aceptado` solo admite el cambio de su línea de estado. ADR-0013 también debe justificar la excepción de detekt de D9 y el umbral de cobertura de `:core:data` de la sección Riesgos.

### D2. Locales por frase, copiados desde la raíz

El JSON declara `targetLocale` y `sourceLocale` una vez en la raíz. El mapper los copia en cada `CatalogPhrase`, así el dominio mantiene locales por frase como exige ADR-0003 sin enmienda. El data source valida que no estén vacíos y que tengan formato BCP-47 (`pt-PT`, no `pt_PT`), como exige la spec `phrase-catalog`.

```kotlin
// :core:domain
data class CatalogPhrase(
    val id: PhraseId,
    val level: CefrLevel,
    val topic: Topic,              // @JvmInline value class con require(no vacío)
    val targetLocale: LanguageTag, // value class con require(no vacío); sin valor por defecto en el dominio
    val sourceLocale: LanguageTag,
    val target: String,
    val source: String,
)
```

El `aCatalogPhrase()` de `:core:testing` recibe defaults para los campos nuevos. `ComposeNextBatch` y sus tests solo usan `id` y `level`, así que no cambian.

### D3. Puerto `PracticeRepository` con transacción pura

```kotlin
// :core:domain
interface PracticeRepository {
    /** Lee, aplica [change] y escribe dentro de una única transacción. [change] es pura: sin E/S ni suspend. */
    suspend fun <T> transact(change: (PracticeSnapshot) -> PracticeChange<T>): T
}

data class PracticeSnapshot(
    val progress: List<PhraseProgress>, // todas las frases con fila, incluidas las de tanda y cola
    val activeBatch: ActiveBatch?,      // orden de tanda + capacidad; null si no hay tanda activa
    val carry: CarryQueue,
)

data class PracticeChange<T>(val next: PracticeSnapshot, val result: T)
```

- Lectura separada: `suspend fun read(): PracticeSnapshot`, implementada con una transacción de lectura. La UI no necesita reactividad en este cambio; `observe(): Flow<PracticeSnapshot>` queda fuera.
- Room aparece solo en `RoomPracticeDataSource : PracticeRepository` (`:core:data`). `transact` abre una transacción de escritura **IMMEDIATE**, para que dos respuestas rápidas se serialicen. La API exacta (`withWriteTransaction` en Room 3) se confirma en la primera tarea con la documentación oficial.
- Ventaja frente a `commitClosure(datos)`: la lectura y la escritura ocurren en la misma transacción, así que no hay lecturas obsoletas.
- El data source escribe solo las filas que cambian para no reescribir todo el progreso en cada respuesta.
- `RoomPracticeDataSource` recibe su dispatcher por inyección, como los demás data sources (CLAUDE.md).
- Orden de comprobación de `CloseActiveBatch`: ver D4.
- **Orden de escritura en dos fases** (necesario por los UNIQUE y los CHECK, que SQLite evalúa por sentencia):
  - Fase 1: a cada fila que cambie de posición se le asigna una posición temporal negativa única (`-1 - índice`) en la misma columna que tenía. Así no colisiona con ninguna posición final ni viola los CHECK.
  - Fase 2: se escriben las posiciones finales y se limpia la columna sobrante, en una única sentencia por fila cuando la fila pasa de tanda a cola o al revés.
- `AnswerPhrase` coloca la frase al final con `max(batch_position) + 1`, que no exige contigüidad. El riesgo de colisión queda solo en el cierre.
- El fake de `:core:testing` implementa `transact` y `read` en memoria con un `Mutex` (`withLock` alrededor del bloque).
- `Clock` y el catálogo se leen antes de la transacción y se capturan en el bloque como datos; el bloque no lee nada del exterior.

### D4. Casos de uso

- **`ObtainActiveBatch(clock, settingsRepository, catalogRepository, practiceRepository)`**: si hay tanda activa, la devuelve; si no, compone la siguiente con `ComposeNextBatch` dentro de `transact`. Resultado: `Existing`, `Created` o `NothingAvailable`.
- **`AnswerPhrase`**: `RecordSelfAssessment` y `PracticeBatch.answer` dentro de `transact`. Actualiza la fila y la posición (al final).
- **`CloseActiveBatch(expectedUncertified)`**: dentro de `transact`, verifica que `uncertifiedCount()` coincide; si no, `Stale`. Si no hay tanda, `NoActiveBatch`. Si coincide: `ResolveBatchClosure`, `CarryQueue.afterClosure`, la tanda nueva con `ComposeNextBatch` sobre el progreso ya resuelto y, dentro de la misma transacción, la persistencia. Resultado: `Closed`.

El aviso se calcula con `uncertifiedCount()`, que es pura sobre una tanda inmutable, así que el orden de cálculo no es un riesgo. El riesgo está en la escritura (colisiones de posiciones, cubierto por la escritura en dos fases de D3) y en el aviso obsoleto (cubierto por `expectedUncertified`).

**Orden de comprobación de `CloseActiveBatch`:** 1) si no hay tanda activa, `NoActiveBatch`; 2) si `uncertifiedCount()` no coincide con `expectedUncertified`, `Stale`. Si se dan ambas condiciones, gana `NoActiveBatch`.

**Fecha del cierre y de la composición:** `CloseActiveBatch` compone la tanda siguiente con la fecha de cierre (`clock.today()` capturada antes de `transact`). `ObtainActiveBatch` compone con la fecha de la llamada. Si el día cambia con una tanda activa, la tanda no se recompone: sigue activa hasta que el usuario la cierre, y las frases con repaso del nuevo día entran en la tanda siguiente.

### D5. Esquema de Room (versión 1)

Tabla `phrase_progress`:
- `phrase_id TEXT PRIMARY KEY`
- `level INTEGER NOT NULL CHECK (level BETWEEN 0 AND 5)`
- `streak INTEGER NOT NULL CHECK (streak >= 0)`
- `answered_in_batch INTEGER NOT NULL`
- `next_review TEXT NULL` (fecha ISO)
- `batch_position INTEGER NULL`
- `carry_position INTEGER NULL`
- `CHECK (batch_position IS NULL OR carry_position IS NULL)`
- `CHECK (level > 0 OR batch_position IS NOT NULL OR carry_position IS NOT NULL)`
- `CHECK ((level = 0) = (next_review IS NULL))`
- Índices `UNIQUE` sobre `batch_position` y sobre `carry_position` (SQLite permite varios NULL).

Tabla `active_batch` de una sola fila:
- `id INTEGER PRIMARY KEY CHECK (id = 1)`
- `capacity INTEGER NOT NULL CHECK (capacity BETWEEN 10 AND 30)`

Su existencia indica si hay tanda activa.

**Verificación de conflicto (D5 con la spec `practice-batch`):** una fila de nivel 0 de una frase nueva de la tanda activa no es "nueva sin progreso". `ComposeNextBatch` solo compone tras resolver el cierre, y su filtro de nuevas (`it.id !in byId`) excluye cualquier frase con fila. No hay conflicto.

**Nota de implementación:** los CHECK se evalúan por sentencia. Al mover una frase de tanda a cola, la sentencia debe actualizar `batch_position` y `carry_position` a la vez; no puede hacerlo en dos pasos.

Exportación del esquema a `core/data/schemas/` con `schemaDirectory`, versionado en git.

**Desviación de D5 (tarea 8.2):** Room 3 no expresa `CHECK` en sus anotaciones. Las mismas invariantes se aplican con triggers `BEFORE INSERT` y `BEFORE UPDATE` creados en `RoomDatabase.Callback.onCreate` (`PracticeSchema`): nivel entre 0 y 5, no ambas posiciones a la vez, nivel 0 siempre en tanda o cola, `next_review` nulo exactamente si el nivel es 0, y una sola fila de `active_batch` con id 1 y capacidad entre 10 y 30. Los UNIQUE de posición sí son índices de Room y aparecen en el esquema exportado.

**Escritura en dos fases (tarea 8.7):** `RoomPracticeDataSource.transact` lee la instantánea, calcula la transición y escribe. Primero, cada fila que deja una posición ocupada recibe una posición temporal negativa y única. Después, cada fila recibe su posición final. Así ninguna sentencia viola un UNIQUE ni deja una fila de nivel 0 sin posición.

### D6. Catálogo JSON

Archivo `core/data/src/main/assets/catalog/phrases.json`, con `schemaVersion: 1`, locales en la raíz y `phrases` en orden de catálogo. `CatalogJson.parse(text)` es una función pura en `:core:data`, testable en JVM. `AssetsCatalogDataSource : CatalogRepository` solo lee el asset y delega en el parser. El parseo usa `kotlinx.serialization` con el plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21.

Test de contrato: lee `src/main/assets/catalog/phrases.json` desde la ruta del módulo, sin Android, y comprueba ids únicos y no vacíos, CEFR válido, textos y topics no vacíos y locales presentes.

### D7. Ajustes

`DataStoreSettingsDataSource : SettingsRepository` (`:core:data`) con DataStore Preferences 1.3.0. Claves `cefr_level` (String) y `batch_size`. DataStore compara las claves por nombre y, si el valor guardado tiene otro tipo, leer con una clave tipada lanza `ClassCastException` (comprobado en los tests). Por eso `batch_size` se lee del mapa completo (`asMap()`), sin castear: se acepta un `Int` o un texto con forma de entero, y cualquier otra cosa cae al valor por defecto:

```kotlin
val stored = when (val raw = preferences.asMap().entries.firstOrNull { it.key.name == "batch_size" }?.value) {
    is Int -> raw
    is String -> raw.toIntOrNull()
    else -> null
}
val n = stored?.takeIf { it in BatchSize.MIN..BatchSize.MAX } ?: BatchSize.DEFAULT.value
```

Un `IOException` al leer devuelve `UserSettings.DEFAULT`. El nivel CEFR no válido cae a `A1`. Los dispatchers se inyectan en `DataStoreSettingsDataSource` y `AssetsCatalogDataSource`, como exige CLAUDE.md. En los tests, DataStore no admite dos instancias activas sobre el mismo fichero, y en Windows su renombrado falla si el destino ya existe: cada test usa una ruta nueva.

### D8. Reloj

`DeviceClock` implementa `Clock` en `:core:platform`, usa `LocalDate.now(zone)` con la zona inyectada y `java.time`, que es nativo desde API 26. El binding de Hilt (`@Binds`) vive en `:app`. El nombre evita colisionar con `android.os.SystemClock`.

### D9. Detekt

`config/detekt/detekt.yml`: `UnnecessaryAbstractClass` con `ignoreAnnotated: ['Database']`. Sin `@Suppress`. Se verifica con la primera ejecución de `./gradlew detekt`.

### D10. Versiones y justificación

| Librería | Versión | Justificación |
|---|---|---|
| Room 3 (`androidx.room3`) | 3.0.3 | ADR-0004 exige Room. Última estable; 3.1.0 solo tiene alphas. Requiere KSP, sin KAPT. |
| `androidx.sqlite:sqlite-bundled` | 2.7.1 | `BundledSQLiteDriver` en producción y en tests JVM: mismo SQLite en ambos. Última estable. |
| DataStore Preferences | 1.3.0 | ADR-0004 exige DataStore. Última estable. |
| `kotlinx-serialization-json` | 1.12.0 | Parseo del JSON sin depender de Android; testable en JVM. Última estable. |
| Plugin serialization | 2.4.21 | Alineado con Kotlin 2.4.21. La 2.5.0-Beta2 es beta. |
| KSP | 2.3.11 | Se intentó 2.3.13 (tarea 1.1): el POM del plugin Gradle existe, pero el JAR `com.google.devtools.ksp.gradle.plugin-2.3.13.jar` devuelve 404 en Gradle Plugin Portal, así que no se puede resolver. Se mantiene 2.3.11 hasta que el artefacto esté publicado. |

Diferencias con Room 2 que el código debe reflejar: `@ColumnTypeConverter` en lugar de `@TypeConverter`; `withWriteTransaction` en lugar de `runInTransaction`; DAO con funciones `suspend` o `Flow`; `SQLiteDriver` obligatorio al construir la base de datos.

### D11. Inyección y módulos

- `:core:domain`: puerto `PracticeRepository`, `CatalogRepository`, `SettingsRepository`, casos de uso, `Topic`, `LanguageTag`, `CatalogPhrase` ampliado.
- `:core:data`: `RoomPracticeDataSource`, `AssetsCatalogDataSource`, `DataStoreSettingsDataSource`, `CatalogJson`, la base de datos Room y su esquema.
- `:core:platform`: `DeviceClock`.
- `:core:testing`: fakes en memoria `FakePracticeRepository` (con `Mutex`), `FakeCatalogRepository`, `FakeSettingsRepository` y `FakeClock`, más builders. Se crean en la tarea 4.0, antes de los tests de casos de uso.
- `:core:data`: una factoría de la base de datos `createPracticeDatabase(path: String, driver: SQLiteDriver)` que usan los tests JVM (con una ruta temporal o `:memory:`) y `:app`; así el proveedor no vive solo en `:app`. La firma se confirma en 1.3.
- `:app`: `@Binds` de los interfaces a sus implementaciones.

## Risks / Trade-offs

- **Catálogo que quita frases con progreso guardado** → `IllegalArgumentException` en cada composición. Mitigación: regla de permanencia de ids (`phrase-catalog`), test de contrato en CI y regla en `CLAUDE.md`. No hay política de descarte por decisión explícita.
- **Versiones de Room 3 y AGP 9** → la verificación de minSdk y AGP es la primera tarea de andamiaje, compilando en verde. Las notas de versión de Room 3 no indican esos mínimos.
- **Driver nativo de SQLite en JVM** → `sqlite-bundled` puede no cargar en Windows o en CI Linux. Mitigación: el spike de andamiaje lo comprueba antes de escribir la lógica.
- **Plugin de serialization con el Kotlin integrado de AGP 9** → el módulo no aplica `org.jetbrains.kotlin.android`. Mitigación: verificar que el plugin de serialization se aplica sin ese plugin en el andamiaje.
- **Atomicidad del cierre** → depende de que el data source ejecute el bloque y todas las escrituras en una sola transacción. Mitigación: test de rollback forzando una violación de CHECK a mitad de la escritura.
- **Cobertura de `:core:data`**: 80 % de líneas (ADR-0007 no fija umbral para datos). Kover debe excluir el código generado de Room (`*_Impl`, `*Database_Impl`, `*Dao_Impl`) en `build-logic`. Excluir código generado no baja el umbral.
- **Rendimiento de `transact`**: cada respuesta carga todo el progreso, O(n) con n frases con fila. Es aceptable en el MVP. Si se nota, la mitigación prevista es una lectura acotada a la tanda activa para responder, sin cambiar el puerto ni la spec.
- **D3 de `add-practice-batch` queda superada**: las frases nuevas se persisten al componer la tanda, no al responder. Este cambio la sustituye; no afecta al comportamiento observable de las specs vigentes.

## Verificación de andamiaje (tareas 1.2–1.5)

- **Room 3.0.3 (1.3)**, confirmado con las clases de `room3-runtime` 3.0.3:
  - `RoomDatabase.Builder.setDriver(SQLiteDriver)`: el driver es obligatorio.
  - `withWriteTransaction` y `useWriterConnection` (extensiones de `RoomDatabaseKt`).
  - `Transactor.SQLiteTransactionType.IMMEDIATE`: la transacción de escritura se pide explícitamente con `withTransaction(IMMEDIATE)` en `RoomPracticeDataSource` (D3).
  - `androidx.room3.ColumnTypeConverter`, no `@TypeConverter`.
  - `Room.databaseBuilder(name, factory)` e `Room.inMemoryDatabaseBuilder(factory)`: no requieren `Context`, así que sirven en tests JVM. La forma de `factory` (generada por KSP o `@ConstructedBy`) se fija en la tarea 8.2.
- **Marcadores de plugin**: el marcador de `androidx.room3` existe, pero Gradle no lo resuelve para 3.0.3 en este entorno. El plugin de Room y el de serialization se ponen en el classpath de `build-logic` (`room3-gradle-plugin` y `kotlin-serialization`) y se aplican por id desde `fraseafrase.persistence`, igual que AGP y KGP. `org.jetbrains.kotlin.plugin.serialization` no tiene marcador propio en 2.4.21 (su JAR devuelve 404); lo registra `kotlin-serialization`.
- **Esquema (1.3)**: `RoomExtension.schemaDirectory("$projectDir/schemas")`, configurado en el convention plugin.
- **Tests JVM (1.4)**: la variante Android de `sqlite-bundled` no encuentra `sqliteJni` fuera del dispositivo. En el classpath de test se excluye `sqlite-bundled-android` y se añade `sqlite-bundled-jvm` 2.7.1, que trae `natives/windows_x64/sqliteJni.dll`. Producción sigue usando `sqlite-bundled`.
- **KSP para tests**: los tests de Room necesitan `kspTest` además de `ksp`. Está en el convention plugin.
- **Serialization (1.5)**: el plugin compila con el Kotlin integrado de AGP 9, probado con una clase `@Serializable` y un test de ida y vuelta. Ambos spikes se eliminaron tras pasar.
- **Entorno**: Gradle no valida el certificado TLS de `dl.google.com` con el truststore del JDK (PKIX). Para resolver las dependencias de Google se usa `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`, que usa el almacén del sistema. No se ha modificado ningún fichero de configuración del JDK ni del daemon (ADR-0011).

## Evolución prevista

- **Ajuste de variante (ADR-0003):** la variante activa como ajuste del usuario queda diferida. En el MVP no es seleccionable; el locale de la raíz del catálogo es la única fuente de locales en este cambio. ADR-0013 lo recoge.
- **Variantes de idioma (pt-BR u otras):** lo más probable es un fichero de catálogo propio por variante, cargado por el mismo `AssetsCatalogDataSource`. Si hiciera falta un locale por frase, un campo opcional `targetLocale`/`sourceLocale` en cada frase sería compatible hacia atrás: las frases sin el campo heredan la raíz. Este cambio no lo implementa.

## Migration Plan

No hay datos previos. Primera instalación: base de datos vacía, ajustes por defecto, primera tanda compuesta con `ObtainActiveBatch`. Rollback: revertir la rama.

## Open Questions

Ninguna. Las decisiones pendientes del explore están resueltas en D1–D11.
