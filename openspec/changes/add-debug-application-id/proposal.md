# Proposal

## Why

Los tests instrumentados de `:app` se ejecutan contra la app real con su `applicationId` (`com.dalmuina.fraseafrase`) y, al terminar `connectedAndroidTest`, AGP desinstala esa app. Si la build de depuración comparte identificador con la de uso diario, instalar un build de desarrollo en el móvil personal sustituye la app instalada y, al ejecutar los tests, borra su base de datos `practice.db` y con ella el progreso de estudio. La deuda está registrada en `docs/deuda-tecnica.md` con prioridad alta, antes de instalar builds de desarrollo en un dispositivo personal.

## What Changes

- La build `debug` usa `applicationId` `com.dalmuina.fraseafrase.debug` mediante `applicationIdSuffix`, de modo que se instala junto a cualquier otra build y tiene su propio directorio de datos y su propia base de datos.
- La build `release` mantiene `applicationId` `com.dalmuina.fraseafrase`, sin cambios.
- Un test JVM de `:app` comprueba el identificador de cada variante leyendo `BuildConfig.APPLICATION_ID`, y para ello se activa `buildConfig` en `:app`.
- Los tests instrumentados de `:app` se ejecutan contra la build `debug` y, por tanto, borran solo los datos de `.debug`.
- No hay cambios de comportamiento de la aplicación ni de su modelo de datos.

## Capabilities

### New Capabilities
- `build-variants`: identidad y aislamiento de las variantes de build de la aplicación (depuración frente a publicación): qué identificador instala cada variante y qué datos comparte o no con las demás.

### Modified Capabilities
<!-- Ninguna: ninguna especificación vigente describe el identificador de la aplicación ni la configuración de build. -->

## Impact

- `app/build.gradle.kts`: `applicationIdSuffix = ".debug"` en `buildTypes.debug` y `buildFeatures { buildConfig = true }` en `:app`. No cambia `namespace`, que sigue siendo `com.dalmuina.fraseafrase`, por lo que el código Kotlin y los paquetes no se tocan.
- Tests de `:app` (`app/src/test`): test JVM nuevo con JUnit 6 Jupiter, conforme a ADR-0014. Los tests instrumentados de `app/src/androidTest` no cambian, aunque pasen a ejecutarse contra `.debug`. El runner `HiltTestRunner` no cambia.
- Dispositivos: quien tenga instalada una build de depuración anterior con el identificador original debe desinstalarla una vez antes de instalar la `release` firmada (ver `design.md`, migración). Los datos de la antigua no se migran.
- Sin dependencias nuevas ni cambios en el version catalog. No afecta a ADR-0012 (método de práctica), ADR-0010 (módulos) ni ADR-0013 (persistencia).

## Fuera de alcance

- Diferenciar el nombre visible en el lanzador (`resValue` o `app_name` por variante). Es una mejora posterior y no es necesaria para aislar los datos.
- Migrar los datos de una build de depuración antigua a la nueva.
- Configuración de firma de la build de publicación.
- Ejecutar los tests instrumentados en la CI (ya registrado como deuda aparte en `docs/deuda-tecnica.md`).
- Cambiar el runner genérico de `KotlinAndroid.kt` (deuda registrada aparte).
