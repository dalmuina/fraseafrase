# Tasks

## 1. :app: reglas de copia de seguridad (tdd-implementer, haiku)

- [x] 1.1 Test rojo: crear `app/src/test/kotlin/com/dalmuina/fraseafrase/BackupRulesTest.kt` (design D4) con un test
  por escenario de `specs/data-backup/spec.md`: "Copia en la nube en Android 12 o superior", "Transferencia entre
  dispositivos en Android 12 o superior", "Copia completa en Android 8 a 11", "El manifiesto declara las reglas",
  "Ninguna regla excluye el progreso", "Ninguna regla excluye los ajustes" y "Las reglas no restringen la copia a una
  lista de inclusión". Leer los XML con `DocumentBuilderFactory` del JDK (con espacios de nombres), comprobando antes
  que cada fichero existe con un `withClue` explícito (p. ej. "falta src/main/res/xml/backup_rules.xml"). Ejecutar
  `./gradlew :app:test -q --console=plain` y comprobar que falla porque faltan los ficheros de reglas y los atributos
  del manifiesto, no por un error de compilación ni de ruta.
- [x] 1.2 Crear `app/src/main/res/xml/data_extraction_rules.xml` y `app/src/main/res/xml/backup_rules.xml` con las
  exclusiones de `catalog.db`, `catalog.db-wal`, `catalog.db-shm` y `catalog.db-journal` en el dominio `database`,
  sin `<include>` (design D1, D2, D3). Escenarios: los tres primeros y los tres últimos de 1.1.
- [x] 1.3 `app/src/main/AndroidManifest.xml`: mantener `android:allowBackup="true"` y añadir
  `android:dataExtractionRules="@xml/data_extraction_rules"` con `tools:targetApi="31"`,
  `android:fullBackupContent="@xml/backup_rules"` y `xmlns:tools` (design D3). Escenario: "El manifiesto declara las
  reglas". `BackupRulesTest` en verde.
- [x] 1.4 `./gradlew :app:check -q --console=plain` en verde, sin avisos nuevos de lint sobre las reglas de copia
  (`DataExtractionRules`, `FullBackupContent`, `UnusedAttribute`).

## 2. :core:data: restauración sin catálogo (tdd-implementer, haiku)

> Nota: test de caracterización (design D4). El comportamiento ya existe y no hay código de producción que cambiar,
> así que no hay rojo previo; la prueba de que el test detecta el fallo es la mutación de la tarea 2.2.

- [x] 2.1 En `core/data/src/test/kotlin/com/dalmuina/fraseafrase/core/data/catalog/RoomCatalogDataSourceTest.kt`,
  añadir `given saved progress and no catalog when reading phrases then the catalog is seeded and progress is
  kept`: base de práctica con un `PhraseProgressEntity` y base de catálogo vacía; al pedir frases, el resultado es
  `Loaded` con las frases de los documentos y el progreso no cambia. Escenario: `specs/data-backup/spec.md` →
  "Primer arranque tras restaurar".
- [x] 2.2 Mutación: comentar la llamada a `ensureSeeded()` dentro de `phrases(...)` (`RoomCatalogDataSource.kt:56`; no
  la de `availableTargetLocales()`, línea 50), comprobar que el test de 2.1 falla, restaurar la línea y comprobar que
  vuelve a verde. No commitear la mutación. Anotar aquí la evidencia (única prueba del escenario) con el formato
  "Evidencia: fallo observado: <mensaje> / verde tras restaurar". `./gradlew :core:data:check -q --console=plain` en verde.
  Evidencia: fallo observado: `org.opentest4j.AssertionFailedError: Missing elements from index 0; expected:<["Bom dia"]> but was:<[]>` / verde tras restaurar (clase `RoomCatalogDataSourceTest` en verde).

## 3. Documentación (sesión principal)

- [x] 3.1 Aceptar ADR-0031: cambiar su estado a `Aceptado` en el ADR y en `docs/adr/README.md`. Verificar que el índice
  y el ADR dicen lo mismo.
- [x] 3.2 `CLAUDE.md`: añadir "Copia de seguridad: ADR-0031" a la regla de oro 4 tras "Migraciones de Room: ADR-0030".

## 4. Verificación (sesión principal)

- [x] 4.1 `./gradlew check` y `./gradlew koverVerify` en verde.

## Workflow follow-up

- Opcional, en dispositivo: comprobación manual con `bmgr` (design D4).
- `/opsx:archive` en la rama del cambio, antes de la PR.
