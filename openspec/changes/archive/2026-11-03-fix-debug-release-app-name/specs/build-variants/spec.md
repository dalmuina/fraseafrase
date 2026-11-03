# Spec Delta

## ADDED Requirements

### Requirement: Las variantes muestran un nombre visible distinto
La variante `debug` de `:app` SHALL mostrar en el lanzador del dispositivo un nombre visible
distinto al de la variante `release`.

#### Scenario: Nombre visible de la variante debug
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se evalúa el recurso `app_name` de la variante `debug`
- **THEN** su valor es distinto al de la variante `release`
- **Verificación:** comprobación de configuración en `app/build.gradle.kts`
  (`androidComponents.onVariants`, design.md - Decisions), análoga a la de `release` para
  `applicationId`. Se evalúa en cada configuración del proyecto y falla si el `app_name` generado
  por `resValue` no es el esperado.

#### Scenario: Nombre visible de la variante release
- **GIVEN** la configuración de build de `:app` del cambio
- **WHEN** se evalúa el recurso `app_name` de la variante `release`
- **THEN** su valor es el nombre de publicación de la app, sin sufijo ni marca de depuración
- **Verificación:** la misma comprobación de configuración en `app/build.gradle.kts`
  (`androidComponents.onVariants`, design.md - Decisions), evaluada para la variante `release`.
