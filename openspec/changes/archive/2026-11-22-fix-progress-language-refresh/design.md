# Design

## Context

`FraseAFraseApp` (`:app`) alterna las pestañas con un `when`, sin `NavHost`. `hiltViewModel()` se ata a
la Activity, así que `ProgressViewModel` sobrevive a los cambios de pestaña. En cambio,
`ProgressRoute` se desmonta y se vuelve a montar. Hoy el ViewModel solo llama a `reload()` en `init`.
`ProgressAction` está vacía y `onAction` no hace nada.

Practicar ya resuelve el mismo problema. `PracticeSessionRoute` lanza
`LaunchedEffect(Unit) { onAction(Resume) }`, que se ejecuta en cada montaje, y su ViewModel relee.
Este diseño copia ese patrón, dentro de `:feature:progress`.

## Goals / Non-Goals

**Goals:**
- Releer el idioma destino activo, el nivel CEFR vigente y el resumen cada vez que se muestra Progreso.
- No volver a `Loading` en una relectura.

**Non-Goals:**
- Hacer reactivos los puertos de ajustes (`Flow`), o tocar `:app` o `:core:*`.

## Decisions

### D1. `ProgressAction.Resume` lanzada desde `ProgressRoute`

- `ProgressAction` pasa a tener `data object Resume`.
- `ProgressRoute` añade `LaunchedEffect(Unit) { viewModel.onAction(ProgressAction.Resume) }`.
- `onAction(Resume)` lanza `reload()` en `viewModelScope`.

Así se respeta el ADR-0006: un único `onAction()`, y la ruta solo conecta. Es el mismo patrón que
`PracticeSessionRoute`, de modo que las dos pestañas se comportan igual.

*Alternativas descartadas:*
- Observar el ciclo de vida (`LifecycleResumeEffect`) también relanzaría al volver de segundo plano.
  Eso no lo pide ninguna spec y no es lo que hace Practicar.
- Ajustes reactivos con `Flow` cambiarían puertos de `:core:domain` y quedan fuera de alcance.

### D2. Se mantiene la carga de `init`; las relecturas no pasan por `Loading`

- `init` sigue cargando. Así los tests existentes, que crean el ViewModel y esperan `Content`, siguen
  siendo válidos, y el ViewModel funciona aunque nadie envíe `Resume`.
- En el primer montaje se leerá dos veces (`init` y `Resume`). El resultado es igual, y
  `MutableStateFlow` descarta los valores iguales (`ProgressUiState.Content` es `data class`), así que
  no hay recomposición ni parpadeo. El coste de una lectura de más es aceptable.
- `reload()` nunca escribe `Loading`: solo sustituye el estado por el resultado. Esto cubre el escenario
  "Volver a Progreso sin cambios no pasa por el estado de carga".

No hace falta el `onlyIfChanged` de Practicar. Allí evita perder estado local de la tanda (deshacer,
revelado), y Progreso no tiene estado local: la comparación por igualdad del `StateFlow` basta.

*Alternativa descartada:* quitar la carga de `init` y cargar solo con `Resume`. Obligaría a reescribir
todos los tests del ViewModel para enviar `Resume` y no aporta comportamiento.

### D3. Una relectura en curso se cancela si llega otra

`ProgressViewModel` guarda el `Job` de la última recarga y lo cancela antes de lanzar otra. Así una
lectura antigua, por ejemplo la del `init`, no puede terminar después de una más reciente y dejar
datos del idioma anterior.

## Estrategia de test

- Unitarios de `ProgressViewModel` en `:feature:progress`, con los casos de uso reales sobre los fakes
  de `:core:testing`: `FakeSettingsRepository` (`setActiveTargetLocale`, `save`) y
  `FakeCatalogRepository.withCatalogs`. No hace falta ningún fake nuevo.
- `ProgressFixtures` añade una sobrecarga de `progressViewModel` que recibe los repositorios ya creados,
  para que el test cambie los ajustes después de abrir la pantalla y antes de enviar `Resume`.
- El escenario sin parpadeo se comprueba con Turbine: tras el primer `Content`, `Resume` sin cambios no
  emite nada (`expectNoEvents`).
- `ProgressRoute` no tiene test propio, igual que `PracticeSessionRoute`: el `LaunchedEffect` es pura
  conexión. Se comprueba compilando y, manualmente, en la app.

## Risks / Trade-offs

- [Lectura duplicada en el primer montaje] → No es observable gracias a la igualdad del `StateFlow`; el
  coste es una lectura de catálogo y ajustes.
- [Resultados fuera de orden] → Se evitan cancelando el `Job` anterior (D3).
