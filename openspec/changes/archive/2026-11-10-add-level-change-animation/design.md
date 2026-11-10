# Design

## Context

`AnswerPhrase` (`:core:domain/practice/AnswerPhrase.kt`) llama a `PracticeRepository.transact`, que opera
sobre el `PracticeBatch` activo y devuelve `PracticeChange(nextSnapshot, result)`; hoy `result` es `Unit`.
`PracticeBatch.answer()` ya obtiene el `PhraseProgress` resultante de `PhraseProgress.record()` (nivel,
racha y `dueAtSession` ya resueltos al instante, ADR-0017) pero lo descarta tras guardarlo en el snapshot.

En presentación, `PracticeSessionViewModel.answer()` llama a `answerPhrase(...)` y después a `reload()`,
que lee la tanda ya recompuesta y construye el siguiente `PracticeSessionUiState.Practicing` a partir de la
*siguiente* frase de la lista circular, no de la que se acaba de responder. El nivel *anterior* de la frase
respondida es `state.current.level` (el valor ya en pantalla antes de autoevaluar).

`Motion` (`:core:designsystem/motion/Motion.kt`) define duraciones y curvas (`durationShortMillis = 150`,
`durationMediumMillis = 300`, `durationLongMillis = 500`, `standardEasing`, `emphasizedEasing`) que hoy
ningún componente usa todavía: sería el primer consumidor real de esos tokens.

Ver `proposal.md` - Why para la motivación y `specs/practice-session/spec.md` para el contrato de
comportamiento.

## Goals / Non-Goals

**Goals:**
- Exponer el nivel resultante de una autoevaluación desde `:core:domain` sin tocar ninguna regla de
  racha, certificación, bajada de nivel o enfriamiento (ADR-0015, ADR-0017).
- Reproducir la animación como una capa transitoria que no retrasa ni bloquea el avance normal de la tanda
  circular (ADR-0017: resolución al instante).
- Construir el componente de animación con Compose nativo y los tokens ya existentes de `Motion`, sin
  añadir ninguna dependencia nueva.

**Non-Goals:**
- No se diseña ninguna animación para el cierre de tanda, la generación de sesión ni la pantalla de
  Progreso (fuera de alcance, ver `proposal.md`).
- No se añade sonido ni haptics.
- No se introduce un mecanismo de eventos de UI de un solo uso (`SharedFlow`/`Channel`) distinto del
  `StateFlow` único de ADR-0006: el evento se modela como un campo más del estado inmutable.

## Decisions

### 1. `AnswerPhrase` devuelve el nivel resultante, no un tipo nuevo

`AnswerPhrase.invoke` cambia de `suspend operator fun invoke(...)` (sin retorno) a
`suspend operator fun invoke(...): Level`, propagando el resultado de `PracticeBatch.answer()` tal cual
(la frase resuelta ya está en `batch.entries` tras `.record()`; basta con localizarla por `id` tras
responder y devolver su `level`). `PracticeChange<T>` ya es genérico: no hace falta tocar
`PracticeRepository.transact` ni su firma.

**Alternativa descartada**: añadir un tipo sellado `AnswerResult` (`LevelUp`/`LevelDown`/`NoChange`) en el
dominio. Se descarta porque la comparación "antes vs. después" ya la puede hacer la presentación con un
dato que tiene a mano (`state.current.level`); duplicar esa lógica en el dominio sería un tipo que solo
sirve a un consumidor y mezclaría una decisión de presentación (qué cuenta como "cambio visible") con el
dominio.

### 2. El evento de animación vive en `PracticeSessionUiState.Practicing`, no en un flujo aparte

Nuevo campo `badgeChange: BadgeChange?` en `Practicing`, con:

```kotlin
data class BadgeChange(val direction: Direction, val badgeLabel: String) {
    enum class Direction { UP, DOWN }
}
```

`PracticeSessionViewModel.answer()` compara `state.current.level` (antes) con el `Level` que devuelve
`AnswerPhrase` (después) y construye `badgeChange` solo si difieren. `reload()` sigue construyendo el resto
del estado igual que hoy (la frase siguiente, su índice, etc.); `badgeChange` se añade aparte, sin alterar
en qué frase se basa el resto de campos. Mecánicamente, `answer()` calcula `badgeChange` antes de llamar a
`reload(previous = state)` y, tras esa llamada, aplica `_uiState.value = (_uiState.value as Practicing).copy(badgeChange = badgeChange, busy = badgeChange != null)`
sobre el `Practicing` que `reload()` ya dejó en `_uiState`, igual que hoy ya se encadenan `busy = true` antes
de la corrutina y la reconstrucción completa después.

Nueva acción `PracticeSessionAction.ConsumeBadgeChange`, que pone `badgeChange` a `null`. La pantalla la
dispara con `LaunchedEffect(uiState.badgeChange)`, tras una espera cuya duración depende de la dirección
(ver Decisión 4), igual que otros efectos de un solo uso ya resueltos con `LaunchedEffect` en Compose.

**Alternativa descartada**: un `Channel`/`SharedFlow` de eventos de UI independiente del `StateFlow`. Se
descarta porque ADR-0006 fija "un `StateFlow<XxxUiState>` inmutable por pantalla" y el patrón
"campo nullable que la pantalla consume y confirma con una acción" ya existe en este mismo ViewModel
(ninguno de los campos actuales lo necesita, pero el ciclo revelar/responder ya sigue esa misma idea de
estado controlado por acciones); introducir un segundo tipo de canal para un solo caso rompería esa
consistencia sin necesidad.

### 3. `busy` permanece `true` mientras se anima

Hoy `busy` se usa para deshabilitar los controles mientras se registra la respuesta (`answerPhrase` +
`reload()`). Con este cambio, `busy` sigue en `true` también durante la animación: se pone a `false` solo
cuando se consume `badgeChange` (o inmediatamente si no hubo cambio de nivel, como hoy). Los controles de
la frase siguiente (`practice-session`, "Controles de acción siempre visibles y pulsables") siguen visibles
en todo momento, solo deshabilitados, igual que ya ocurre hoy mientras `busy` es `true`.

**Alternativa descartada**: dejar `busy` en `false` tan pronto reload() termina, en paralelo a la
animación. Se descarta porque permitiría revelar o autoevaluar la frase siguiente mientras el indicador de
la frase anterior sigue en pantalla, lo que mezclaría visualmente dos frases distintas sin aportar nada:
la especificación exige que el indicador no bloquee la *visibilidad* de los controles, no que permita
interactuar con ellos antes de que termine.

### 4. Componente visual: `DsLevelBadgeChange` en `:core:designsystem`

Nuevo composable junto a `DsLevelBadge` (mismo paquete `component`), con firma
`DsLevelBadgeChange(level: Int, emphasized: Boolean, modifier: Modifier = Modifier)` — recibe `Int`, no
`Level`, por la misma razón que `DsLevelBadge` (ADR-0010: el design system no depende de ningún módulo del
proyecto). `emphasized` es `true` para una subida y `false` para una bajada; el componente decide su propio
estilo (halo o no) y duración a partir de ese único booleano, usando los tokens ya existentes de `Motion`:

- Subida (`emphasized = true`): `durationShortMillis` para la aparición con `emphasizedEasing` y un ligero
  sobre-escalado, halo dibujado con `Canvas` (círculo en `MaterialTheme.colorScheme.tertiary` a baja
  opacidad, pulsando con `animate*AsState`), visible durante `durationLongMillis`, y
  `durationMediumMillis` para desaparecer.
- Bajada (`emphasized = false`): sin halo, aparición y desaparición con `durationMediumMillis` y
  `standardEasing`, visible durante `durationShortMillis`. El total es menor que el de una subida, que es
  lo único que exige la especificación (no fija milisegundos concretos).

Se promueve directamente a `:core:designsystem` en vez de empezar en `:feature:session`, aunque hoy solo lo
use esa feature: reutiliza los mismos `drawableRes`/etiquetas de `DsLevelBadge` y es, como él, un
componente sin estado ni tipos del dominio — no hay razón para que viva en la feature primero.

**Alternativa descartada**: una librería de animación/confetti. Se descarta por `proposal.md` - Fuera de
alcance, y porque Compose ya cubre lo que pide la especificación (halo, aparición/desaparición, énfasis)
sin dependencias nuevas.

## Riesgos / Trade-offs

- **[Riesgo]** `busy = true` durante la animación retrasa, aunque sea brevemente, la posibilidad de revelar
  la frase siguiente. → **Mitigación**: la duración total (subida ≤ `durationShortMillis` +
  `durationLongMillis` + `durationMediumMillis` ≈ 950 ms; bajada ≤ `durationMediumMillis` × 2 ≈ 600 ms) es
  comparable a la espera de red/disco que ya existe hoy entre tocar HIT/MISS y ver la frase siguiente.
- **[Riesgo]** Un usuario que autoevalúa muy rápido varias frases seguidas podría ver solapamientos
  conceptuales entre indicadores si `busy` no se respeta en algún punto. → **Mitigación**: `busy` ya impide
  hoy una segunda autoevaluación mientras hay una en curso (spec `practice-session`, "Doble toque no
  registra dos respuestas"); este cambio solo extiende esa misma ventana, no introduce una nueva vía de
  carrera.
- **[Trade-off]** Afinar las duraciones exactas y el aspecto del halo requerirá prueba manual visual, no
  solo tests automáticos (que verifican presencia/ausencia del halo y relación de duraciones, no su
  apariencia exacta). Aceptado: es el mismo tipo de ajuste que ya reconoce ADR-0017 para sus propios
  números de enfriamiento.

## Estrategia de test

- **`:core:domain`** (unitario, JUnit 6 + Kotest): `AnswerPhrase` devuelve el `Level` esperado tras acierto
  que certifica, acierto que no certifica y fallo con y sin nivel que bajar. Reutiliza los fakes de
  `:core:testing` ya existentes para `PracticeRepository`.
- **`:feature:session`** (ViewModel, JUnit 6 + Turbine + `MainDispatcherExtension`):
  `PracticeSessionViewModel` expone `badgeChange` con la dirección correcta tras subida/bajada, no lo
  expone sin cambio de nivel, y lo limpia (vuelve a `null`, `busy` a `false`) tras `ConsumeBadgeChange`.
  Usa un fake de `AnswerPhrase`/`PracticeRepository` que permite fijar el nivel anterior y el devuelto.
- **`:core:designsystem`** (Compose UI test): `DsLevelBadgeChange` muestra el halo cuando `emphasized` es
  `true` y no lo muestra cuando es `false` (por `testTag` del halo, igual que `DsLevelBadge` ya usa
  `testTag` en su icono); además `@PreviewLightDark` para los dos casos (subida/bajada), como ya tiene
  `DsLevelBadge`.
- **`:feature:session`** (Compose UI test, integración de pantalla): con una autoevaluación que certifica,
  el indicador de cambio de insignia aparece y después desaparece solo, sin intervención del usuario,
  mientras los controles de la frase siguiente permanecen presentes en el árbol semántico (spec: escenarios
  de `specs/practice-session/spec.md` de este cambio).

No se inyecta ningún `Clock` ni dispatcher nuevo: la animación no depende de la hora del dispositivo, solo
de temporizadores de Compose (`LaunchedEffect` + `delay`), igual que el resto de la UI no depende del reloj
del dominio.
