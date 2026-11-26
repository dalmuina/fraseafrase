# Proposal

## Why

`AndroidManifest.xml` declara `android:allowBackup="true"` sin reglas, así que Auto Backup y la transferencia entre
dispositivos copian todo el almacenamiento interno, incluido `catalog.db`. Ese catálogo es una caché recreable desde los
assets (ADR-0028, ADR-0030) cuya huella vive dentro del propio fichero: restaurarlo solo aporta riesgo (un catálogo o un
esquema que no corresponde al APK instalado) y gasta cuota de copia. A la vez, el progreso (`practice.db`) y los ajustes
son lo único que el usuario no puede recuperar, y deben viajar juntos porque el progreso es por idioma destino y ese
idioma está en los ajustes. Es la fila A4 del plan de la MVP1 y debe quedar resuelta antes de publicar 1.0.0.

## What Changes

- Reglas de copia de seguridad para Android 12+ (`dataExtractionRules`: copia en la nube y transferencia entre
  dispositivos) y Android 8-11 (`fullBackupContent`), declaradas en el manifiesto de `:app`.
- `catalog.db` y sus ficheros auxiliares de SQLite (`-wal`, `-shm`, `-journal`) quedan fuera de toda copia.
- `practice.db` con sus ficheros auxiliares y el almacén de ajustes (`datastore/settings.preferences_pb`) se copian y se
  restauran juntos. No se añade ninguna exclusión que pueda separarlos.
- Tras restaurar, el catálogo se siembra desde los assets en el primer uso, como en una instalación nueva (sin cambio de
  código: lo hace ya `RoomCatalogDataSource`).
- ADR-0031 (nuevo, no sustituye a ninguno): política de copia de seguridad.

## Capabilities

### New Capabilities
- `data-backup`: qué datos de la app entran en la copia de seguridad y la transferencia entre dispositivos, y qué
  ocurre al restaurarlos.

### Modified Capabilities
(ninguna)

## Impact

- `:app`: `src/main/AndroidManifest.xml`, `src/main/res/xml/data_extraction_rules.xml`,
  `src/main/res/xml/backup_rules.xml` y un test JVM en `src/test`.
- ADR: nuevo ADR-0031 e índice `docs/adr/README.md`. Respeta ADR-0028 (resembrado por huella) y ADR-0030 (`catalog.db`
  es una caché; `practice.db` solo cambia con migración). Dentro del alcance del MVP (ADR-0018): no añade funcionalidad
  visible, solo protege los datos existentes.
- Sin librerías nuevas, sin cambios en `:core:*` ni en las features.

## Fuera de alcance

- Restaurar un `practice.db` con ids que el catálogo instalado no tiene: lo evita A11 (`freeze-released-ids`), que
  congela los ids antes de publicar.
- Restaurar una copia hecha con una versión de `practice.db` más nueva que la instalada (bajada de versión): ADR-0030 no
  contempla migraciones hacia atrás; con la versión 1 publicada no puede darse todavía.
- Copia manual, exportación o sincronización propia (no hay backend, ADR-0004/ADR-0013).
- Prueba manual de restauración en dispositivo con `bmgr`: se documenta como comprobación, no como test automático.
