# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Los escenarios referencian `specs/build-variants/spec.md`.

## 1. Prueba del identificador de la build de depuración (rojo)

- [ ] 1.1 Crear `app/src/androidTest/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt` con el test `givenDebugBuild_whenRunningInstrumentedTests_thenTargetPackageEndsWithDebug`: lee `InstrumentationRegistry.getInstrumentation().targetContext.packageName` y afirma que termina en `.debug`. Referencia: *Escenario "Ejecución de tests instrumentados"* y *"Build de depuración instalada"*.
- [ ] 1.2 Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` con un emulador en marcha y comprobar que **falla** porque el paquete es `com.dalmuina.fraseafrase` y no `com.dalmuina.fraseafrase.debug`. Si falla por otro motivo (cableado, compilación), corregir la prueba antes de continuar.

## 2. Identificador de la build de depuración (verde)

- [ ] 2.1 En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace`. Referencia: *Escenarios "Build de depuración instalada" y "Build de publicación sin cambios"*; decisiones D1 y D2 de `design.md`.
- [ ] 2.2 Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` y comprobar que `BuildVariantTest` pasa. Referencia: *Escenario "Ejecución de tests instrumentados"*.

## 3. Verificación manual de la build de publicación y de los datos (decisiones D5 y D6)

- [ ] 3.1 Generar la build de publicación y comprobar que su identificador sigue siendo `com.dalmuina.fraseafrase`: `./gradlew :app:assembleRelease` y revisar el `package` del manifiesto fusionado de `release` (o `aapt2 dump badging` sobre el APK). Referencia: *Escenario "Build de publicación sin cambios"*.
- [ ] 3.2 Instalar `release` y `debug` en el mismo emulador (`Pixel_7a(AVD) - 17`) y comprobar que ambas se pueden instalar a la vez. Referencia: *Escenario "Instalación simultánea"*.
- [ ] 3.3 Guardar progreso en `release`, ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` y comprobar que `release` sigue instalada y conserva ese progreso. Referencia: *Escenario "Progreso de la build de publicación tras los tests"*.
- [ ] 3.4 Registrar el resultado de 3.1 a 3.3 en `.claude/handoff.md` (no es tarea de código, pero sin ese registro la verificación no queda cerrada).

## 4. Verificación final

- [ ] 4.1 Ejecutar `./gradlew check` y `./gradlew koverVerify`. Ambos en verde.
- [ ] 4.2 Ejecutar `./gradlew :app:assembleDebug` y comprobar que el APK generado instala el identificador `com.dalmuina.fraseafrase.debug`.
- [ ] 4.3 Nota para el revisor: detekt no analiza `src/androidTest` (ver `docs/deuda-tecnica.md`), así que `BuildVariantTest` no pasa por detekt. Esto no se corrige en este cambio.
