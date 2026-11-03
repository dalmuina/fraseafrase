# Proposal

## Why

`:feature:settings` existe como módulo vacío. El ADR-0012 incluye los ajustes (nivel CEFR y tamaño de
tanda N) dentro del alcance del MVP, y la spec `user-settings` ya cubre su persistencia y valores por
defecto, pero dice explícitamente que no define la pantalla. Sin una UI que permita editarlos ni una
forma de llegar a ella desde la práctica, el usuario no puede cambiar su nivel ni N una vez instalada
la app.

## What Changes

- Nueva pantalla de ajustes (`:feature:settings`) que muestra el nivel CEFR y el tamaño de tanda N
  vigentes, permite cambiarlos y los guarda.
- Nuevo caso de uso de guardado de ajustes en `:core:domain` y la escritura correspondiente en
  `DataStoreSettingsDataSource` (`:core:data`): hoy `SettingsRepository` solo tiene lectura.
- Validación en la UI de la pantalla de ajustes: el nivel CEFR se elige de una lista cerrada (siempre
  válido) y N se restringe a enteros entre 10 y 30 antes de poder guardar.
- Navegación entre la pantalla de práctica y la de ajustes con Jetpack Navigation 3 (`NavBackStack` /
  `NavDisplay` en `:app`, que ya es el único módulo que conoce ambas features). Añade un icono de
  ajustes en la pantalla de práctica como punto de entrada y una acción de volver en la de ajustes.
- Añade la dependencia de Navigation 3 al catálogo de versiones.

## Capabilities

### New Capabilities

- `settings-screen`: pantalla de ajustes (ver y editar nivel CEFR y N, validación, guardado) y su
  navegación de entrada/salida con la pantalla de práctica.

### Modified Capabilities

- `user-settings`: añade la escritura/persistencia de un cambio de ajustes hecho por el usuario (hoy
  la spec solo cubre lectura, valores por defecto y *fallback*).

## Impact

- `:core:domain`: nuevo puerto de escritura en `SettingsRepository` (o caso de uso dedicado), nuevo
  caso de uso `SaveUserSettings` (nombre a confirmar en diseño).
- `:core:data`: `DataStoreSettingsDataSource` añade la escritura de nivel CEFR y N.
- `:feature:settings`: deja de estar vacío; `SettingsRoute`, `SettingsScreen`, `SettingsUiState`,
  `SettingsAction`, `SettingsViewModel`, siguiendo el patrón UDF de `:feature:session` (ADR-0006).
- `:app`: `FraseAFraseApp` pasa a alojar un `NavDisplay` con dos destinos (práctica y ajustes) en vez
  de mostrar `PracticeSessionRoute` directamente.
- `gradle/libs.versions.toml`: nueva entrada para Navigation 3 (`androidx.navigation3`).
- Fuera de alcance: cualquier otro ajuste además de nivel CEFR y N (ADR-0012 no los contempla),
  animaciones de transición, *deep links*, y cualquier Scene distinta de la navegación básica entre
  dos destinos.
