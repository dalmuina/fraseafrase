# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Un único grupo de implementación, que se commitea en verde. Los escenarios referencian `specs/build-variants/spec.md`.

## 1. Identificador de la build de depuración (rojo → verde)

- [ ] 1.1 Crear `app/src/androidTest/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt`, con `@RunWith(AndroidJUnit4::class)` y `org.junit.Test` (JUnit 4, ADR-0014). Test `givenDebugBuild_whenRunningInstrumentedTests_thenTargetPackageEndsWithDebug`: lee `InstrumentationRegistry.getInstrumentation().targetContext.packageName` y afirma que termina en `.debug`. Referencia: escenarios *"Ejecución de tests instrumentados"* y *"Build de depuración instalada"*.
- [ ] 1.2 Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` con un emulador en marcha y comprobar que `BuildVariantTest` **falla por aserción** (el paquete es `com.dalmuina.fraseafrase`, no `.debug`). Si falla por otro motivo (cableado, compilación), corregir la prueba antes de continuar. Registrar la salida del fallo en el resultado del apply.
- [ ] 1.3 En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace`. Decisiones D1 y D2 de `design.md`.
- [ ] 1.4 En `app/build.gradle.kts`, añadir la comprobación de la variante `release` descrita en `design.md` D5: falla el build si `applicationId` de `release` no es `com.dalmuina.fraseafrase`.
- [ ] 1.5 Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` y comprobar que `BuildVariantTest` pasa y que el paquete del test instrumentado es `com.dalmuina.fraseafrase.debug.test`. Ejecutar `./gradlew :app:assembleRelease` y comprobar que pasa la comprobación de 1.4 (escenario *"Build de publicación sin cambios"*).

## 2. Verificación de dispositivo (decisión D6, manual)

Requiere firma local de la build `release`: el proyecto no define `signingConfig` para `release`, así que el APK sale sin firmar y no se puede instalar. Se firma solo para esta verificación, con la clave de depuración de Android, y el APK firmado se guarda fuera del repositorio (carpeta scratchpad). La clave no se commitea.

- [ ] 2.1 Firmar el APK de `release` con `apksigner sign --ks %USERPROFILE%\.android\debug.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out <scratchpad>\app-release-signed.apk app/build/outputs/apk/release/app-release-unsigned.apk`. Comprobar el identificador con `apkanalyzer manifest application-id <scratchpad>\app-release-signed.apk`, que debe devolver `com.dalmuina.fraseafrase`.
- [ ] 2.2 Instalar `release` firmada y `debug` en `Pixel_7a(AVD) - 17` y comprobar que las dos conviven. Escenario *"Instalación simultánea"*. Registrar en `tasks.md` la fecha, el dispositivo y el resultado.
- [ ] 2.3 Escenario *"Build de depuración anterior instalada"*: instalar sobre el emulador una build `debug` con el identificador original (versión previa a este cambio) y después la nueva `debug`. Comprobar que la antigua no cambia y que la nueva arranca con base de datos vacía. Registrar el resultado en `tasks.md`.
- [ ] 2.4 Escenario *"Progreso de la build de publicación tras los tests"*: con `release` instalada, practicar una tanda con al menos una frase respondida y cerrarla. Anotar el estado observado (frases certificadas y nivel) antes de ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache`. Comprobar que `release` sigue instalada y que el estado es el mismo. Registrar ambos valores en `tasks.md`.

## 3. Verificación final

- [ ] 3.1 Ejecutar `./gradlew check` y `./gradlew koverVerify`. Ambos en verde.
- [ ] 3.2 Ejecutar `./gradlew :app:assembleDebug` y comprobar con `apkanalyzer manifest application-id app/build/outputs/apk/debug/app-debug.apk` que devuelve `com.dalmuina.fraseafrase.debug`.
- [ ] 3.3 Cerrar la entrada "Build debug con `applicationIdSuffix`" de `docs/deuda-tecnica.md` moviéndola a una sección "Resuelto en `1.0.0/add-debug-application-id`". No ampliar alcance.
- [ ] 3.4 Nota para el revisor: detekt no analiza `src/androidTest` (ver `docs/deuda-tecnica.md`), así que `BuildVariantTest` no pasa por detekt. Esto no se corrige en este cambio.
