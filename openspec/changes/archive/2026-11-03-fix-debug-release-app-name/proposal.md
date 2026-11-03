# Proposal

## Why

Las variantes `debug` y `release` de `:app` ya tienen identificadores de aplicación distintos
(`add-debug-application-id`), pero comparten el mismo nombre visible en el lanzador (`app_name`,
sin `resValue` por variante). Con ambas instaladas en el mismo dispositivo no se puede distinguir
cuál es cuál a simple vista, lo que puede llevar a practicar en la build `debug` creyendo que es
la build de uso diario. Hay que resolverlo antes de usar `debug` a diario en el móvil personal
(`docs/deuda-tecnica.md`, "Confusión visual entre variantes").

## What Changes

- Añadir un `resValue` de `app_name` por `buildType` en `app/build.gradle.kts`: la variante `debug`
  muestra un nombre distinto al de `release` (por ejemplo, con un sufijo visible como "(Debug)").
- La variante `release` conserva el nombre de publicación actual.
- Sin cambios en `applicationId`, en la lógica de dominio ni en la UI de la app.

## Capabilities

### New Capabilities

(ninguna)

### Modified Capabilities

- `build-variants`: además de identificadores de aplicación distintos, las variantes `debug` y
  `release` SHALL mostrar un nombre visible distinto en el lanzador del dispositivo.

## Impact

- `app/build.gradle.kts`: `buildTypes { debug { ... } release { ... } }`, añadiendo `resValue` de
  `string/app_name` por variante.
- `app/src/main/res/values/strings.xml`: se elimina el `app_name` base; cada variante obtiene el
  suyo por `resValue` (detalle en design.md).
- Ningún ADR queda contradicho: ADR-0012 (alcance del MVP) no se ve afectado, es una corrección de
  configuración de build. No se requiere un ADR nuevo.

## Fuera de alcance

- Firma de la build `release` (deuda técnica distinta, ya registrada).
- Migración de datos de la build `debug` antigua.
- Cualquier otro identificador visual (icono, color) más allá del nombre.
