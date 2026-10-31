# Tasks

## 1. Andamiaje y verificación de versiones

- [ ] 1.1 (BLOQUEADA: el JAR del plugin KSP 2.3.13 no está publicado en Gradle Plugin Portal; se mantiene 2.3.11, ver design.md) Subir KSP de 2.3.11 a 2.3.13 en `gradle/libs.versions.toml` en un commit propio (`build(deps): upgrade KSP to 2.3.13`). Justificación: última versión publicada en Maven Central; la tarea 1.3 confirma si Room 3 la requiere, y si no, se revierte a 2.3.11. Verificar en todo el proyecto con `./gradlew detekt check koverVerify assembleDebug --rerun-tasks` en verde antes del commit.
- [x] 1.2 Añadir a `gradle/libs.versions.toml` Room 3.0.3, `sqlite-bundled` 2.7.1, DataStore Preferences 1.3.0, `kotlinx-serialization-json` 1.12.0, el plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21 y el plugin `androidx.room3`; verificar con `./gradlew help` que la configuración resuelve sin errores.
- [x] 1.3 Confirmar con la documentación oficial de Room 3 la API de transacción de escritura (`withWriteTransaction` o equivalente), la transacción IMMEDIATE, `@ColumnTypeConverter`, `schemaDirectory`, la firma de `createPracticeDatabase` y si Room 3 requiere KSP 2.3.13; registrar en `design.md` lo confirmado. Verificar con la lectura de la documentación, sin código.
- [x] 1.4 Spike de tests JVM: un test de `:core:data` que abra una base Room 3 en memoria con `BundledSQLiteDriver` y ejecute una consulta; verificar que pasa con `./gradlew :core:data:testDebugUnitTest`. Si el driver nativo no carga, parar y notificarlo antes de continuar. Este spike es código desechable y no entra en producción.
- [x] 1.5 Compilar `:core:data` con el plugin de serialization aplicado sin `org.jetbrains.kotlin.android` (Kotlin integrado de AGP 9) y un `@Serializable` de prueba desechable; verificar con `./gradlew :core:data:assembleDebug` y registrar el minSdk y el AGP verificados en `design.md`.

## 2. Documentación y gobierno (ADR y reglas)

- [x] 2.1 Crear `docs/adr/0013-*.md` con estado `Propuesto`, que sustituye a ADR-0004. Debe justificar: la excepción de detekt (`ignoreAnnotated: ['Database']`), el umbral de cobertura de `:core:data` (80 %) con la exclusión del código generado de Room en Kover, y la excepción a la regla de ADR-0001 ("un ADR aceptado no se edita"), que queda como única edición permitida: el cambio de su línea de estado a `Sustituido por ADR-XXXX` al aceptar el ADR que lo sustituye. Debe indicar también que el ajuste de variante de ADR-0003 queda diferido (no seleccionable en el MVP). Verificar que contiene Contexto, Decisión, Consecuencias y Descartado, y que indica qué de ADR-0004 se mantiene (revisión manual del catálogo, sin cuentas ni sincronización, sin IA en ejecución).
- [x] 2.2 Añadir la fila de ADR-0013 (`Propuesto`) en la tabla de `docs/adr/README.md`, y añadir a su introducción ("Cada ADR es inmutable una vez aceptado") la excepción de la línea de estado; ADR-0004 queda `Aceptado` hasta el cierre del cambio. Verificar con `git diff`. La plantilla ya contempla `Propuesto`, así que no se modifica.
- [x] 2.3 Actualizar en `CLAUDE.md`, sección "No hacer", la regla de ADR: un ADR `Propuesto` puede editarse; un ADR `Aceptado` solo admite el cambio de su línea de estado a `Sustituido por ADR-XXXX` cuando un ADR nuevo lo sustituye. Verificar que la sección lo contiene y que el resto de la regla sobre `openspec/specs/` se mantiene.
- [x] 2.4 Añadir en `CLAUDE.md`, sección "No hacer", las reglas de ampliación del catálogo (ids permanentes, sin reutilización ni borrado, alta al final); verificar que la sección lo contiene.
- [x] 2.5 Actualizar `openspec/config.yaml`: sustituir "catálogo de frases precargado" por el catálogo JSON en assets; añadir referencias a ADR-0003, ADR-0013 (sustituye a ADR-0004), ADR-0010, ADR-0011 y ADR-0012; y cambiar "JVM toolchain 17" por "daemon de Gradle en JDK 21 (ADR-0011)". Verificar con `openspec validate add-persistence --strict` y leyendo el fichero.
- [x] 2.6 Añadir `ignoreAnnotated: ['Database']` a `UnnecessaryAbstractClass` en `config/detekt/detekt.yml` sin `@Suppress`; verificar con `./gradlew detekt` en verde.

## 3. Dominio: catálogo y value classes

- [x] 3.1 Escribir tests de `Topic` y `LanguageTag` que rechacen valores vacíos y formatos no BCP-47 según `phrase-catalog` (Requisitos "Campos obligatorios de cada frase" y "Locales por frase"); verificar que fallan por el motivo esperado.
- [x] 3.2 Implementar `Topic` y `LanguageTag` como value classes con `require` de no vacío y de formato BCP-47 en `LanguageTag`; verificar que los tests de 3.1 pasan.
- [x] 3.3 Escribir tests de `CatalogPhrase` ampliado con `topic`, `targetLocale`, `sourceLocale`, `target` y `source`; verificar que fallan.
- [x] 3.4 Ampliar `CatalogPhrase` con los campos nuevos y actualizar `aCatalogPhrase()` en `:core:testing` con defaults; verificar que `./gradlew :core:domain:test` pasa, incluido `ComposeNextBatchTest` sin cambios.

## 4. Dominio: puertos, fakes y casos de uso

- [x] 4.0 Crear en `:core:testing` los fakes en memoria `FakeClock`, `FakeCatalogRepository`, `FakeSettingsRepository` y `FakePracticeRepository` (este último con `Mutex` en `transact` y `read`). Verificar con un test de uso por fake que `transact` aplica el bloque y devuelve su resultado, y que `FakeClock` devuelve la fecha fijada; el test se escribe antes que el fake y falla por la falta de la clase.
- [x] 4.1 Declarar en `:core:domain` las interfaces `CatalogRepository`, `SettingsRepository` y `PracticeRepository` (con `read` y `transact`), los tipos `PracticeSnapshot`, `PracticeChange`, `ActiveBatch` y el sealed `CatalogLoadError` (`UnsupportedSchemaVersion`, `MissingSchemaVersion`, `InvalidLocale`, `InvalidPhrase`, `DuplicateId`, `AssetUnreadable`). Es una declaración de puerto sin test propio; verificar con `./gradlew :core:domain:compileKotlin`.
- [x] 4.2 Escribir tests de `ObtainActiveBatch` con `FakePracticeRepository` que cubran "Existing", "Created", "NothingAvailable" y "Primer uso crea la primera tanda" según `practice-batch` y `practice-persistence`, y los de "Cambio de fecha con tanda activa" con `FakeClock`; verificar que fallan porque `ObtainActiveBatch` aún no existe.
- [x] 4.3 Implementar `ObtainActiveBatch` con `ComposeNextBatch` dentro de `transact`; verificar que los tests de 4.2 pasan.
- [x] 4.4 Escribir tests de `AnswerPhrase` que cubran "Acierto mueve la frase al final" de `practice-batch` y la colocación al final con `max(batch_position)+1`; verificar que fallan porque `AnswerPhrase` aún no existe.
- [x] 4.5 Implementar `AnswerPhrase` dentro de `transact`; verificar que los tests de 4.4 pasan.
- [ ] 4.6 Escribir tests de `CloseActiveBatch` que cubran `Closed`, `Stale`, `NoActiveBatch`, "Sin tanda activa tiene prioridad sobre la expectativa", "Cierre sin composición posible deja la tanda sin activar" y "El aviso coincide con las arrastradas del cierre"; verificar que fallan porque `CloseActiveBatch` aún no existe.
- [ ] 4.7 Implementar `CloseActiveBatch` con `ResolveBatchClosure`, `CarryQueue.afterClosure` y `ComposeNextBatch` dentro de `transact`, con la fecha de cierre capturada antes de la transacción y el orden de comprobación de D4; verificar que los tests de 4.6 pasan.
- [x] 4.8 Verificar cobertura del dominio con `./gradlew :core:domain:koverVerify` (mínimo 90 %).

## 5. Platform: reloj

- [x] 5.1 Escribir un test de `DeviceClock` con una zona inyectada que compruebe que `today()` devuelve la fecha local de esa zona; verificar que falla.
- [x] 5.2 Implementar `DeviceClock : Clock` en `:core:platform` con `LocalDate.now(zone)`; verificar que el test de 5.1 pasa con `./gradlew :core:platform:test`.

## 6. Data: catálogo JSON

- [x] 6.1 Escribir tests de `CatalogJson.parse` que cubran "Formato del catálogo", "Locales por frase", "Campos obligatorios de cada frase", "Versión del esquema ausente", "Ids duplicados rechazan la carga" e "Id vacío rechaza la carga" de `phrase-catalog`, incluidos el rechazo de `schemaVersion` 2 y de CEFR inválido; verificar que fallan porque `CatalogJson` aún no existe.
- [x] 6.2 Implementar `CatalogJson.parse` con `kotlinx.serialization` que copie los locales de la raíz a cada frase, valide formato BCP-47 y devuelva `CatalogLoadError` tipados; verificar que los tests de 6.1 pasan.
- [x] 6.3 Crear el fichero de referencia `core/data/src/test/resources/catalog/released-ids.txt` con los ids publicados (en esta primera versión, los dos ids de ejemplo de 6.4). Escribir los tests de contrato sobre fixtures de catálogo que cubran "Ids publicados siguen presentes y en el mismo orden", "Alta de frase nueva al final" y "Catálogo que quita un id publicado devuelve el id retirado". La comprobación de contrato devuelve el id retirado; verificar que los tests fallan porque la comprobación aún no existe, no porque falte el fichero.
- [x] 6.4 Implementar la comprobación de contrato sobre el catálogo; verificar que los tests de 6.3 pasan. Crear `core/data/src/main/assets/catalog/phrases.json` con `schemaVersion: 1`, locales en la raíz y dos frases de ejemplo de nivel A1 con topic, de modo que sus ids coincidan con `released-ids.txt`; verificar que el contrato pasa sobre el catálogo real. Todos los tests terminan en verde.
- [x] 6.5 Escribir el test de integración de `AssetsCatalogDataSource`, que recibe un lector inyectado, que devuelve las frases en orden de catálogo, y el de "Asset no legible falla la carga" con un lector que lanza error (`AssetUnreadable`); verificar que fallan.
- [x] 6.6 Implementar `AssetsCatalogDataSource : CatalogRepository`, con el lector y el dispatcher inyectados, que lea el asset y delegue en `CatalogJson.parse`; verificar que el test de 6.5 pasa.

## 7. Data: ajustes

- [x] 7.1 Escribir tests de `DataStoreSettingsDataSource` con un fichero temporal que cubran "Valores por defecto", "Persistencia de los ajustes", "N guardado como entero se lee correctamente", los fallbacks de N ("N guardado como texto no entero usa el valor por defecto" y el resto de escenarios de rango), el fallback del nivel CEFR y "Lectura de un almacén ilegible"; verificar que fallan por el motivo esperado.
- [x] 7.2 Implementar `DataStoreSettingsDataSource : SettingsRepository` con claves `stringPreferencesKey`, `toIntOrNull`, comprobación de rango y `emptyPreferences()` ante `IOException`, con el dispatcher inyectado; verificar que los tests de 7.1 pasan.

## 8. Data: Room y estado de práctica

- [x] 8.1 Escribir tests de DAO en Room 3 con `BundledSQLiteDriver` que comprueben los CHECK y los UNIQUE de `phrase_progress` y `active_batch` (ambas posiciones a la vez, posición repetida, nivel 0 sin posición, capacidad fuera de 10..30, fila única), con la excepción esperada indicada en cada test. Incluir también los tests de caracterización del esquema "Frase nueva de la tanda activa es válida", "Frase de nivel 3 arrastrada sin responder es válida" y "Frase de nivel 0 fuera de tanda y cola se rechaza", que fallan porque el esquema aún no existe; verificar que todos fallan.
- [x] 8.2 Definir entidades, DAOs y la base de datos Room 3 versión 1 con los CHECK y UNIQUE del design (D5), `schemaDirectory` configurado y la factoría `createPracticeDatabase` en `:core:data`; verificar que los tests de 8.1 pasan y que se genera `core/data/schemas/`. Crear también el stub `RoomPracticeDataSource` con `read()` y `transact()` que lanzan `TODO()` (`NotImplementedError`), para que los tests de 8.3 a 8.5 y 8.7 fallen por ese motivo.
- [x] 8.3 Escribir tests de `RoomPracticeDataSource.read` que cubran "Persistencia del progreso por frase", "Orden de la tanda activa", "Sin tanda activa" y "Lectura coherente del estado" (incluido el test de concurrencia de lectura durante un cierre); verificar que fallan con `NotImplementedError` del stub.
- [x] 8.4 Escribir el test de "Cierre con reasignación cruzada de posiciones" y del intercambio de dos posiciones de tanda; verificar que falla con `NotImplementedError` del stub.
- [x] 8.5 Escribir el test de "Fallo a mitad del cierre no deja estado parcial" con el mecanismo de fallo inyectado que se fija aquí: un wrapper del DAO que lanza un error tras la k-ésima escritura. Comprobar que el estado es idéntico al anterior al cierre; verificar que falla con `NotImplementedError` del stub.
- [ ] 8.6 (BLOQUEADA: depende de la decisión sobre el cierre con composición, ver conflicto de 4.6/4.7) Escribir el test de integración del ciclo completo de dos tandas (obtener, responder, cerrar con el número del aviso, componer la siguiente) con persistencia real en Room, según "La cola sobrante entra primera tras el cierre" de `practice-batch`; verificar que falla con `NotImplementedError` del stub.
- [ ] 8.7 (implementado; la verificación de 8.6 queda pendiente) Implementar `RoomPracticeDataSource.read` y `transact` con transacción IMMEDIATE, escritura en dos fases (D3) y escritura solo de las filas que cambian; verificar que los tests de 8.3, 8.4, 8.5 y 8.6 pasan, y que los de 8.1 siguen en verde.
- [x] 8.8 Ejecutar `./gradlew :core:data:koverVerify` con las exclusiones de código generado de Room añadidas en `build-logic` y el umbral de 80 % en `core/data/build.gradle.kts`.

## 9. App: inyección

- [ ] 9.1 (SIN TEST PROPIO: el requisito "implementa su puerto" se comprueba en compilación, y las fuentes ya se construyen a mano en 7.1 y 8.3; pendiente de decisión) Escribir un test JVM que construya `RoomPracticeDataSource` y `DataStoreSettingsDataSource` a mano, con base en memoria (`BundledSQLiteDriver`) y un fichero temporal, y compruebe que cada uno implementa su puerto del dominio; verificar que falla.
- [x] 9.2 Añadir los módulos `@Binds` de `:app` y el proveedor de la base de datos con `createPracticeDatabase` y `BundledSQLiteDriver`; verificar que el test de 9.1 pasa y que `./gradlew assembleDebug` compila, lo que valida el grafo de Hilt en compilación.

## 10. Calidad

- [x] 10.1 Ejecutar `./gradlew check` y `./gradlew koverVerify` en verde.

## 11. Cierre de ADR

- [ ] 11.1 (PENDIENTE: depende de cerrar 4.6, 4.7 y 8.6) Pasar ADR-0013 a `Aceptado`, cambiar la línea de estado de ADR-0004 a `Sustituido por [ADR-0013](0013-*.md)` y actualizar la tabla de `docs/adr/README.md`, todo en un único commit (`docs(adr): accept ADR-0013 and supersede ADR-0004`); verificar con `git show --stat` que solo cambian esos ficheros y con `git diff HEAD~1 -- docs/adr/0004-*.md` que solo cambia la línea de estado.

## Workflow follow-up

- Revisar el código con el subagente `code-reviewer` antes de abrir la pull request.
- Ejecutar `/opsx:verify` contra la especificación antes de archivar.
- Hacer push de la rama actual con `git push -u origin HEAD` y parar; el usuario abre la pull request a `develop`.
- Archivar el cambio con `/opsx:archive` tras la revisión y la CI.
