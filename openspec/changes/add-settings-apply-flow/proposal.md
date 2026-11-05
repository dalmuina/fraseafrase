# Proposal

## Why

Hoy guardar los ajustes no da ninguna confirmación visible, y cambiar el nivel CEFR o el tamaño de
tanda N no tiene ningún efecto hasta que, por casualidad, se cierre la tanda activa más adelante: el
usuario puede cambiar de A1 a B1 y seguir viendo frases A1 durante días sin saber si el cambio se
guardó. Es la segunda prioridad de la hoja de ruta de mejoras acordada tras `add-practice-batch-indicator`
(P1): cerrar el ciclo de "cambio ajustes -> veo el efecto" de forma inmediata y visible.

## What Changes

- Al guardar los ajustes con éxito, la pantalla de ajustes MUST mostrar una confirmación visual
  (p. ej. un snackbar "Ajustes guardados").
- Si el nivel CEFR o el tamaño de tanda N guardados difieren de los que estaban persistidos antes de
  guardar, además de la confirmación: la aplicación MUST cerrar la tanda activa (si hay alguna) sin
  mostrar el aviso previo de no certificadas — es un cierre deliberado desde ajustes, no un cierre
  casual desde la práctica — y MUST navegar a la pestaña de práctica, donde se compone la tanda
  siguiente con las reglas de prioridad ya vigentes de `practice-batch` (arrastradas, luego
  pendientes, luego nuevas del nivel elegido).
- Si se guarda sin que el nivel CEFR ni la N cambien respecto a lo persistido, solo se muestra la
  confirmación: no se cierra nada ni se navega.

Fuera de alcance de este cambio (quedan en la hoja de ruta para cambios posteriores):

- Mostrar el aviso de no certificadas antes de este cierre forzado: se decidió explícitamente que
  este cierre no lo muestra.
- Sustituir la racha visible por una insignia de categoría y el ADR sobre cómo un fallo baja el nivel
  de consolidación (P3/P4).
- Cualquier rediseño visual de la confirmación o de componentes `Ds*` a medida: se usan los
  componentes de Material 3 ya en uso en el proyecto.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `settings-screen`: guardar los ajustes, además de persistirlos, MUST mostrar una confirmación
  visual; y cuando el nivel CEFR o la N cambian respecto a lo persistido, MUST cerrar la tanda activa
  sin aviso previo y MUST navegar a la pantalla de práctica.

## Impact

- `:feature:settings`: `SettingsViewModel` compara los ajustes guardados con los anteriores; si
  cambian, invoca un caso de uso de cierre forzado nuevo (ver `:core:domain` más abajo), sin aviso en
  la UI. El `UiState` gana una señal de un solo uso para la confirmación visual y otra para pedir la
  navegación a práctica, que `SettingsRoute` consume y reenvía a `:app`.
- `:app`: `FraseAFraseApp` reacciona a la señal de navegación cambiando `selectedDestination` a
  `BottomDestination.Practice`.
- `:feature:session`: al entrar a la pestaña de práctica, vuelve a leer la tanda activa en vez de
  confiar solo en el estado ya cargado en memoria (hoy el `ViewModel` solo carga una vez en `init`,
  porque sobrevive al cambio de pestaña al no haber pila de navegación); si la tanda no cambió, el
  estado visible (frase revelada, posición) no se altera. El mecanismo exacto se decide en
  `design.md`.
- `:core:domain`: caso de uso nuevo `ForceCloseActiveBatch` (paquete `practice`), que cierra la tanda
  activa sin el chequeo `expectedUncertified` de `CloseActiveBatch` — leer el número de no
  certificadas antes de cerrar (como hace `PracticeSessionViewModel.requestClose()`) obligaría a pasar
  por `ObtainActiveBatch`, que compone y guarda una tanda nueva con los ajustes viejos si no había
  ninguna activa, un efecto secundario no deseado aquí. Comparte la transacción de cierre con
  `CloseActiveBatch` mediante una función extraída, sin cambiar el contrato de `CloseActiveBatch`.
- Ningún ADR afectado: no cambia ninguna regla de composición, racha, certificación ni intervalos de
  ADR-0012; solo añade un disparador adicional para cerrar la tanda activa con las mismas reglas de
  resolución y composición ya vigentes.
