# Design

## Context

`app/build.gradle.kts` fija `applicationId = "com.dalmuina.fraseafrase"` en `defaultConfig` y no define ningún `applicationIdSuffix`, así que todas las variantes comparten identificador. Android guarda los datos de la aplicación (base de datos `practice.db` creada con `Room.databaseBuilder(context, ...)` y DataStore `settings`) en un directorio privado derivado del identificador. Por eso, dos builds con el mismo identificador comparten datos y se sobrescriben al instalarse. Además, `buildConfig` no está activado en ningún módulo del proyecto, así que `BuildConfig` no se genera.

Ver `proposal.md` (motivación y alcance) y `specs/build-variants/spec.md` (requisitos observables).

## Goals / Non-Goals

**Goals:**
- Que la variante `debug` tenga identificador propio, distinto del de `release`.
- Que la variante `release` no cambie de identificador.
- Que el código fuente de la aplicación no cambie: el `namespace` y los paquetes Kotlin se mantienen.

**Non-Goals:**
- Diferenciar el nombre visible en el lanzador, la firma de la build de publicación o la migración de datos (ver `proposal.md`, "Fuera de alcance").

## Decisions

**D1. Sufijo de identificador solo en el tipo de build `debug`.** Se añade `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`, y `applicationId` queda como está en `defaultConfig`. Resultado: `debug` → `com.dalmuina.fraseafrase.debug`; `release` → `com.dalmuina.fraseafrase`.
- Alternativa descartada: cambiar `applicationId` en `defaultConfig` y añadir un sufijo solo para release. Obliga a tocar la variante que hoy usa el usuario y no aporta nada.
- Alternativa descartada: un `productFlavor` `dev`. Duplica variantes (`devDebug`, `devRelease`…) para un problema que solo tiene dos casos. Se valorará si aparecen más variantes.

**D2. El `namespace` no cambia.** `namespace` determina el paquete de la clase `R` y de `BuildConfig`, no el identificador instalado. Mantenerlo evita tocar imports y el `HiltTestRunner`.

**D3. La independencia de datos viene del identificador, no de código.** `practice.db` y `settings` se crean en el contexto de la aplicación, que es privado por identificador. No hace falta cambiar `PracticeSchema.kt` ni `DataStoreSettingsDataSource.kt`. Se comprobó que no hay rutas de base de datos ni de DataStore fijadas a un paquete (solo el nombre de fichero relativo) y que el manifiesto no declara `FileProvider` ni otros proveedores con `authorities`.

**D4. Verificación mediante `BuildConfig.APPLICATION_ID` en un test JVM.** Se activa `buildFeatures { buildConfig = true }` en `:app`, porque el proyecto no la tiene activa y el test necesita la constante. `BuildConfig.APPLICATION_ID` contiene el identificador de la variante con su sufijo. El test `BuildVariantTest` vive en `app/src/test` (JVM), usa JUnit 6 Jupiter y Kotest, que `KotlinAndroid.kt` ya añade a todos los módulos Android, y calcula el valor esperado a partir de `BuildConfig.DEBUG`: `com.dalmuina.fraseafrase.debug` si es `true`, `com.dalmuina.fraseafrase` si es `false`. Así el mismo test sirve para `testDebugUnitTest` y `testReleaseUnitTest`.
- Por qué JVM y no instrumentado: el test no necesita dispositivo ni APK, corre en la CI dentro de `check` y no desinstala ninguna aplicación.
- ADR-0014: los tests JVM usan JUnit 6, como exige para el unitario.
- Impedimento previsto: si `testReleaseUnitTest` no se ejecuta con `check` en esta configuración, se añade `tasks.named("check") { dependsOn("testReleaseUnitTest") }` en `:app`. Se comprueba en la tarea 1.6.
- Nombre del test: frase en comillas invertidas, según la convención de CLAUDE.md para tests JVM. Un comentario en el test referencia los escenarios que cubre.

**D5. Mutación como comprobación de regresión del test de `release`.** La variante `release` ya cumple el requisito antes del cambio, así que su test no empieza en rojo. Para comprobar que el test sí detecta un error, la tarea 1.5 introduce temporalmente un sufijo en `release` y comprueba que `testReleaseUnitTest` falla por aserción. La mutación se revierte antes de commitear.
- Desviación de ADR-0014:21 (rojo por test). ADR-0014:26-28 limita la mutación a la excepción de DI y declara que no aplica a código nuevo de producción. Aquí se aplica por decisión del usuario, sin ADR-0015. Se registra en la descripción de la PR.
- La tarea 1.2 registra el "pasa" de `release` como verde inicial, no como rojo.

**D6. Propiedades de Android que no se verifican como escenarios.** Estas garantías se derivan de tener identificadores distintos y no requieren comprobación propia en el proyecto:
- Dos variantes con identificadores distintos se instalan a la vez en el mismo dispositivo.
- Cada identificador tiene su propio directorio privado de datos, así que `practice.db` y `settings` no se comparten (D3).
- Desinstalar o ejecutar los tests instrumentados de una variante no toca los datos de la otra.
- La variante `release` conserva su identificador, así que su progreso no cambia por este cambio.

Por eso no aparecen como escenarios en `spec.md`. Se comprueban a mano solo como checklist para la descripción de la PR (ver `tasks.md`, grupo 2). La distinción de identificadores sí es un escenario de `spec.md`, y se verifica con una aserción explícita en `BuildVariantTest` (tarea 1.1).

## Risks / Trade-offs

- **[Instalaciones antiguas y firma; pérdida de datos]** La build `debug` antigua tiene el identificador `com.dalmuina.fraseafrase`, el mismo que tendrá la `release`, y está firmada con la clave de depuración. Android rechaza instalar una `release` firmada con otra clave sobre una app con el mismo identificador. Desinstalar la build `debug` antigua **borra su base de datos y su progreso de estudio**. Esa build puede ser la que se usa a diario. `allowBackup` no sustituye a una copia manual y no hay copia automática. → Mitigación: antes de desinstalar, decidir si se conserva el progreso (exportarlo a mano) o asumir su pérdida. Esta decisión es del usuario.
- **[Ámbito de `release` en la CI]** Si `check` no ejecuta `testReleaseUnitTest`, el test de `release` no protege la CI. → Mitigación: la tarea 1.6 lo comprueba y, si hace falta, añade la dependencia de `check`.
- **[Confusión visual]** Las dos variantes se llaman igual en el lanzador. Se puede practicar en `debug` creyendo que es la app de uso diario. → Mitigación: no se toca `resValue` en este cambio (fuera de alcance); queda como mejora posterior prioritaria.
- **[Copias de seguridad]** `allowBackup="true"` en el manifiesto. Las copias van por identificador, así que `.debug` queda separado y no se ve afectado.
- **[Detekt]** detekt no analiza `src/androidTest` (ver `docs/deuda-tecnica.md`). El test nuevo está en `src/test`, sí analizado por detekt.

## Migration Plan

Nota de migración para quien tenga instalada una build `debug` antigua: desinstalarla una vez antes de instalar una `release` firmada con otra clave. **Desinstalar la build `debug` antigua borra su base de datos y su progreso de estudio.** No hay copia automática; si se quiere conservar el progreso, exportarlo a mano antes, o asumir su pérdida. No hay migración de datos.

Instalar la nueva build `debug` no modifica la antigua. La nueva empieza con base de datos vacía. La pérdida ocurre solo al desinstalar la antigua. Hoy el proyecto no tiene `signingConfig` para `release`, así que la necesidad de desinstalar solo se presenta cuando exista una `release` firmada.

Despliegue: cambiar el build script. Rollback: eliminar `applicationIdSuffix` del tipo `debug` y `buildConfig = true` de `:app`.

## Open Questions

Ninguna.
