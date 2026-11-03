# Tasks

## 1. Nombre visible por variante

- [ ] 1.1 Añadir a `androidComponents.onVariants` en `app/build.gradle.kts` la comprobación de
      configuración del `app_name` esperado por variante (design.md - Decisions) y confirmar que
      falla en la configuración del proyecto (`./gradlew :app:help` o cualquier tarea que
      configure `:app`) porque `resValue` todavía no existe para `app_name`.
- [ ] 1.2 Activar `buildFeatures { resValues = true }` y añadir `resValue("string", "app_name",
      ...)` a `debug` y `release` en `app/build.gradle.kts` (design.md - Decisions), y eliminar el
      `app_name` de `app/src/main/res/values/strings.xml`. Verificar escenarios "Nombre visible de
      la variante debug" y "Nombre visible de la variante release"
      (`specs/build-variants/spec.md`) comprobando que la configuración del proyecto
      (`./gradlew :app:help`) ya no falla.

## 2. Verificación manual en dispositivo

- [ ] 2.1 Instalar `assembleDebug` y `assembleRelease` en el mismo dispositivo o emulador y
      comprobar en el lanzador que los nombres visibles son distintos (evidencia adicional a la
      comprobación de configuración de la tarea 1). Registrar el resultado en la descripción de la
      PR.

## 3. Calidad

- [ ] 3.1 Ejecutar `./gradlew check` y verificar que termina en verde.

## Workflow follow-up

- Archivar el cambio con `/opsx:archive` cuando la revisión del proyecto lo autorice.
- Verificar el cambio archivado con `/opsx:verify`.
