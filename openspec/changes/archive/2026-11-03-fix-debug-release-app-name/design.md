# Design

## Context

`app/build.gradle.kts` ya separa `applicationId` por `buildType` (`add-debug-application-id`).
El nombre visible (`android:label="@string/app_name"` en `AndroidManifest.xml`) sigue viniendo del
único `app_name` de `app/src/main/res/values/strings.xml`, igual para ambas variantes. Ver
proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Que el lanzador del dispositivo muestre un nombre distinto para `debug` y `release`.

**Non-Goals:**
- Cambiar el icono, el `applicationId` o cualquier otro identificador de las variantes.
- Tocar la UI de la app (ningún texto en pantalla usa `app_name`).

## Decisions

- **`resValue` por `buildType` en `app/build.gradle.kts`**, en vez de un `strings.xml` por
  variante (`src/debug/res`, `src/release/res`). Una sola línea por variante, sin duplicar el
  fichero de recursos ni arrastrar el resto de `strings.xml` a un directorio por variante.
  AGP 9 cambió el valor por defecto de `android.defaults.buildfeatures.resvalues` de `true` a
  `false`: hay que activar `buildFeatures { resValues = true }` en `:app`, o el DSL `resValue(...)`
  falla en la configuración del proyecto.
  ```kotlin
  android {
      buildFeatures {
          resValues = true
      }
  }

  buildTypes {
      debug {
          resValue("string", "app_name", "Frase a Frase (Debug)")
      }
      release {
          resValue("string", "app_name", "Frase a Frase")
      }
  }
  ```
- El `app_name` que queda en `app/src/main/res/values/strings.xml` se elimina: con `resValue` en
  ambas variantes no hace falta un valor por defecto, y mantenerlo sería un tercer valor sin usar.
- **Comprobación de configuración en `app/build.gradle.kts`, igual que D5 para `applicationId`**,
  en vez de confiar solo en la verificación manual en dispositivo. El nombre visible generado por
  `resValue` sí es legible en tiempo de configuración: AGP expone `resValues` como un
  `MapProperty<ResValue.Key, ResValue>` en `Variant` (interfaz `GeneratesResources`, implementada
  por `ApplicationVariant`), igual que expone `applicationId`. Se amplía el bloque
  `androidComponents.onVariants` que ya comprueba el `applicationId` de `release`:
  ```kotlin
  androidComponents {
      onVariants { variant ->
          // El MapProperty de resValues no se puede leer con .get() dentro de onVariants (se
          // lanza "configuration of project has not completed yet"): se difiere a cuando el
          // grafo de tareas está listo, momento en el que la configuración de todos los
          // proyectos ya ha terminado. ResValue.Key no tiene constructor público: se crea con
          // variant.makeResValueKey(...).
          project.gradle.taskGraph.whenReady {
              val expected = if (variant.buildType == "debug") "Frase a Frase (Debug)" else "Frase a Frase"
              val actual = variant.resValues.get()[variant.makeResValueKey("string", "app_name")]?.value
              if (actual != expected) {
                  throw GradleException(
                      "app_name de la variante '${variant.name}': esperado '$expected', obtenido '$actual'",
                  )
              }
          }
      }
  }
  ```
  Esta comprobación falla primero sin el `resValue` (tarea 1.1 de `tasks.md`, roja antes de
  añadirlo, con el motivo esperado: ausencia del valor, no un error de sintaxis) y pasa en cuanto
  se añade (verde), como exige `openspec/config.yaml` (`rules.tasks`). Verificado también que
  detecta un valor incorrecto (tarea 1.2).
- La verificación manual en dispositivo (tarea 2.1) se mantiene como evidencia adicional de que el
  lanzador muestra de verdad los dos nombres, no como única prueba.

## Risks / Trade-offs

- [`resValue()` es una API de AGP marcada para sustituirse por `variant.resValues.put(...)` en una
  futura versión mayor, según el calendario de migración de AGP] → aceptado: AGP 9.3.3 (versión
  actual del catálogo) todavía la admite sin avisos de obsolescencia; migrar es una tarea futura
  del cambio de versión de AGP, no de este cambio.
