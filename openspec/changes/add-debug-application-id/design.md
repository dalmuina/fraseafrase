# Design

## Context

`app/build.gradle.kts` fija `applicationId = "com.dalmuina.fraseafrase"` en `defaultConfig` y no define ningún `applicationIdSuffix`, así que todas las variantes comparten identificador. Android guarda los datos de la aplicación (base de datos `practice.db` creada con `Room.databaseBuilder(context, ...)` y DataStore `settings`) en un directorio privado derivado del identificador. Por eso, dos builds con el mismo identificador comparten datos y se sobrescriben al instalarse. El módulo `:app` es también el único que declara el runner de tests (`HiltTestRunner`), y AGP desinstala la aplicación probada al terminar `connectedAndroidTest`.

Ver `proposal.md` (motivación y alcance) y `specs/build-variants/spec.md` (requisitos observables).

## Goals / Non-Goals

**Goals:**
- Que la build `debug` tenga identificador propio, de modo que su directorio de datos sea independiente del de `release`.
- Que la build `release` no cambie de identificador.
- Que el código fuente no cambie: el `namespace` y los paquetes Kotlin se mantienen.

**Non-Goals:**
- Diferenciar el nombre visible en el lanzador, la firma de la build de publicación o la migración de datos (ver `proposal.md`, "Fuera de alcance").

## Decisions

**D1. Sufijo de identificador solo en el tipo de build `debug`.** Se añade `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`, y `applicationId` queda como está en `defaultConfig`. Resultado: `debug` → `com.dalmuina.fraseafrase.debug`; `release` → `com.dalmuina.fraseafrase`.
- Alternativa descartada: cambiar `applicationId` en `defaultConfig` y añadir un sufijo solo para release. Obliga a tocar la variante que hoy usa el usuario y no aporta nada.
- Alternativa descartada: un `productFlavor` `dev`. Duplica variantes (`devDebug`, `devRelease`…) para un problema que solo tiene dos casos. Se valorará si aparecen más variantes.

**D2. El `namespace` no cambia.** `namespace` determina el paquete de la clase `R` y de `BuildConfig`, no el identificador instalado. Mantenerlo evita tocar imports y el `HiltTestRunner`, y la clase `R` sigue siendo la misma en ambas variantes.

**D3. La independencia de datos viene del identificador, no de código.** `practice.db` y `settings` se crean en el contexto de la aplicación, que es privado por identificador. No hace falta cambiar `PracticeSchema.kt` ni `DataStoreSettingsDataSource.kt`. Se comprobó que no hay rutas de base de datos ni de DataStore fijadas a un paquete (solo el nombre de fichero relativo) y que el manifiesto no declara `FileProvider` ni otros proveedores con `authorities`.

**D4. Verificación del requisito de la build `debug`.** El requisito "La build de depuración usa un identificador propio" se comprueba con un test instrumentado que compara por igualdad exacta `InstrumentationRegistry.getInstrumentation().targetContext.packageName` con `com.dalmuina.fraseafrase.debug`. La igualdad exacta evita aceptar valores como `x.debug`. Esto cubre también la ejecución de los tests (requisito 3, primer escenario), porque el mismo test corre contra la app probada.

**D5. Verificación automática de la build `release` (requisito 1, segundo escenario).** El identificador de `release` es una propiedad de configuración, así que se comprueba en el propio build. En `app/build.gradle.kts` se declara una constante `PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"`, que se usa tanto en `defaultConfig` como en la comprobación. La comprobación usa `androidComponents.onVariants(selector().withBuildType("release"))` y compara `applicationId.get()` con la constante; si no coincide, lanza `GradleException` con el valor esperado y el obtenido.
- Efecto: la comprobación se ejecuta en la fase de configuración de cualquier invocación de Gradle, no solo de `assembleRelease`. Un fallo también rompe `./gradlew test`. Es el comportamiento deseado.
- Verificación TDD: la tarea 1.4a introduce temporalmente una mutación (`applicationIdSuffix = ".mutante"` en `release`) sin la comprobación y demuestra que el build pasa, es decir, que nada detecta el error (rojo). La tarea 1.4b añade la comprobación y repite la mutación: el build debe fallar con mensaje claro (verde).
- Alternativa descartada: inspeccionar `processReleaseManifestForPackage` o un APK firmado en cada ejecución. Requiere firma (el proyecto no tiene `signingConfig` de `release`) y no detecta el error hasta generar el artefacto.
- Alternativa de respaldo: si la API de `onVariants` no compilara con AGP 9.3.3, `afterEvaluate { check(android.defaultConfig.applicationId == PRODUCTION_APP_ID) }`, equivalente porque `buildTypes.release` no tiene sufijo.

**D6. Verificación de dispositivo (requisitos 2 y 3, excepción a la regla de test automático).** Los escenarios "Instalación simultánea", "Build de depuración anterior instalada" y "Progreso de la build de publicación tras los tests" exigen dos variantes instaladas a la vez en el mismo dispositivo, o una instalación previa con datos reales. Ningún test de `:app` puede hacerlo sin un dispositivo con ambas variantes, así que se verifican manualmente en `Pixel_7a(AVD) - 17` (grupo 2 de `tasks.md`) y el resultado queda registrado en `tasks.md`. Es una excepción a la regla de CLAUDE.md, y los escenarios de `spec.md` la declaran explícitamente. La build `release` se firma solo para esta verificación, con la clave de depuración de Android, y el APK firmado se guarda fuera del repositorio.
- Antes de cada tarea de dispositivo se comprueba con `adb devices` que solo aparece el emulador. El test instrumentado desinstala la aplicación de destino, así que un móvil personal conectado puede verse afectado.

## Risks / Trade-offs

- **[Instalaciones antiguas]** Quien tenga instalada una build `debug` con el identificador original verá una app nueva junto a la antigua. → Mitigación: el usuario desinstala la antigua a mano si quiere; no se migran datos (fuera de alcance).
- **[Verificación de dispositivo]** D6 no está automatizado. → Mitigación: excepción declarada en `spec.md`; los pasos y el resultado quedan registrados en `tasks.md`; el `code-reviewer` revisa que estén registrados.
- **[Nombre de test]** Cambia solo el paquete del APK de test instrumentado, que pasa a `com.dalmuina.fraseafrase.debug.test`. El paquete fuente (`com.dalmuina.fraseafrase`) no cambia. → Mitigación: se comprueba con `apkanalyzer` en la tarea 1.5.
- **[Firma de `release`]** El proyecto no define `signingConfig` para `release`, así que el APK sale sin firmar. → Mitigación: la verificación de dispositivo firma con la clave de depuración solo fuera del repositorio; no se añade configuración de firma (fuera de alcance).
- **[Confusión visual]** Las dos variantes se llaman igual en el lanzador. Se puede practicar en `debug` creyendo que es la app de uso diario. → Mitigación: no se toca `resValue` en este cambio (fuera de alcance); queda como mejora posterior prioritaria, y el propio usuario decide cuándo instalar `debug`.
- **[Copias de seguridad]** `allowBackup="true"` en el manifiesto. Las copias van por identificador, así que `.debug` queda separado y no se ve afectado.
- **[Detekt]** detekt no analiza `src/androidTest` (ver `docs/deuda-tecnica.md`), así que `BuildVariantTest` no pasa por detekt. No se corrige en este cambio.

## Migration Plan

No hay migración de datos. Despliegue: cambiar el build script y reinstalar la build `debug`. Rollback: eliminar la línea `applicationIdSuffix` del tipo `debug`; la app vuelve a su identificador original.

## Open Questions

Ninguna.
