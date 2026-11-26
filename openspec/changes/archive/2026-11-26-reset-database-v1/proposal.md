# Proposal

## Why

Antes de publicar 1.0.0, `CatalogDatabase` está en la versión 4 y `PracticeDatabase` en la 5, con esquemas
intermedios que ninguna instalación real tendrá nunca. Además, las dos bases usan `fallbackToDestructiveMigration()`.
En `practice.db` eso significa que, en cuanto haya usuarios, una subida de versión sin migración borraría su progreso
sin avisar. Es el último momento para empezar en la versión 1 y fijar la política de migraciones (A3 de
`docs/plan-mvp.md`).

## What Changes

- **BREAKING (solo desarrollo):** `CatalogDatabase` y `PracticeDatabase` vuelven a `version = 1`. Se borran los
  esquemas exportados `2.json` a `N.json` de cada base y se regenera `1.json` con el esquema actual. Las instalaciones
  de desarrollo con una base de una versión superior tienen que desinstalar la app o borrar sus datos.
- `practice.db` deja de usar `fallbackToDestructiveMigration()`. A partir de 1.0.0, cada subida de versión necesita
  una migración que conserve el progreso.
- `catalog.db` mantiene `fallbackToDestructiveMigration()`. Es una caché que se vuelve a sembrar desde los assets
  (ADR-0028), así que recrearla no pierde nada del usuario. Se actualiza su comentario, que hoy se justifica por ser
  "pre-1.0.0".
- Se elimina `PracticeMigrationTest`, que caracteriza la migración destructiva de v3 y v4, y el requisito
  "Descarte del estado guardado con un esquema anterior" de `practice-persistence`.
- Nuevo **ADR-0030, "Política de migraciones de las bases Room"**:
  - `practice.db` nunca migra de forma destructiva.
  - Cada subida usa `@AutoMigration`, o `Migration` manual si Room no puede deducirla, con un test de
    `MigrationTestHelper` contra los esquemas exportados.
  - `catalog.db` puede recrearse.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `practice-persistence`: se elimina el requisito "Descarte del estado guardado con un esquema anterior" y su
  escenario "Una base anterior se abre vacía".

## Impact

- **Código (`:core:data`):** `PracticeDatabase.kt`, `PracticeSchema.kt`, `CatalogDatabase.kt` y `CatalogSchema.kt`
  (versión, KDoc, fallback y comentario).
- **Esquemas:** `core/data/schemas/...CatalogDatabase/{2,3,4}.json` y `...PracticeDatabase/{2,3,4,5}.json` se borran;
  los dos `1.json` se regeneran.
- **Tests:** se borra `PracticeMigrationTest.kt`. Se ajusta el comentario de `RoomPracticeDataSourceReadTest.kt:129`,
  que cita el fallback como precedente.
- **Documentación:** ADR-0030 nuevo, índice de `docs/adr/README.md` y regla de oro 4 de `CLAUDE.md`.
- **Dependencias:** ninguna. `room3-testing` se añadirá con la primera migración real, no ahora.
- **ADR relacionados:**
  - Vuelve a lo que fijaba ADR-0013: "versión inicial 1, sin `fallbackToDestructiveMigration`".
  - ADR-0017, 0019, 0022, 0023 y 0027 aplicaron migraciones destructivas como consecuencia de estar en pre-1.0.0.
    Son historia y no se tocan.
  - ADR-0030 no sustituye a ninguno: añade una política que no existía para después de 1.0.0.
- **Alcance del MVP (ADR-0018):** no cambia ninguna regla de la práctica ni lo que ve el usuario.

## Fuera de alcance

- Copia de seguridad de `catalog.db`, `practice.db` y ajustes (`allowBackup`): es A4, `configure-backup-rules`.
- Montar ya las herramientas de prueba de migraciones (`room3-testing`, `MigrationTestHelper`) o un test que exija una
  migración por versión: se harán en la primera subida real de `practice.db`.
- Un escenario que compruebe que una versión sin migración falla en lugar de borrar los datos. La política queda en el
  ADR-0030 y la revisión de código la vigila.
- Cambiar el esquema de alguna tabla: el `1.json` nuevo tiene exactamente el esquema actual.
