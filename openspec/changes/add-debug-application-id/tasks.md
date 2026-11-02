# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Un único grupo de implementación (1), que se commitea en verde. Los escenarios referencian `specs/build-variants/spec.md`. Comandos para PowerShell desde la raíz del repositorio.

## 1. Identificadores de las variantes (rojo → verde)

- [ ] 1.0 (preparación, sin comportamiento) En `app/build.gradle.kts`, activar `buildFeatures { buildConfig = true }` en `:app`, necesario para que el test pueda leer `BuildConfig` (D4).
- [ ] 1.1 (rojo) Crear `app/src/test/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt` con JUnit 6 Jupiter y Kotest (ADR-0014). Test `givenBuildType_whenReadingApplicationId_thenMatchesVariantIdentity`: calcula `expected` como `"com.dalmuina.fraseafrase.debug"` si `BuildConfig.DEBUG` es `true`, y `"com.dalmuina.fraseafrase"` si es `false`; afirma `BuildConfig.APPLICATION_ID == expected`. Escenarios: *"Variante debug"*, *"Variante release"* e *"Identificadores distintos"* (D4).
- [ ] 1.2 (rojo) Ejecutar `.\gradlew.bat :app:testDebugUnitTest`. Debe **fallar por aserción**: `APPLICATION_ID` vale `com.dalmuina.fraseafrase`, no `.debug`. Ejecutar `.\gradlew.bat :app:testReleaseUnitTest`: debe **pasar** (la variante release ya cumple, D5). Registrar ambas salidas en este fichero.
- [ ] 1.3 (verde) En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace`. Decisiones D1 y D2.
- [ ] 1.4 (verde) Ejecutar `.\gradlew.bat :app:testDebugUnitTest :app:testReleaseUnitTest`. Ambos deben pasar. Escenarios *"Variante debug"*, *"Variante release"* e *"Identificadores distintos"*.
- [ ] 1.5 (mutación, D5) Añadir **temporalmente** `applicationIdSuffix = ".mutante"` a `buildTypes.release`. Ejecutar `.\gradlew.bat :app:testReleaseUnitTest`: debe **fallar por aserción**. Revertir la mutación y comprobar con `git diff app/build.gradle.kts` que solo quedan los cambios de 1.0 y 1.3.
- [ ] 1.6 Ejecutar `.\gradlew.bat :app:check` y comprobar en la salida que se ejecutan `testDebugUnitTest` y `testReleaseUnitTest`. Si `testReleaseUnitTest` no se ejecuta, añadir `check` → `dependsOn(testReleaseUnitTest)` en `:app` y repetir.
- [ ] 1.7 Ejecutar `.\gradlew.bat check` y `.\gradlew.bat koverVerify` en la raíz. Ambos en verde.

## 2. Checklist para la descripción de la PR (manual, no es escenario)

No es una verificación de escenario de `spec.md`: son comprobaciones de dispositivo que se copian a la descripción de la PR. Requieren firmar la build `release` para instalarla, porque el proyecto no define `signingConfig`. Si no se quiere firmar, se omite esta sección y se indica en la PR.

- [ ] 2.1 Si hay instalada una build `debug` antigua con el identificador `com.dalmuina.fraseafrase`, desinstalarla (nota de migración de `design.md`).
- [ ] 2.2 En el emulador, instalar `debug` y `release` firmada y comprobar que conviven. Anotar el resultado en la PR.

## 3. Verificación final y deuda

- [ ] 3.1 Ejecutar `.\gradlew.bat :app:assembleDebug` y `.\gradlew.bat :app:assembleRelease`. Ambos deben compilar.
- [ ] 3.2 Cerrar la entrada "Build debug con `applicationIdSuffix`" de `docs/deuda-tecnica.md` moviéndola a una sección "Resuelto en `1.0.0/add-debug-application-id`". No ampliar alcance.
