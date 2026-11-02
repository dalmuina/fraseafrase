# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Un único grupo de implementación (1), que se commitea en verde. Los escenarios referencian `specs/build-variants/spec.md`. Comandos para PowerShell desde la raíz del repositorio.

## 1. Identificadores de las variantes (rojo → verde)

- [ ] 1.0 (prerrequisito de compilación, exento de rojo) En `app/build.gradle.kts`, activar `buildFeatures { buildConfig = true }` en `:app`. No cambia ningún comportamiento (D4). Sin esto, la prueba 1.2 falla por compilación y no por aserción, así que va antes del rojo y no tiene escenario.
- [ ] 1.1 (rojo) Crear `app/src/test/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt` con JUnit 6 Jupiter y Kotest (ADR-0014). Test `` `given a build type when reading application id then it matches its variant identity` ``: calcula `expected` como `"com.dalmuina.fraseafrase.debug"` si `BuildConfig.DEBUG` es `true`, y `"com.dalmuina.fraseafrase"` si es `false`; afirma `BuildConfig.APPLICATION_ID == expected`. Un comentario en el test referencia los escenarios *"Variante debug"*, *"Variante release"* e *"Identificadores distintos"* (D4). Para *"Identificadores distintos"*, el test añade una aserción explícita: `BuildConfig.APPLICATION_ID` no es igual al valor de la otra variante (`"com.dalmuina.fraseafrase.debug"` shouldNotBe `"com.dalmuina.fraseafrase"`, con los dos literales).
- [ ] 1.2 (rojo) Ejecutar `.\gradlew.bat :app:testDebugUnitTest`. Debe **fallar por aserción**: `APPLICATION_ID` vale `com.dalmuina.fraseafrase`, no `.debug`. Registrar el valor observado de `BuildConfig.APPLICATION_ID`. **Si el valor no incluye `.debug`, parar y revisar D4 antes de continuar**: significa que `BuildConfig` no refleja el sufijo. Ejecutar `.\gradlew.bat :app:testReleaseUnitTest`: debe **pasar** (verde inicial, la variante release ya cumple, D5). Registrar ambas salidas en este fichero. Escenarios *"Variante debug"* y *"Variante release"*.
- [ ] 1.3 (verde) Satisface los escenarios *"Variante debug"* e *"Identificadores distintos"* de `specs/build-variants/spec.md`. En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace` (decisiones D1 y D2).
- [ ] 1.4 (verde) Ejecutar `.\gradlew.bat :app:testDebugUnitTest :app:testReleaseUnitTest`. Ambos deben pasar. Registrar el valor observado de `BuildConfig.APPLICATION_ID` en cada variante. Escenarios *"Variante debug"*, *"Variante release"* e *"Identificadores distintos"*.
- [ ] 1.5 (mutación, comprobación de regresión, D5) Añadir **temporalmente** `applicationIdSuffix = ".mutante"` a `buildTypes.release`. Ejecutar `.\gradlew.bat :app:testReleaseUnitTest`: debe **fallar por aserción**. Revertir la mutación. Comprobar con `git diff app/build.gradle.kts` y `git status --short` que solo quedan los cambios de 1.0 y 1.3 y que no hay restos de la mutación en ningún fichero. Escenario *"Variante release"*.
- [ ] 1.6 (verificación de cableado de `check`) Ejecutar `.\gradlew.bat check --dry-run -PwarningsAsErrors=true` y comprobar que aparecen `:app:testDebugUnitTest` y `:app:testReleaseUnitTest`. El dry-run no ejecuta tareas. Si no aparece `:app:testReleaseUnitTest`, añadir `tasks.named("check") { dependsOn("testReleaseUnitTest") }` en `:app` y repetir (D4).
- [ ] 1.7 (verificación de build, sin escenario) Ejecutar desde la raíz, igual que la CI (`ci.yml`): `.\gradlew.bat check -PwarningsAsErrors=true`, `.\gradlew.bat detekt -PwarningsAsErrors=true` y `.\gradlew.bat koverVerify`. Los tres en verde.

## 2. Checklist para la descripción de la PR (manual, no es escenario)

No es una verificación de escenario de `spec.md`: son comprobaciones de dispositivo que se copian a la descripción de la PR.

- [ ] 2.1 **Advertencia de datos:** si hay instalada una build `debug` antigua con el identificador `com.dalmuina.fraseafrase`, desinstalarla **borra su base de datos y su progreso de estudio**, sin copia automática. Instalar la nueva `debug` no modifica la antigua. Decidir antes si se conserva el progreso (exportarlo a mano) o se asume su pérdida (nota de migración de `design.md`).
- [ ] 2.2 (opcional, sin procedimiento hoy) Instalar `release` y `debug` en el emulador para comprobar que conviven. Hoy no se puede: el proyecto no tiene `signingConfig` y `assembleRelease` produce un APK sin firmar que no se instala. Omitir esta comprobación y anotarlo en la PR, hasta que exista `signingConfig`.

## 3. Verificación final y deuda

- [ ] 3.1 (verificación de build, sin escenario) Ejecutar `.\gradlew.bat :app:assembleDebug` y `.\gradlew.bat :app:assembleRelease`. Ambos deben compilar.
- [ ] 3.2 (documental, sin escenario) Cerrar la entrada "Build debug con `applicationIdSuffix`" de `docs/deuda-tecnica.md` moviéndola a una sección "Resuelto en `1.0.0/add-debug-application-id`". No ampliar alcance.
