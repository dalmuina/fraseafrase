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

**D4. Verificación de la variante `debug` mediante `BuildConfig.APPLICATION_ID` en un test JVM.** Se activa `buildFeatures { buildConfig = true }` en `:app`, porque el proyecto no la tiene activa y el test necesita la constante. `BuildConfig.APPLICATION_ID` contiene el identificador de la variante con su sufijo. El test `BuildVariantTest` vive en `app/src/test` (JVM), usa JUnit 6 Jupiter y Kotest, que `KotlinAndroid.kt` ya añade a todos los módulos Android, y afirma que `BuildConfig.APPLICATION_ID` es `com.dalmuina.fraseafrase.debug`.
- Por qué solo `debug`: AGP ejecuta los tests unitarios contra `testBuildType`, que por defecto es `debug` y admite un único valor. No se genera `testReleaseUnitTest` (documentación de AGP, "Change the test build type"). La verificación de `release` no puede apoyarse en un test JVM sin cambiar esa configuración, y D5 la cubre.
- Por qué JVM y no instrumentado: el test no necesita dispositivo ni APK, corre en la CI dentro de `check` y no desinstala ninguna aplicación.
- ADR-0014: los tests JVM usan JUnit 6, como exige para el unitario.
- Nombre del test: frase en comillas invertidas, según la convención de CLAUDE.md para tests JVM. Un comentario en el test referencia los escenarios que cubre.

**D5. Verificación de `release` mediante comprobación de configuración.** En `app/build.gradle.kts`, sin imports adicionales (`selector()` es miembro de `androidComponents` y el compilador no resuelve el import `com.android.build.api.variant.selector`), y con la constante `val PRODUCTION_APP_ID = "com.dalmuina.fraseafrase"` (`val`, no `const val`, que es la forma segura en un script `.gradle.kts`):

```kotlin
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val actual = variant.applicationId.get()
        if (actual != PRODUCTION_APP_ID) {
            throw GradleException("applicationId de release: esperado '$PRODUCTION_APP_ID', obtenido '$actual'")
        }
    }
}
```

`PRODUCTION_APP_ID` se usa también en `defaultConfig`. La API de `onVariants` con `selector().withBuildType(...)` es la forma moderna de AGP, según la documentación de Android Developers. **No confirmado:** que `variant.applicationId.get()` sea legal dentro del callback con AGP 9.3.3. Si no lo es, la configuración falla ya en el paso "debe pasar" de la tarea 1.5, y la alternativa es comparar en `afterEvaluate` que `buildTypes.release.applicationIdSuffix` es nulo y que `defaultConfig.applicationId` es `PRODUCTION_APP_ID`.
- Efecto: la comprobación se evalúa en cada configuración del proyecto, no solo cuando se genera `release`. Con `org.gradle.configuration-cache=true` (`gradle.properties`), no se reejecuta cuando la caché se reutiliza, pero cualquier cambio en `build.gradle.kts` la invalida. Un fallo también rompe `./gradlew test` y `check`. Es el comportamiento deseado.
- Comprobación de regresión: la tarea 1.5 introduce temporalmente un sufijo en `release` y comprueba que la configuración falla, ejecutando con `--no-configuration-cache` para forzar la evaluación. La variante `release` ya cumplía el requisito antes del cambio, así que no empieza en rojo.
- Desviación de ADR-0014:21 (rojo por test). ADR-0014:26-28 limita la mutación a la excepción de DI y declara que no aplica a código nuevo de producción. El cambio es una aserción de configuración de `build.gradle.kts`, no código de producción de la aplicación. Aquí se aplica por decisión del usuario, sin ADR-0015. Se registra en la descripción de la PR. Si el usuario quiere constancia formal, la alternativa es un ADR-0015.
- Alternativa descartada: forzar `testReleaseUnitTest` con `enableUnitTest` en `beforeVariants`. No está confirmado en la documentación de AGP y añade una configuración de test que el proyecto no necesita.
- Alternativa descartada: inspeccionar el manifiesto o un APK firmado en cada build. Requiere firma, que el proyecto no tiene para `release`.

**D6. Propiedades de Android que no se verifican como escenarios.** Estas garantías se derivan de tener identificadores distintos y no requieren comprobación propia en el proyecto:
- Dos variantes con identificadores distintos se instalan a la vez en el mismo dispositivo.
- Cada identificador tiene su propio directorio privado de datos, así que `practice.db` y `settings` no se comparten (D3).
- Desinstalar o ejecutar los tests instrumentados de una variante no toca los datos de la otra.
- La variante `release` conserva su identificador, así que su progreso no cambia por este cambio.

Por eso no aparecen como escenarios en `spec.md`. Se comprueban a mano solo como checklist para la descripción de la PR (ver `tasks.md`, grupo 2). La distinción de identificadores sí es un escenario de `spec.md`, y se cumple cuando la verificación de `debug` y la de `release` pasan (tareas 1.2 y 1.4).

## Risks / Trade-offs

- **[Instalaciones antiguas y firma; pérdida de datos]** La build `debug` antigua tiene el identificador `com.dalmuina.fraseafrase`, el mismo que tendrá la `release`, y está firmada con la clave de depuración. Android rechaza instalar una `release` firmada con otra clave sobre una app con el mismo identificador. Desinstalar la build `debug` antigua **borra su base de datos y su progreso de estudio**. Esa build puede ser la que se usa a diario. `allowBackup` no sustituye a una copia manual y no hay copia automática. → Mitigación: antes de desinstalar, decidir si se conserva el progreso (exportarlo a mano) o asumir su pérdida. Esta decisión es del usuario.
- **[Comprobación de `release` en configuración]** Si la API de `onVariants` cambia o no se comporta como se espera, la comprobación de `release` dejaría de proteger. → Mitigación: la tarea 1.5 demuestra con una mutación que la comprobación falla; si no lo hiciera, la tarea se detiene.
- **[Confusión visual]** Las dos variantes se llaman igual en el lanzador. Se puede practicar en `debug` creyendo que es la app de uso diario. → Mitigación: no se toca `resValue` en este cambio (fuera de alcance); queda como mejora posterior prioritaria.
- **[Copias de seguridad]** `allowBackup="true"` en el manifiesto. Las copias van por identificador, así que `.debug` queda separado y no se ve afectado.
- **[Detekt]** detekt no analiza `src/androidTest` (ver `docs/deuda-tecnica.md`). El test nuevo está en `src/test`, sí analizado por detekt.

## Migration Plan

Nota de migración para quien tenga instalada una build `debug` antigua: desinstalarla una vez antes de instalar una `release` firmada con otra clave. **Desinstalar la build `debug` antigua borra su base de datos y su progreso de estudio.** No hay copia automática; si se quiere conservar el progreso, exportarlo a mano antes, o asumir su pérdida. No hay migración de datos automática. El procedimiento de copia manual de la entrada "Migración de la debug antigua" en `docs/deuda-tecnica.md` solo aplica si el uso diario pasa a `.debug`; si la release arranca vacía, no se copia nada.

Instalar la nueva build `debug` no modifica la antigua. La nueva empieza con base de datos vacía. La pérdida ocurre solo al desinstalar la antigua. Hoy el proyecto no tiene `signingConfig` para `release`, así que la necesidad de desinstalar solo se presenta cuando exista una `release` firmada.

Despliegue: cambiar el build script. Rollback: eliminar `applicationIdSuffix` del tipo `debug`, la comprobación de `onVariants` y `buildConfig = true` de `:app`.

## Open Questions

Ninguna.
