# build-variants Specification

## Purpose

Define qué identificador de aplicación tiene cada variante de build, de modo que la build de depuración pueda instalarse junto a la de publicación sin compartir su identidad de aplicación.

## Requirements

### Requirement: La build de depuración usa un identificador propio
La variante `debug` de `:app` SHALL tener el identificador de aplicación `com.dalmuina.fraseafrase.debug`.

#### Scenario: Variante debug
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se evalúa la configuración de la variante `debug`
- **THEN** el identificador de aplicación es `com.dalmuina.fraseafrase.debug`
- **Verificación:** test JVM `BuildVariantTest` (tarea 1.1), ejecutado en `testDebugUnitTest`. AGP ejecuta los tests unitarios solo contra la variante `debug` por defecto (`testBuildType`).

### Requirement: La build de publicación conserva su identificador
La variante `release` de `:app` SHALL tener el identificador de aplicación `com.dalmuina.fraseafrase`.

#### Scenario: Variante release
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se evalúa la configuración de la variante `release`
- **THEN** el identificador de aplicación es `com.dalmuina.fraseafrase`
- **Verificación:** comprobación de configuración en `app/build.gradle.kts` (`androidComponents.onVariants` sobre `release`, D5). Se evalúa en cada configuración del proyecto y falla si el identificador no coincide. Con la caché de configuración reutilizada no se reejecuta, pero cualquier cambio en `build.gradle.kts` la invalida. Su comprobación de regresión es la mutación de la tarea 1.5, ejecutada con `--no-configuration-cache`. Es una aserción de configuración de Gradle, no un test: decisión del usuario, registrada en la descripción de la PR (D5).

### Requirement: Los identificadores de depuración y publicación son distintos
El identificador de la variante `debug` SHALL ser distinto del de la variante `release`, de modo que ambas puedan instalarse en el mismo dispositivo.

#### Scenario: Identificadores distintos
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se comparan los identificadores de las variantes `debug` y `release`
- **THEN** son distintos
- **Verificación (derivada):** se cumple si `BuildVariantTest` pasa en `testDebugUnitTest` (identificador `.debug`) y la comprobación de configuración de `release` pasa (identificador base). Además, `BuildVariantTest` afirma directamente que el identificador de `debug` no es `com.dalmuina.fraseafrase`. Cada variante fija un valor distinto.

### Requirement: Las variantes muestran un nombre visible distinto
La variante `debug` de `:app` SHALL mostrar en el lanzador del dispositivo un nombre visible
distinto al de la variante `release`.

#### Scenario: Nombre visible de la variante debug
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se evalúa el recurso `app_name` de la variante `debug`
- **THEN** su valor es distinto al de la variante `release`
- **Verificación:** comprobación de configuración en `app/build.gradle.kts`
  (`androidComponents.onVariants`, design.md - Decisions), análoga a la de `release` para
  `applicationId`. Se evalúa en cada configuración del proyecto y falla si el `app_name` generado
  por `resValue` no es el esperado.

#### Scenario: Nombre visible de la variante release
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se evalúa el recurso `app_name` de la variante `release`
- **THEN** su valor es el nombre de publicación de la app, sin sufijo ni marca de depuración
- **Verificación:** la misma comprobación de configuración en `app/build.gradle.kts`
  (`androidComponents.onVariants`, design.md - Decisions), evaluada para la variante `release`.
