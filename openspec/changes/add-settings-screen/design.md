# Design

## Context

`SettingsRepository` (`:core:domain`) hoy solo tiene `suspend fun settings(): UserSettings`; su única
implementación es `DataStoreSettingsDataSource` (`:core:data`, DataStore Preferences, ADR-0013). Ningún
ViewModel lo usa directamente (ADR-0006: un ViewModel solo depende de casos de uso); hoy solo lo
consume `ObtainActiveBatch` dentro del dominio. `:feature:settings` está vacío (ver proposal.md - Why).
`FraseAFraseApp` (`:app`) muestra `PracticeSessionRoute` sin ningún mecanismo de navegación: no hay
librería de navegación en el proyecto.

## Goals / Non-Goals

**Goals:**
- Pantalla de ajustes en `:feature:settings` siguiendo el patrón UDF de `:feature:session` (ADR-0006).
- Escritura de ajustes en `:core:domain` / `:core:data`, hoy inexistente.
- Navegación de ida y vuelta entre práctica y ajustes con Jetpack Navigation 3.

**Non-Goals:**
- Persistir qué pantalla está abierta entre procesos (back stack *saveable*): con solo dos destinos y
  sin argumentos, perderlo en un cambio de proceso solo hace que la app reabra en la práctica, su
  pantalla de inicio habitual (spec `practice-session`).
- *Deep links*, animaciones de transición o layouts adaptables (*Scenes*): fuera de lo que ADR-0012
  pide para esta pantalla.
- Cualquier ajuste además de nivel CEFR y N.

## Decisions

### Navegación: Navigation 3 básico, sin *back stack* guardable, vive en `:app`

Se añade `androidx.navigation3:navigation3-runtime` y `androidx.navigation3:navigation3-ui` al catálogo
de versiones, en su última versión estable (no `alpha`/`beta`) en el momento de implementar (tarea
1.1). Si solo hay una versión prerelease disponible, se documenta aquí como riesgo aceptado (ver
Risks / Trade-offs) en vez de dejarlo implícito. `:app` sigue siendo el único módulo que conoce ambas
features (ADR-0010), así que las claves de ruta (`Practice`, `Settings`, `data object` en un paquete
de `:app`) y el `NavDisplay` viven ahí; ninguna feature declara una dependencia de proyecto nueva.

`FraseAFraseApp` pasa a:

```kotlin
val backStack = remember { mutableStateListOf<Any>(Practice) }
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = { key ->
        when (key) {
            Practice -> NavEntry(key) { PracticeSessionRoute(onOpenSettings = { backStack.add(Settings) }) }
            Settings -> NavEntry(key) { SettingsRoute(onBack = { backStack.removeLastOrNull() }) }
            else -> error("Unknown route: $key")
        }
    },
)
```

Se usa `mutableStateListOf` con `remember`, no `rememberNavBackStack` (que exige que las rutas sean
`@Serializable` y `NavKey`, y añade `navigation3-runtime-savedstate` más `kotlinx-serialization` a la
dependencia). Sin argumentos de navegación ni necesidad de sobrevivir a la muerte del proceso, esa
infraestructura no se justifica aquí; se puede añadir después si una ruta futura (ADR-0012 fuera de
alcance) lo necesita.

**Alternativas consideradas:**
- *Jetpack Navigation (Fragments/Compose "Navigation 2")*: requiere un grafo por rutas con cadenas y
  argumentos tipados por extensiones; más código para dos pantallas sin argumentos.
- *Estado propio sin librería* (un `sealed class` de pantalla actual en `:app`): evita una dependencia
  nueva, pero el usuario ya pidió Navigation 3 explícitamente para este cambio.

### `SettingsRepository` gana un método de escritura

Se añade `suspend fun save(settings: UserSettings)` a la interfaz existente, no una interfaz nueva:
sigue habiendo una única fuente (DataStore), así que `DataStoreSettingsDataSource` implementa
directamente la interfaz ampliada (ADR-0010). `DataStoreSettingsDataSource.save` escribe ambas claves
(`cefr_level`, `batch_size`) en una sola llamada a `dataStore.edit { ... }`, que aplica la transacción
de forma atómica: cualquier fallo de E/S deja las claves como estaban, sin mezclar valores antiguos y
nuevos (spec `user-settings`, "Guardado fallido no deja ajustes a medias"). El fallo se propaga tal
cual (`IOException` de DataStore), porque es un error de infraestructura, no un resultado esperable de
dominio.

`FakeSettingsRepository` (`:core:testing`) pasa a ser mutable para poder usarse en los tests de
guardado: su constructor sigue aceptando `UserSettings` por defecto, y expone un `var` interno que
`save()` reemplaza y `settings()` lee.

### Dos casos de uso nuevos en `:core:domain`, no acceso directo al repositorio

ADR-0006: un ViewModel solo depende de casos de uso. Se añaden `ObtainUserSettings` (envuelve
`settings()`) y `SaveUserSettings` (envuelve `save(settings)`), cada uno una clase con `operator fun
invoke`. `ObtainActiveBatch` sigue usando `SettingsRepository` directamente: es un caso de uso del
dominio, no un ViewModel, y la regla no le aplica.

### Validación de N en la UI, no en el dominio

`BatchSize` ya lanza `IllegalArgumentException` para un valor fuera de 10–30, documentado como error
de programación, no como entrada de usuario. La pantalla de ajustes valida el texto introducido (es un
entero y está en 10–30) antes de construir un `BatchSize` o de habilitar la acción de guardar
(`SettingsViewModel`); un valor no válido nunca llega a `SaveUserSettings`. El nivel CEFR no necesita
esta validación porque se elige de una lista cerrada (siempre un valor válido del enum).

### Icono de ajustes: Material directo, sin componente `Ds*`

El icono de ajustes del `TopAppBar` de `PracticeSessionScreen` usa `Icons.Default` (o
`Icons.AutoMirrored`, si aplica) de Material 3 directamente, igual que el resto de componentes de esa
pantalla (`Button`, `Card`, `Scaffold`). No se promueve a `:core:designsystem` porque lo usa una única
feature; se promovería si una segunda feature necesitara el mismo icono (convención del proyecto).

### `SettingsUiState` y `SettingsAction`

Igual que `PracticeSessionUiState.Loading`, `SettingsUiState` necesita representar "ajustes aún no
leídos" mientras `ObtainUserSettings` resuelve de forma asíncrona, para no mostrar un nivel CEFR o una N
por defecto antes de tiempo. Se modela como un estado sellado con un caso `Loading` y uno `Loaded`,
siguiendo el mismo patrón que `PracticeSessionUiState`:

```kotlin
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Loaded(
        val cefrLevel: CefrLevel,
        val batchSizeText: String,
        val isBatchSizeValid: Boolean,
    ) : SettingsUiState
}

sealed interface SettingsAction {
    data class SelectCefrLevel(val level: CefrLevel) : SettingsAction
    data class ChangeBatchSizeText(val text: String) : SettingsAction
    data object Save : SettingsAction
}
```

`batchSizeText` es el texto en edición (no un `BatchSize`), porque mientras no sea válido no hay un
`BatchSize` que representarlo. La acción de guardar está disponible únicamente cuando
`isBatchSizeValid` es verdadero (spec `settings-screen`, "N dentro de rango permite guardar"); no hay
ningún otro criterio para habilitarla o deshabilitarla, así que `Loaded` no necesita un campo que
indique si hay cambios sin guardar. Volver es una acción de la UI (`onBack` en `SettingsRoute`),
independiente de haber guardado, según la spec.

## Risks / Trade-offs

- [El usuario navega a ajustes y vuelve con el sistema (gesto/botón atrás) en vez del botón de la
  pantalla] → `NavDisplay.onBack` ya hace `backStack.removeLastOrNull()`, así que el comportamiento es
  el mismo que pulsar "volver"; no hace falta lógica adicional.
- [Cambiar de `mutableStateListOf` en memoria a un *back stack* guardable más adelante] → Si una ruta
  futura necesita argumentos o sobrevivir a la muerte del proceso, migrar a `rememberNavBackStack` es
  un cambio local a `FraseAFraseApp` y las claves de ruta; no afecta a las features.
- [Navigation 3 solo tiene versión prerelease (`alpha`/`beta`) en el momento de implementar] →
  Comprobar en la tarea 1.1; si ocurre, usarla de todos modos (es la única forma de cumplir lo que
  pidió el usuario) y registrar aquí la versión exacta fijada como riesgo aceptado, para revisarla en
  un cambio posterior cuando haya una versión estable.

## Migration Plan

Sin datos que migrar: el almacén de DataStore ya existe y solo gana claves ya definidas (`cefr_level`,
`batch_size`) con un método de escritura nuevo; las lecturas existentes no cambian de formato.
