# Proposal

## Why

Si el usuario cambia el idioma destino activo o el nivel CEFR en Ajustes y vuelve a Progreso, la pantalla
sigue mostrando los recuentos, el nivel CEFR y el icono de idioma del primer arranque. Progreso solo lee
el resumen una vez, al crearse, y esa lectura sobrevive a los cambios de pestaña. Así se incumple la spec
vigente `language-indicator` ("El icono se actualiza tras cambiar de idioma", en cualquiera de las tres
pantallas) y Progreso enseña datos de un idioma que ya no se practica. Practicar ya lo resuelve
releyendo al volver a mostrarse; Progreso tiene que hacer lo mismo.

## What Changes

- Progreso vuelve a leer el resumen cada vez que se muestra: idioma destino activo, nivel CEFR vigente
  y recuento por nivel de consolidación.
- Nueva acción de presentación para "la pantalla vuelve a mostrarse", con el mismo patrón que Practicar.
  La lanza la ruta de Progreso al montarse.
- Mientras se relee, la pantalla no vuelve al estado de carga: el contenido anterior sigue visible
  hasta que llega el nuevo.
- Delta en `progress-overview`: nuevo requisito de refresco al volver a mostrarse, con escenarios de
  cambio de idioma destino activo y de cambio de nivel CEFR aplicado.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `progress-overview`: añade los requisitos "El progreso refleja los ajustes vigentes al volver a mostrarse" y "La relectura del progreso no pasa por el estado de carga".
  Cubre los recuentos, el total y el nivel CEFR tras cambiar el idioma destino activo o aplicar otro nivel CEFR.
  `language-indicator` no cambia: su requisito ya cubre Progreso, y este cambio solo hace que se cumpla.

## Impact

- `:feature:progress`: `ProgressAction`, `ProgressViewModel` y `ProgressRoute`; tests del ViewModel y sus fixtures.
- Sin cambios en `:core:domain`, `:core:data`, `:app` ni en el design system. Sin librerías nuevas.
- ADR afectados:
  - ADR-0006: un único `onAction()` y un estado inmutable; se respeta.
  - ADR-0018: progreso básico; no cambia qué se cuenta, solo cuándo se relee.
  - ADR-0010: el cambio queda dentro de la feature.
- Respeta el alcance del ADR-0012: no toca tandas, niveles ni rachas.

## Fuera de alcance

- Introducir un `NavHost` o cambiar cómo `:app` alterna las pestañas.
- Observar los ajustes con `Flow` reactivo desde el dominio (los puertos actuales son `suspend`).
- Refrescar Ajustes. Refrescar Practicar más allá de lo que ya hace.
- Cambios visuales en la pantalla de progreso.
