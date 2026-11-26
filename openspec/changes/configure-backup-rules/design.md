# Design

## Context

- `app/src/main/AndroidManifest.xml` declara `android:allowBackup="true"` y ninguna regla: Auto Backup (Android 6+) y la
  transferencia entre dispositivos (Android 12+) copian todo el almacenamiento interno.
- Ficheros de la app (rutas relativas al dominio de Auto Backup):
  - `database`: `catalog.db` (`CatalogSchema.kt`) y `practice.db` (`PracticeSchema.kt`), cada una con sus ficheros
    auxiliares de SQLite (`-wal`, `-shm` y, si se usa el modo de diario por rollback, `-journal`).
  - `file`: `datastore/settings.preferences_pb` (`preferencesDataStore(name = "settings")` en
    `DataStoreSettingsDataSource.kt`).
- `RoomCatalogDataSource.ensureSeeded()` siembra cada idioma cuya huella no coincide con la guardada en
  `catalog_seed_version`. Sin `catalog.db`, no hay huella y siembra todo: ya cubre la restauración sin catálogo.
- minSdk 26 (ADR-0008): Android 8-11 usa `android:fullBackupContent`; Android 12+ usa `android:dataExtractionRules` e
  ignora el anterior.

## Goals / Non-Goals

**Goals:** excluir `catalog.db*` de toda copia; copiar `practice.db*` y los ajustes juntos; fijarlo con tests JVM.

**Non-Goals:** cambiar código de `:core:*`; copia propia o exportación; tratar copias con ids desconocidos (A11) o con
una versión de `practice.db` más nueva que la instalada (ADR-0030).

## Decisions

### D1. Solo reglas de exclusión, sin `<include>`

En ambos formatos, la presencia de un `<include>` convierte la copia en una lista cerrada: todo lo no incluido queda
fuera. Usar solo `<exclude>` deja dentro, por defecto, `practice.db*` y `datastore/`, y cualquier fichero futuro de
estado del usuario entra sin tocar las reglas. Alternativa descartada: incluir explícitamente `practice.db*` y
`datastore/settings.preferences_pb`; es más frágil (un fichero nuevo de estado quedaría fuera sin aviso) y no aporta
nada a "copiarlos juntos", que ya garantiza la copia, porque Auto Backup restaura la instantánea completa de una vez.

### D2. Ficheros excluidos

`domain="database"` con `path` = `catalog.db`, `catalog.db-wal`, `catalog.db-shm`, `catalog.db-journal`. Se excluyen
los cuatro aunque hoy Room use WAL (no `-journal`): una base restaurada a medias (`-wal` sin su `.db`, o al revés) es
peor que ninguna. Se escriben como `<exclude>` de fichero, no de carpeta, para no tocar `practice.db*` en el mismo
dominio.

### D3. Ficheros de reglas

- `app/src/main/res/xml/data_extraction_rules.xml`: `<data-extraction-rules>` con `<cloud-backup>` y
  `<device-transfer>`, ambos con las cuatro exclusiones de D2. Sin `disableIfNoEncryptionCapabilities` (valor por
  defecto): el progreso no es sensible y perder la copia en dispositivos sin cifrado sería peor.
- `app/src/main/res/xml/backup_rules.xml`: `<full-backup-content>` con las cuatro exclusiones de D2.
- Manifiesto: `android:allowBackup="true"` (se mantiene), `android:dataExtractionRules="@xml/data_extraction_rules"`
  con `tools:targetApi="31"` (el atributo no existe por debajo de API 31 y lint avisaría con `UnusedAttribute`) y
  `android:fullBackupContent="@xml/backup_rules"`. Se añade `xmlns:tools` al `<manifest>`.

Todo vive en `:app`, el único módulo con manifiesto de aplicación (ADR-0010). Las rutas `catalog.db` y `practice.db`
se repiten como literales en los XML: los recursos no pueden leer constantes de Kotlin, y el test de D4 detecta un
desajuste porque compara con los mismos nombres.

### D4. Estrategia de test

- `app/src/test/kotlin/com/dalmuina/fraseafrase/BackupRulesTest.kt` (JVM, JUnit 6 + Kotest): lee
  `src/main/res/xml/data_extraction_rules.xml`, `src/main/res/xml/backup_rules.xml` y `src/main/AndroidManifest.xml`
  desde el directorio del módulo (el directorio de trabajo de los tests unitarios de Gradle) con
  `javax.xml.parsers.DocumentBuilderFactory` del JDK (sin librerías nuevas), con espacios de nombres activados para leer
  los atributos `android:`; antes de parsear, comprueba que el fichero existe con un `withClue` explícito, para que el
  rojo inicial diga qué fichero falta. Un test por escenario de `data-backup` de los dos primeros requisitos:
  - exclusiones de `catalog.db*` en `cloud-backup`, `device-transfer` y `full-backup-content`;
  - manifiesto con `allowBackup="true"`, `dataExtractionRules="@xml/data_extraction_rules"` y
    `fullBackupContent="@xml/backup_rules"`;
  - ningún `<exclude>` cuyo `path` empiece por `practice.db` ni por `datastore` (o sea vacío/`.` en el dominio `file`);
  - ningún `<include>` en ninguna de las tres secciones.
- Escenario "Primer arranque tras restaurar": test en
  `core/data/src/test/kotlin/com/dalmuina/fraseafrase/core/data/catalog/RoomCatalogDataSourceTest.kt`, junto a
  `given saved progress when re-seeding its language then the progress is unchanged`, con una base de práctica con
  progreso y una base de catálogo vacía (`inMemoryCatalogDatabase()`, `inMemoryPracticeDatabase()`). Es un **test de
  caracterización**: el comportamiento ya existe y no hay código de producción que cambiar, así que se espera verde a
  la primera; para comprobar que el test detecta un fallo, se verifica que falla si se comenta la llamada a
  `ensureSeeded()` en `phrases(...)` (no la de `availableTargetLocales()`) y se restaura después (sin commitear la mutación).
- Comprobación manual opcional en dispositivo (no automatizable en CI): `adb shell bmgr backupnow <applicationId>`,
  desinstalar, reinstalar y `adb shell bmgr restore`; el progreso y los ajustes vuelven y el catálogo se siembra.

### D5. ADR-0031

Nuevo ADR `docs/adr/0031-politica-de-copia-de-seguridad.md` (no sustituye a ninguno): qué entra en la copia y por qué
el progreso y los ajustes van juntos. Se escribe como `Propuesto` en este propose y pasa a `Aceptado` en la sección de
documentación de `tasks.md`.

## Risks / Trade-offs

- **Restaurar progreso con ids que no están en el catálogo** → excepción en `ComposeNextBatch`. Mitigación: A11 congela
  los ids antes de publicar; fuera de alcance aquí.
- **Ficheros futuros de estado entran en la copia sin decisión explícita** (consecuencia de D1) → se acepta: es lo
  deseable para estado del usuario; una caché nueva debe añadirse a las exclusiones, y ADR-0031 lo deja escrito.
- **Los tests leen los XML de fuente, no el APK fusionado** → un manifiesto de librería podría añadir reglas en la
  fusión. Ninguna dependencia actual declara `dataExtractionRules` ni `fullBackupContent`; se acepta.
