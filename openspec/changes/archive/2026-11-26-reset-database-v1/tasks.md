# Tasks

## 1. :core:data: bases a la versión 1 (tdd-implementer, haiku)

> Nota: este grupo no lleva un test rojo previo. Quita un escenario y una configuración sin añadir comportamiento
> (design D2, "Tests"). La red de seguridad son los tests de Room que ya existen, más la comprobación del
> `identityHash` de la tarea 1.4.

- [x] 1.1 Borrar `core/data/src/test/kotlin/com/dalmuina/fraseafrase/core/data/practice/PracticeMigrationTest.kt`.
  Cubría el escenario eliminado "Una base anterior se abre vacía" (`specs/practice-persistence/spec.md`, REMOVED).
  Ajustar el comentario de `RoomPracticeDataSourceReadTest.kt:129` para que no cite el fallback como precedente
  (design D5). Verificar con Grep que ningún test cita ya "Una base anterior se abre vacía" ni
  `fallbackToDestructiveMigration`.
- [x] 1.2 `PracticeDatabase`: `version = 1` y KDoc reescrito según design D5 (ADR-0030, que retoma la versión inicial 1
  del ADR-0013, ya sustituido; versión 1 = 1.0.0). `PracticeSchema.createPracticeDatabase`: quitar
  `.fallbackToDestructiveMigration()` y su comentario, y citar el ADR-0030 (design D2). Ajustar el KDoc de
  `PracticeSchema.kt:15-17` para que no cite "`PracticeDatabase` v4" (design D5). Verificar con Grep que `core/data/src/main/**/practice/` no contiene
  `fallbackToDestructiveMigration`.
- [x] 1.3 `CatalogDatabase`: `version = 1`. `CatalogSchema.createCatalogDatabase`: conservar
  `.fallbackToDestructiveMigration()` y cambiar su comentario a la justificación de caché sembrada desde assets
  (ADR-0028, ADR-0030; design D3). Verificar leyendo el diff.
- [x] 1.4 Los `identityHash` de referencia son `f083de87fdfc91362f55b8e4eeb10200` (catálogo, `4.json`) y
  `97f06cba0448b30e84a80bbbc1289a6a` (práctica, `5.json`). Borrar todos los `*.json` de `core/data/schemas/com.dalmuina.fraseafrase.core.data.catalog.CatalogDatabase/` y de
  `core/data/schemas/com.dalmuina.fraseafrase.core.data.practice.PracticeDatabase/`, y compilar `:core:data` para
  regenerarlos (design D1). Verificar que cada carpeta tiene solo `1.json`, con `"version": 1`, y que su
  `identityHash` coincide con el de referencia.
- [x] 1.5 `./gradlew :core:data:check` en verde.

## 2. Documentación (sesión principal)

- [x] 2.1 Aceptar ADR-0030: cambiar su estado a `Aceptado` en el ADR y en `docs/adr/README.md`. Verificar que el índice
  y el ADR dicen lo mismo.
- [x] 2.2 `CLAUDE.md`: añadir "Migraciones de Room: ADR-0030" a la regla de oro 4 y, en "No hacer", una entrada:
  no volver a añadir `fallbackToDestructiveMigration()` a `practice.db`; cada subida de versión lleva su migración y
  su test (ADR-0030). Verificar que las referencias apuntan a ficheros que existen.

## 3. Verificación (sesión principal)

- [x] 3.1 `./gradlew check` y `./gradlew koverVerify` en verde.

## Workflow follow-up

- Tras el merge, desinstalar la app o borrar sus datos en los dispositivos de desarrollo (design, "Migration Plan").
- `/opsx:archive` en la rama del cambio, antes de la PR.
