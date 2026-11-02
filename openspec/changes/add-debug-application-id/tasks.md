# Tasks

Orden: primero la prueba que falla por el motivo esperado, después el cambio de build. Un único grupo de implementación (1), que se commitea en verde. Los escenarios referencian `specs/build-variants/spec.md`. Comandos para PowerShell desde la raíz del repositorio.

## 1. Identificadores de las variantes (rojo → verde)

- [x] 1.0 (prerrequisito de compilación, exento de rojo) En `app/build.gradle.kts`, activar `buildFeatures { buildConfig = true }` en `:app`. No cambia ningún comportamiento (D4). Sin esto, la prueba 1.2 falla por compilación y no por aserción, así que va antes del rojo y no tiene escenario.
- [x] 1.1 (rojo) Antes de empezar: si existe `app/src/test/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt` de una ejecución anterior, **no lo reutilices**: el fichero se crea desde cero según esta tarea. Crear `app/src/test/kotlin/com/dalmuina/fraseafrase/BuildVariantTest.kt` con JUnit 6 Jupiter y Kotest (ADR-0014). Test `` `given the debug variant when reading application id then it has the debug suffix` ``: afirma `BuildConfig.APPLICATION_ID` igual a `"com.dalmuina.fraseafrase.debug"`, y además `BuildConfig.APPLICATION_ID` distinto de `"com.dalmuina.fraseafrase"` (distinción en la misma variante). Un comentario en el test referencia solo los escenarios *"Variante debug"* e *"Identificadores distintos"* (D4). Sin condicionales ni lógica en el test: solo corre contra `debug` (`testBuildType`).
- [x] 1.2 (rojo) Ejecutar `.\gradlew.bat :app:testDebugUnitTest`. Debe **fallar por aserción**: `APPLICATION_ID` vale `com.dalmuina.fraseafrase`, no `.debug`. Registrar el valor observado de `BuildConfig.APPLICATION_ID` en este fichero. Escenario *"Variante debug"*. **Registro:** observado `com.dalmuina.fraseafrase` (fallo por aserción en `BuildVariantTest.kt:21`, sin error de compilación).
- [x] 1.3 (verde) Satisface el escenario *"Variante debug"* de `specs/build-variants/spec.md`. En `app/build.gradle.kts`, añadir `applicationIdSuffix = ".debug"` dentro de `buildTypes.debug`. No cambiar `defaultConfig.applicationId` ni `namespace` (decisiones D1 y D2).
- [x] 1.4 (verde) Ejecutar `.\gradlew.bat :app:testDebugUnitTest`: debe pasar. **Si `APPLICATION_ID` no incluye `.debug` tras 1.3, parar y revisar D4**: significa que `BuildConfig` no refleja el sufijo. Registrar el valor observado. Escenarios *"Variante debug"* e *"Identificadores distintos"*. **Registro:** observado `com.dalmuina.fraseafrase.debug`; pasa.
- [x] 1.5 (verde + mutación de regresión, D5) En `app/build.gradle.kts`, añadir la comprobación de `release` descrita en D5: `androidComponents { onVariants(selector().withBuildType("release")) { ... } }` con `val PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"`, que lanza `GradleException` con el valor esperado y el obtenido. Usar la constante también en `defaultConfig`. Ejecutar `.\gradlew.bat :app:tasks --no-configuration-cache` (debe pasar; si la API de `get()` no es legal en el callback, la configuración falla aquí y se aplica la alternativa de D5 antes de seguir). Después, mutación **temporal**: añadir `applicationIdSuffix = ".mutante"` a `buildTypes.release` y ejecutar `.\gradlew.bat :app:tasks --no-configuration-cache`: debe **fallar** y el mensaje debe contener el valor esperado y el obtenido. Revertir la mutación y comprobar con `git diff app/build.gradle.kts` y `git status --short` que no quedan restos. Escenarios *"Variante release"* e *"Identificadores distintos"*.
- [x] 1.6 (verificación de build, sin escenario) Ejecutar desde la raíz, igual que la CI (`ci.yml`): `.\gradlew.bat check -PwarningsAsErrors=true`, `.\gradlew.bat detekt -PwarningsAsErrors=true` y `.\gradlew.bat koverVerify`. Los tres en verde. Ejecutar también `.\gradlew.bat :app:assembleDebug` y `.\gradlew.bat :app:assembleRelease`.

## 2. Checklist para la descripción de la PR (manual, no es escenario)

No es una verificación de escenario de `spec.md`: son comprobaciones de dispositivo que se copian a la descripción de la PR.

- [x] 2.1 **Advertencia de datos:** no se desinstala la build debug antigua. No es necesario mientras no exista una release instalable. Migración diferida, registrada en `docs/deuda-tecnica.md` (sección de este cambio).
- [x] 2.2 Descartada por decisión del usuario. Para la descripción de la PR: "Instalar release y debug a la vez requiere signingConfig de release (pendiente)". Firma registrada en `docs/deuda-tecnica.md`.

## 3. Verificación final y deuda

- [x] 3.1 (documental, sin escenario) Cerrar la entrada "Build debug con `applicationIdSuffix`" de `docs/deuda-tecnica.md` moviéndola a una sección "Resuelto en `1.0.0/add-debug-application-id`". No ampliar alcance.
