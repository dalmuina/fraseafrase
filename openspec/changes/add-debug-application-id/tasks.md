# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Un único grupo de implementación (1), que se commitea en verde. Los escenarios referencian `specs/build-variants/spec.md`. Los resultados de verificación se registran en este fichero.

## 1. Identificador de la build de depuración (rojo → verde)

- [ ] 1.1 (rojo) Crear `app/src/androidTest/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt`, con `@RunWith(AndroidJUnit4::class)` y `org.junit.Test` (JUnit 4, ADR-0014). Test `givenDebugBuild_whenRunningInstrumentedTests_thenTargetPackageEndsWithDebug`: afirma con `assertEquals("com.dalmuina.fraseafrase.debug", InstrumentationRegistry.getInstrumentation().targetContext.packageName)`. Escenarios: *"Ejecución de tests instrumentados"* y *"Build de depuración instalada"*.
- [ ] 1.2 (rojo) Desconectar el móvil personal. Comprobar con `adb devices` que solo aparece el emulador (`emulator-5554`); si aparece otro dispositivo, no ejecutar. Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` y comprobar que `BuildVariantTest` **falla por aserción** (el paquete es `com.dalmuina.fraseafrase`, no `.debug`). Si falla por otro motivo (cableado, compilación), corregir la prueba antes de continuar. Registrar la salida del fallo en este fichero.
- [ ] 1.3 (verde) En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace`. Decisiones D1 y D2 de `design.md`.
- [ ] 1.4a (rojo) Comprobar que nada detecta un `release` mal configurado. Añadir **temporalmente** `applicationIdSuffix = ".mutante"` a `buildTypes.release`, sin la comprobación, y ejecutar `./gradlew :app:tasks`: el build debe **pasar**, lo que demuestra que el error no se detecta. Revertir la mutación antes de seguir. Escenario *"Build de publicación sin cambios"*; decisión D5.
- [ ] 1.4b (verde) Añadir a `app/build.gradle.kts` la constante `PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"` y la comprobación de D5 con `androidComponents.onVariants(selector().withBuildType("release"))`, que lanza `GradleException` si `applicationId` no coincide. Usar la constante también en `defaultConfig`. Ejecutar `./gradlew :app:tasks`: debe pasar. Repetir la mutación de 1.4a: el build debe **fallar** con el mensaje esperado y el obtenido. Revertir la mutación y volver a ejecutar `./gradlew :app:tasks`, que debe pasar.
- [ ] 1.5 (verde) Desconectar el móvil personal y comprobar con `adb devices` que solo aparece el emulador. Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` y comprobar que `BuildVariantTest` pasa. Comprobar el paquete del APK de test con `apkanalyzer manifest application-id app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk`, que debe devolver `com.dalmuina.fraseafrase.debug.test`. Ejecutar `./gradlew :app:assembleRelease` y comprobar que pasa la comprobación de 1.4b.

## 2. Verificación de dispositivo (decisión D6, manual)

Requiere firma local de la build `release`: el proyecto no define `signingConfig` para `release`, así que el APK sale sin firmar y no se puede instalar. Se firma solo para esta verificación, con la clave de depuración de Android, y el APK firmado se guarda fuera del repositorio (carpeta scratchpad). La clave no se commitea.

Antes de cada tarea de este grupo, comprobar con `adb devices` que solo aparece el emulador.

- [ ] 2.1 Generar la build de publicación sin firmar (`./gradlew :app:assembleRelease`). Firmar con PowerShell, sustituyendo `<versión>` por la versión de build-tools instalada (ver `ls $env:ANDROID_HOME\build-tools`) y `<scratchpad>` por la carpeta scratchpad:
  `& "$env:ANDROID_HOME\build-tools\<versión>\apksigner.bat" sign --ks "$env:USERPROFILE\.android\debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "<scratchpad>\app-release-signed.apk" app\build\outputs\apk\release\app-release-unsigned.apk`
  Si el instalador rechaza el APK, alinearlo antes con `zipalign -p 4`. Comprobar el identificador con `& "$env:ANDROID_HOME\cmdline-tools\latest\bin\apkanalyzer.bat" manifest application-id "<scratchpad>\app-release-signed.apk"`, que debe devolver `com.dalmuina.fraseafrase`. Escenario *"Build de publicación sin cambios"*.
- [ ] 2.2 Instalar `release` firmada y `debug` en `Pixel_7a(AVD) - 17`. Practicar y cerrar una tanda en `debug` y comprobar que el progreso de `release` (frases certificadas y nivel) no cambia; después, al revés. Escenario *"Instalación simultánea"*. Registrar fecha, dispositivo, valores antes y después, y resultado en este fichero.
- [ ] 2.3 Escenario *"Build de depuración anterior instalada"*: obtener una build `debug` con el identificador original con `git worktree add --detach "<scratchpad>\old-debug" master` (no crea rama), ejecutar `./gradlew :app:assembleDebug` dentro de esa carpeta e instalar su APK. Después instalar la nueva `debug`. Comprobar que la antigua no cambia y que la nueva arranca con base de datos vacía. Eliminar el worktree con `git worktree remove "<scratchpad>\old-debug"`. Registrar el resultado en este fichero.
- [ ] 2.4 Escenario *"Progreso de la build de publicación tras los tests"*: con `release` instalada, practicar una tanda con al menos una frase respondida y cerrarla. Anotar el estado (frases certificadas y nivel). Ejecutar `./gradlew :app:connectedDebugAndroidTest --no-configuration-cache` (tras comprobar `adb devices`). Comprobar que `release` sigue instalada y que el estado es el mismo. Registrar ambos valores en este fichero.

## 3. Verificación final

- [ ] 3.1 Ejecutar `./gradlew check` y `./gradlew koverVerify`. Ambos en verde.
- [ ] 3.2 Ejecutar `./gradlew :app:assembleDebug` y comprobar con `apkanalyzer manifest application-id app\build\outputs\apk\debug\app-debug.apk` que devuelve `com.dalmuina.fraseafrase.debug`. Escenario *"Build de depuración instalada"*.
- [ ] 3.3 Cerrar la entrada "Build debug con `applicationIdSuffix`" de `docs/deuda-tecnica.md` moviéndola a una sección "Resuelto en `1.0.0/add-debug-application-id`". No ampliar alcance.
