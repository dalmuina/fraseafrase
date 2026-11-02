# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Un único grupo de implementación (1), que se commitea en verde. Los escenarios referencian `specs/build-variants/spec.md`. Comandos para PowerShell desde la raíz del repositorio.

## 1. Identificadores de las variantes (rojo → verde)

- [x] 1.0 (prerrequisito de compilación, exento de rojo) En `app/build.gradle.kts`, activar `buildFeatures { buildConfig = true }` en `:app`. No cambia ningún comportamiento (D4). Sin esto, la prueba 1.2 falla por compilación y no por aserción, así que va antes del rojo y no tiene escenario.
- [ ] 1.1 (rojo) Crear `app/src/test/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt` con JUnit 6 Jupiter y Kotest (ADR-0014). Test `` `given the debug variant when reading application id then it has the debug suffix` ``: afirma `BuildConfig.APPLICATION_ID` igual a `"com.dalmuina.fraseafrase.debug"`. Un comentario en el test referencia el escenario *"Variante debug"* (D4). Sin condicionales ni lógica en el test: solo corre contra `debug` (`testBuildType`).
- [ ] 1.2 (rojo) Ejecutar `.\gradlew.bat :app:testDebugUnitTest`. Debe **fallar por aserción**: `APPLICATION_ID` vale `com.dalmuina.fraseafrase`, no `.debug`. Registrar el valor observado de `BuildConfig.APPLICATION_ID` en este fichero. Escenario *"Variante debug"*.
- [ ] 1.3 (verde) Satisface el escenario *"Variante debug"* de `specs/build-variants/spec.md`. En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace` (decisiones D1 y D2).
- [ ] 1.4 (verde) Ejecutar `.\gradlew.bat :app:testDebugUnitTest`: debe pasar. **Si `APPLICATION_ID` no incluye `.debug` tras 1.3, parar y revisar D4**: significa que `BuildConfig` no refleja el sufijo. Registrar el valor observado. Escenario *"Variante debug"*.
- [ ] 1.5 (verde + mutación de regresión, D5) En `app/build.gradle.kts`, añadir la comprobación de `release` descrita en D5: `androidComponents.onVariants(selector().withBuildType("release"))` con `PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"`, que lanza `GradleException` con el valor esperado y el obtenido. Usar la constante también en `defaultConfig`. Ejecutar `.\gradlew.bat :app:tasks` (debe pasar). Después, mutación **temporal**: añadir `applicationIdSuffix = ".mutante"` a `buildTypes.release`, ejecutar `.\gradlew.bat :app:tasks`: debe **fallar** con el mensaje esperado. Revertir la mutación y comprobar con `git diff app/build.gradle.kts` y `git status --short` que no quedan restos. Escenario *"Variante release"* e *"Identificadores distintos"*.
- [ ] 1.6 (verificación de build, sin escenario) Ejecutar desde la raíz, igual que la CI (`ci.yml`): `.\gradlew.bat check -PwarningsAsErrors=true`, `.\gradlew.bat detekt -PwarningsAsErrors=true` y `.\gradlew.bat koverVerify`. Los tres en verde. Ejecutar también `.\gradlew.bat :app:assembleDebug` y `.\gradlew.bat :app:assembleRelease`.

## 2. Checklist para la descripción de la PR (manual, no es escenario)

No es una verificación de escenario de `spec.md`: son comprobaciones de dispositivo que se copian a la descripción de la PR.

- [ ] 2.1 **Advertencia de datos:** si hay instalada una build `debug` antigua con el identificador `com.dalmuina.fraseafrase`, desinstalarla **borra su base de datos y su progreso de estudio**, sin copia automática. Instalar la nueva `debug` no modifica la antigua. Decidir antes si se conserva el progreso (exportarlo a mano) o se asume su pérdida (nota de migración de `design.md`).
- [ ] 2.2 (opcional, sin procedimiento hoy) Instalar `release` y `debug` en el emulador para comprobar que conviven. Hoy no se puede: el proyecto no tiene `signingConfig` y `assembleRelease` produce un APK sin firmar que no se instala. Omitir esta comprobación y anotarlo en la PR, hasta que exista `signingConfig`.

## 3. Verificación final y deuda

- [ ] 3.1 (documental, sin escenario) Cerrar la entrada "Build debug con `applicationIdSuffix`" de `docs/deuda-tecnica.md` moviéndola a una sección "Resuelto en `1.0.0/add-debug-application-id`". No ampliar alcance.
