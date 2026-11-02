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

**D4. Verificación del requisito de la build `debug`.** El requisito "La build de depuración usa un identificador propio" se comprueba con un test instrumentado que lee `InstrumentationRegistry.getInstrumentation().targetContext.packageName` y afirma que termina en `.debug`. Esto cubre también la ejecución de los tests (requisito 3, primer escenario), porque el mismo test corre contra la app probada.

**D5. Verificación de la build `release` (requisito 1, segundo escenario).** No hay un test instrumentado que pueda ejecutar la build `release` desde los tests de `:app`. Se comprobará con un comando de Gradle que genera el manifiesto fusionado de `release` y se inspecciona su `package`, o con `aapt2 dump badging` sobre el APK. Es una verificación manual, documentada en `tasks.md`, igual que la tarea 5.4 de `add-practice-session`.

**D6. Verificación de la instalación simultánea y de la conservación del progreso (requisitos 2 y 3).** Son escenarios de dispositivo: se instala `release`, se guarda progreso, se ejecuta `connectedDebugAndroidTest` y se comprueba que `release` sigue con su progreso. Se verifican manualmente en el emulador `Pixel_7a(AVD) - 17` y se registra el resultado en el handoff. No se automatiza porque exige dos variantes instaladas a la vez en el mismo dispositivo.

## Risks / Trade-offs

- **[Instalaciones antiguas]** Quien tenga instalada una build `debug` con el identificador original verá una app nueva junto a la antigua. → Mitigación: el usuario desinstala la antigua a mano si quiere; no se migran datos (fuera de alcance).
- **[Verificación parcialmente manual]** D5 y D6 no están automatizados. → Mitigación: el comando y el resultado esperado quedan escritos en `tasks.md`; el `code-reviewer` revisa que las tareas manuales estén registradas.
- **[Nombre de test]** El paquete del test instrumentado pasa a `com.dalmuina.fraseafrase.debug.test`. → Mitigación: ningún script del repositorio referencia el paquete del test; se comprobará al ejecutar `connectedDebugAndroidTest`.

## Migration Plan

No hay migración de datos. Despliegue: cambiar el build script y reinstalar la build `debug`. Rollback: eliminar la línea `applicationIdSuffix` del tipo `debug`; la app vuelve a su identificador original.

## Open Questions

Ninguna.
