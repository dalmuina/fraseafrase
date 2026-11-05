# Design

## Context

Ver `proposal.md` - Why. Estado actual relevante:

- `SettingsViewModel` (`:feature:settings`) carga los ajustes una vez en `init` y los guarda con
  `SaveUserSettings`, sin ninguna señal hacia la UI ni hacia `:app` tras guardar.
- `FraseAFraseApp` (`:app`) no usa `NavHost`: alterna qué `Route` compone con un `remember { mutableStateOf<BottomDestination> }`,
  sin pila de navegación (spec `app-navigation`). Las tres `Route` (`PracticeSessionRoute`, `ProgressRoute`,
  `SettingsRoute`) solo toman `modifier` y resuelven su propio `ViewModel` con `hiltViewModel()`; no hay
  callbacks de una feature hacia otra ni hacia `:app` hoy.
- `hiltViewModel()` sin una entrada de `NavBackStackEntry` resuelve el `ViewModelStoreOwner` en la
  `Activity`: el `PracticeSessionViewModel` sobrevive a cambiar de pestaña y no a la muerte de proceso.
  Su `init` solo se ejecuta una vez por proceso; cambiar de pestaña y volver no vuelve a leer la tanda.
- `CloseActiveBatch` (`:core:domain`) cierra la tanda activa, pero exige el número de no certificadas
  que vio el usuario en el aviso (`expectedUncertified`) y devuelve `Stale` si no coincide con el actual,
  para proteger el aviso de `practice-session` de una tanda que cambió mientras estaba abierto. Ajustes
  no muestra ningún aviso, así que no tiene ese número que proteger.

## Goals / Non-Goals

**Goals:**
- Confirmar visualmente el guardado y, cuando corresponda, cerrar la tanda activa y llevar al usuario a
  verla sin que tenga que cambiar de pestaña a mano.
- Mantener el contrato de `:feature:settings` y `:feature:session` con `:core:domain` (ADR-0006,
  ADR-0010): los `ViewModel` solo dependen de casos de uso; ninguna feature depende de otra.

**Non-Goals:**
- No se cambia `CloseActiveBatch` ni su contrato: lo sigue usando `practice-session` tal cual. El cierre
  sin aviso de este cambio usa un caso de uso nuevo (Decisión D3).
- No se introduce un `NavHost`: se sigue con el `when` de `BottomDestination` ya existente.

## Decisions

### D1: La confirmación y la navegación pendientes viven en `SettingsUiState.Loaded`, no en un canal de eventos aparte

`Loaded` gana dos banderas de un solo uso: `saveConfirmationPending: Boolean` y
`navigateToPracticePending: Boolean`. `SettingsAction` gana `ConsumeSaveConfirmation` y
`ConsumeNavigateToPractice`: `SettingsRoute` las observa con `LaunchedEffect`, actúa (snackbar o
navegación) y envía la acción de consumo correspondiente para que la bandera vuelva a `false` y no se
repita en una recomposición o rotación.

Alternativa considerada: un `Channel`/`SharedFlow` de eventos efímeros (patrón habitual en Compose para
"un solo disparo"). Se descarta porque ADR-0006 fija un único `StateFlow<UiState>` inmutable por
pantalla y ningún `ViewModel` del proyecto usa hoy un segundo flujo de eventos; añadirlo complicaría el
test (Turbine sobre dos flows) para un caso con solo dos señales. Dos banderas en el propio estado
siguen el contrato ya establecido y se testean igual que `closeWarning`.

### D2: "Hubo cambios" se compara contra la última base guardada en memoria, que se actualiza en cada guardado

`SettingsViewModel` guarda en memoria un `UserSettings` base, inicializado con lo que `reload()` lee al
abrir la pantalla, y lo compara con el `UserSettings` que se va a guardar. Si `cefrLevel` o `batchSize`
difieren de la base, hubo cambios. Tras cada guardado con éxito (con o sin cambios), la base en memoria
se actualiza al `UserSettings` recién guardado, para que un segundo guardado en la misma vida del
`ViewModel` compare contra lo que de verdad está persistido en ese momento y no contra la lectura
original de `reload()`. Esto importa porque `SettingsViewModel` sobrevive al cambio de pestaña igual que
`PracticeSessionViewModel` (D4): sin esta actualización, guardar N=15 y más tarde volver a guardar N=20
compararía contra la base original (20) y no detectaría el cambio real frente a lo persistido (15).

No se vuelve a leer `ObtainUserSettings` en el momento de guardar: no hace falta, porque en esta app no
hay otro escritor concurrente de los ajustes (sin cuentas ni sincronización, ADR-0004/ADR-0012) y la base
en memoria, actualizada en cada guardado, ya refleja lo persistido.

### D3: Un caso de uso nuevo, `ForceCloseActiveBatch`, cierra sin la comprobación de aviso obsoleto

Se añade `ForceCloseActiveBatch` en `:core:domain` (paquete `practice`), junto a `CloseActiveBatch`.
Cierra la tanda activa si existe, resolviendo cada frase con el mismo `ResolveBatchClosure` que ya usa
`CloseActiveBatch`, pero sin recibir ni comprobar un `expectedUncertified`: no hay ningún aviso mostrado
al usuario que proteger de quedar obsoleto. Sin tanda activa, no hace nada. La resolución y la escritura
de la tanda (actualizar progreso, vaciar `activeBatch`, recalcular la cola de arrastradas) se extraen a
una función compartida de paquete para que `CloseActiveBatch` y `ForceCloseActiveBatch` no dupliquen el
cuerpo de la transacción.

Alternativa considerada: reutilizar `CloseActiveBatch` leyendo antes el número de no certificadas (como
hace `PracticeSessionViewModel.requestClose()`) y pasándolo como `expectedUncertified`. Se descarta
porque obligaría a `:feature:settings` a depender también de `ObtainPracticeSession` o de leer el
catálogo solo para calcular un número que no se muestra ni protege nada aquí, y porque `ObtainActiveBatch`
(detrás de `ObtainPracticeSession`) **compone y guarda una tanda nueva si no hay ninguna** — un efecto
secundario no deseado al solo querer comprobar o cerrar la tanda existente antes de guardar los ajustes
nuevos.

### D4: `PracticeSessionRoute` vuelve a leer la sesión cada vez que entra en composición

Se añade `PracticeSessionAction.Resume` y `PracticeSessionRoute` lo envía en un `LaunchedEffect(Unit)`.
Como la pestaña de práctica se desmonta y remonta al cambiar de pestaña (sin `NavHost`), ese
`LaunchedEffect` se vuelve a ejecutar cada vez que el usuario vuelve a la pestaña, aunque el
`PracticeSessionViewModel` sea el mismo objeto (sobrevive en el `ViewModelStore` de la `Activity`).

`Resume` lee la sesión igual que `reload()`, pero **no sustituye el estado actual sin comparar primero**:
solo si la tanda leída difiere de la que ya está en memoria (frase actual distinta, orden distinto o
tamaño distinto) reemplaza el estado por el resultado de `reload()`; si es la misma tanda en el mismo
estado, `Resume` no toca `revealed`, `busy`, `closeWarning` ni `index`. Esto es necesario porque, sin esta
comparación, cualquier cambio de pestaña —no solo uno que siga a un cierre forzado desde Ajustes—
ocultaría una frase ya revelada y reiniciaría el indicador de posición a 1 aunque la tanda no hubiera
cambiado en absoluto, una regresión sobre el comportamiento ya entregado en `add-practice-batch-indicator`.
Con la comparación, si Ajustes cerró la tanda activa con `ForceCloseActiveBatch`, la siguiente vez que se
entra en la pestaña de práctica `Resume` detecta la tanda distinta y compone/muestra la nueva con las
reglas de `practice-batch` ya vigentes; si no cambió nada, la pantalla se ve exactamente como se dejó. Es
una lectura adicional en cada cambio de pestaña, pero es idempotente y barata (una lectura), y además
mantiene la tanda al día si cambió el día natural mientras la pestaña estaba en segundo plano.

Alternativa considerada: una señal explícita desde Ajustes ("se cerró una tanda") que `:app` reenviara a
`:feature:session`. Se descarta porque ninguna `Route` toma hoy callbacks de otra feature y porque
acoplaría `:feature:session` a un evento que solo tiene sentido si viene de ajustes, cuando "recargar al
entrar" ya es correcto en general (ADR-0010: las features no se conocen entre sí).

### D5: `SettingsRoute` gana `onNavigateToPractice`, con valor por defecto sin efecto

`SettingsRoute(modifier, viewModel, onNavigateToPractice: () -> Unit = {})`. `FraseAFraseApp` es el único
que pasa una implementación real (`{ selectedDestination = BottomDestination.Practice }`), igual que ya
decide qué `Route` compone; `SettingsRoute` no conoce `BottomDestination` ni ninguna otra feature
(ADR-0010). El valor por defecto deja las previews y los tests de `SettingsScreen`/`SettingsRoute` sin
necesidad de proveer el callback.

### D6 (revisada): La confirmación visual es un `Snackbar` de Material 3 en el `Scaffold` de `FraseAFraseApp`, no en uno propio de `SettingsScreen`

**Versión original de esta decisión (descartada tras encontrar un bug en verificación):** `SettingsScreen`
envolvía su contenido en su propio `Scaffold` con `snackbarHost`; `SettingsRoute` recordaba un
`SnackbarHostState` local y lo mostraba con `LaunchedEffect(uiState)`. Esto producía una carrera con D5:
cuando `saveConfirmationPending` y `navigateToPracticePending` se activan a la vez (cambiar el nivel CEFR
o N y guardar), los dos `LaunchedEffect(uiState)` de `SettingsRoute` se relanzan juntos. El de navegación
no espera nada y llama a `onNavigateToPractice()` de inmediato, lo que saca `SettingsRoute` de
composición en el mismo frame; Compose cancela entonces la corrutina del snackbar, suspendida dentro de
`showSnackbar()`. `SnackbarHostState.showSnackbar()` vacía `currentSnackbarData` en su `finally` al
cancelarse, así que el snackbar desaparece aunque ya se hubiera encolado, y la cancelación salta la línea
`ConsumeSaveConfirmation`: la bandera se queda en `true` en el `ViewModel` (que sobrevive al cambio de
pestaña) y solo se consume — mostrando el snackbar con retraso — la próxima vez que se entra en Ajustes.
Mover solo el `SnackbarHostState`/`SnackbarHost` a `:app` no bastaría: la corrutina que llama a
`showSnackbar()` seguiría viviendo en la composición de `SettingsRoute` y se cancelaría igual.

**Decisión final:** `SettingsRoute` ya no posee ningún `SnackbarHostState`. Gana un parámetro
`onShowSaveConfirmation: (String) -> Unit = {}` (mismo patrón que `onNavigateToPractice` de D5: valor por
defecto sin efecto, solo `:app` lo conecta). Su `LaunchedEffect` resuelve el texto con `stringResource`
(la cadena sigue viviendo en `:feature:settings`), llama a `onShowSaveConfirmation(texto)` sin esperar
nada, y envía `ConsumeSaveConfirmation` justo después, en la misma corrutona: como no depende de que el
snackbar termine de mostrarse, no hay nada que la cancelación de `SettingsRoute` pueda interrumpir a
medio camino. `FraseAFraseApp` añade `snackbarHost` a su propio `Scaffold` (el que envuelve la barra de
navegación inferior y nunca se desmonta al cambiar de pestaña) con un `SnackbarHostState` y un
`rememberCoroutineScope()` propios, e implementa el callback como
`{ message -> scope.launch { snackbarHostState.showSnackbar(message) } }`: esa corrutina vive en la
composición de `FraseAFraseApp`, que sobrevive al cambio de pestaña, así que el snackbar se ve completo
aunque la navegación ya haya cambiado de pantalla. `SettingsScreen` conserva su propio `Scaffold` solo
para el `topBar` y pierde el `snackbarHost`/`snackbarHostState` que ya no necesita. No se introduce ningún
componente `Ds*` nuevo: `Scaffold`, `SnackbarHost` y `Snackbar` siguen siendo de Material 3.

## Risks / Trade-offs

- [`ForceCloseActiveBatch` certifica frases con menos de 3 aciertos accidentalmente] → No aplica: usa el
  mismo `ResolveBatchClosure` que ya decide certificación por racha ≥ 3 (ADR-0012); cambiar de ajustes no
  cambia esa regla, solo dispara el cierre.
- [Recarga adicional en cada cambio de pestaña (D4) añade una lectura de Room/DataStore de más] →
  Mitigación: es una única lectura (`PracticeRepository.read()` + unión con catálogo en memoria), igual de
  barata que la que ya hace `init`; no hay E/S de red ni de disco pesada en esta app offline-first.
- [Guardar ajustes sin tanda activa pero con error de catálogo] → `ForceCloseActiveBatch` no depende del
  catálogo (solo de `PracticeRepository`), así que cierra o no la tanda sin que un catálogo roto lo
  bloquee; la pantalla de práctica, al navegar, mostrará su propio estado de error de catálogo como ya
  hace hoy (spec `practice-session`, "Error de catálogo"), sin relación con este cambio.
- [Carrera entre confirmación y navegación cuando ambas banderas se activan a la vez] → Detectado durante
  `/opsx:verify` manual (guardar con el nivel cambiado no mostraba el snackbar hasta volver a Ajustes) y
  corregido en D6 (revisada): el snackbar ya no depende de una corrutina que la navegación pueda cancelar
  a medio camino.
