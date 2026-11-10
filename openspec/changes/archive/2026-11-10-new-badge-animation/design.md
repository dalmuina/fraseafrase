# Design

## Context

`PracticeSessionViewModel.answer()` (`:feature:session`) hoy llama a `answerPhrase(...)` y, en la
misma corrutina, a `reload(previous = state)` — que sustituye `current` por la frase siguiente de
la tanda circular y reconstruye todo el `Practicing` con `busy = false` — **antes** de calcular
`badgeChange`. Solo después aplica `copy(badgeChange = badgeChange, busy = badgeChange != null)`
sobre ese estado ya avanzado. `PracticeSessionScreen.kt` renderiza el indicador
(`BadgeChangeIndicator`) como un `Box` centrado en toda la pantalla (`Modifier.align(Alignment.Center)`),
separado de la `Row` con `DsLevelBadge` dentro de `PhraseCard`. `DsLevelBadgeChange`
(`:core:designsystem`) solo anima un halo (`Canvas` + `animateFloatAsState`, 150 ms, 25 % de
alpha) detrás de una `DsLevelBadge` estática, y su `LaunchedEffect(emphasized)` está cacheado por
un `Boolean`, no por la instancia del cambio.

Ver `proposal.md` - Why para la motivación y `specs/practice-session/spec.md` de este cambio para
el contrato de comportamiento corregido.

## Goals / Non-Goals

**Goals:**
- Que la transición de insignia ocurra en la posición de la insignia actual de la frase
  respondida (junto a su nivel CEFR), no como una capa aparte sobre una frase ya navegada.
- Que el avance a la frase siguiente lo dispare el fin de esa transición, no la respuesta en sí.
- Que la transición sea una animación real y perceptible (crossfade + escala), no solo un halo
  casi invisible, reutilizando los tokens ya existentes de `Motion` y sin dependencias nuevas.

**Non-Goals:**
- No se toca la resolución del nivel en el dominio (`AnswerPhrase`, `PracticeBatch.answer()`):
  sigue siendo instantánea (ADR-0015, ADR-0017). Solo cambia cuándo se *presenta* ese resultado.
- No se diseña ninguna animación para cierre de tanda, generación de sesión ni Progreso.
- No se añade sonido, haptics ni ninguna librería de animación.

## Decisions

### 1. El avance de frase lo dispara `consumeBadgeChange()`, no `answer()`

`answer()` calcula `badgeChange` **antes** de decidir si navega:

```kotlin
private fun answer(assessment: SelfAssessment) {
    val state = currentPracticing() ?: return
    if (state.isAnswerable()) {
        _uiState.value = state.copy(busy = true)
        val levelBefore = state.current.level
        viewModelScope.launch {
            val levelAfter = answerPhrase(state.current.id, assessment)
            val badgeChange = badgeChangeFor(levelBefore, levelAfter)
            if (badgeChange != null) {
                updatePracticing { copy(badgeChange = badgeChange) }
            } else {
                reload(previous = state)
            }
        }
    }
}

private fun consumeBadgeChange() {
    val state = currentPracticing() ?: return
    viewModelScope.launch { reload(previous = state) }
}
```

Con cambio de nivel, `current`, `revealed` y `busy` (ya `true`) no se tocan: la frase respondida
sigue en pantalla con su insignia transicionando. `reload()` solo se invoca desde
`consumeBadgeChange()`, y como ya construye el siguiente `Practicing` con `busy = false` y
`badgeChange` sin fijar (por tanto `null`, su valor por defecto), no hace falta ningún `copy`
adicional para limpiarlos. El `state` capturado en `consumeBadgeChange()` conserva el
`index`/`total` de antes de responder (lo único que cambió entre medias fueron `badgeChange` y
`busy`), así que `reload(previous = state)` avanza el contador exactamente igual que hoy.

**Alternativa descartada**: mantener `reload()` en `answer()` y retrasar solo la *visibilidad* del
cambio de insignia con un `delay()` adicional antes de recomponer. Se descarta porque seguiría
mostrando la frase siguiente con la insignia de la frase anterior transicionando encima, que es
exactamente la confusión que se quiere corregir.

### 2. El indicador se renderiza dentro de `PhraseCard`, no en una capa centrada

Se elimina el `Box` centrado de `PracticingContent` en `PracticeSessionScreen.kt`. `PhraseCard`
pasa a recibir `badgeChange: BadgeChange?` y, en la `Row` que ya muestra el nivel CEFR, alterna
entre `DsLevelBadge` (sin cambio) y `BadgeChangeIndicator` (con cambio):

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s), verticalAlignment = Alignment.CenterVertically) {
    Text(text = item.cefrLevel.name, style = MaterialTheme.typography.labelMedium)
    if (badgeChange != null) {
        BadgeChangeIndicator(badgeChange = badgeChange, onAction = onAction)
    } else {
        DsLevelBadge(level = item.level.value)
    }
}
```

`BadgeChangeIndicator` conserva su `LaunchedEffect(badgeChange) { delay(...); onAction(ConsumeBadgeChange) }`
tal cual, solo sin `Modifier.align(Alignment.Center)`.

**Alternativa descartada**: mantener el indicador como overlay pero reposicionado sobre la
insignia real con coordenadas calculadas (`onGloballyPositioned`). Se descarta por complejidad
innecesaria: `PhraseCard` ya sabe exactamente dónde pintar la insignia; sustituirla ahí es más
simple y no depende de medir posiciones en tiempo de layout.

### 3. `BadgeChange` lleva el nivel de partida, no solo el resultante

`PracticeSessionUiState.BadgeChange` pasa de `(direction, badgeLabel, level)` a
`(direction, badgeLabel, fromLevel, toLevel)`; `badgeChangeFor(before, after)` pasa
`before.value` como `fromLevel` y `after.value` como `toLevel`. Es el único dato que falta para
que `DsLevelBadgeChange` pueda mostrar de dónde viene la transición, no solo a dónde va.

### 4. `DsLevelBadgeChange` transiciona de verdad: `Crossfade` + escala, disparador corregido

Firma nueva: `DsLevelBadgeChange(fromLevel: Int, toLevel: Int, emphasized: Boolean, modifier: Modifier = Modifier)`.
Tres defectos corregidos a la vez:

- `LaunchedEffect(emphasized)` (clave `Boolean`, no se repite en cambios sucesivos de la misma
  dirección) pasa a `LaunchedEffect(Unit)`: correcto ahora porque, tras la Decisión 2, el
  composable se inserta de nuevo en el árbol en cada aparición (swap `if/else` contra
  `DsLevelBadge`), no una instancia que sobrevive entre cambios.
- El halo deja de envolver el pill completo de `DsLevelBadge` (icono + etiqueta de texto).
  `DsLevelBadge` es un `Row(Icon, Text)`: su icono ocupa solo la parte izquierda del pill, así que
  un `Box(contentAlignment = Center)` que envuelve el pill entero centra el halo respecto al ancho
  total (variable según la etiqueta: "Bronce" vs. "Diamante"), no respecto al icono — el halo
  terminaba apareciendo al lado del icono, no detrás, y el ancho del indicador no coincidía con el
  de `DsLevelBadge` al intercambiarlos en la `Row` de `PhraseCard`, provocando un salto de layout
  al montar/desmontar. `DsLevelBadgeChange` pasa a reconstruir la misma estructura `Row(Icon, Text)`
  que `DsLevelBadge`, pero con dos `Crossfade` independientes (mismo `targetState`/`animationSpec`,
  así que quedan sincronizados visualmente): uno solo para el `Icon`, envuelto en un
  `Box(contentAlignment = Center)` junto al halo (ahí sí comparten el mismo bounding box, así que
  el halo queda centrado exactamente sobre el icono); otro solo para el `Text` de la etiqueta. Para
  reutilizar el icono/etiqueta de cada nivel sin duplicar el `when` de recursos, `levelBadgeSpec` y
  `LevelBadgeSpec` (hoy `private` en `DsLevelBadge.kt`) pasan a `internal`, visibles solo dentro de
  `:core:designsystem` (ADR-0010).
- La transición del icono y de la etiqueta usa `Crossfade` (tokens `Motion.durationMediumMillis` +
  `standardEasing`) envuelto en un `Box` con un `scale` de "pop" vía `animateFloatAsState` +
  `Modifier.graphicsLayer`, aplicado a la `Row` completa. El halo (`Canvas`, ya existente) se
  mantiene solo para `emphasized = true`, con la misma duración `durationMediumMillis` para que
  coincida con el crossfade en vez de los 150 ms originales, casi imperceptibles.

Se mantiene la firma de `DsLevelBadge` sin cambios y se sigue sin tipos del dominio en el design
system (ADR-0010). Se actualizan los `@PreviewLightDark` existentes para pasar
`fromLevel`/`toLevel`.

**Alternativa descartada**: `AnimatedContent` con transiciones de entrada/salida independientes
por tamaño. Se descarta por ahora: `Crossfade` ya resuelve la transición visual pedida
(insignia anterior → insignia nueva) sin la complejidad añadida de animar también el tamaño del
contenedor, que `DsLevelBadge` no varía entre niveles (mismo `Row` con icono + texto).

**Alternativa descartada (icono+halo)**: medir la posición del icono con `onGloballyPositioned` y
desplazar el halo a esas coordenadas. Se descarta por complejidad innecesaria: reconstruir el
mismo `Row(Icon, Text)` con un `Crossfade` por elemento consigue el mismo resultado (halo
compartiendo bounding box exacto con el icono) sin medir posiciones en tiempo de layout.

## Riesgos / Trade-offs

- **[Riesgo]** Alargar la permanencia en la frase respondida (en vez de avanzar de inmediato)
  retrasa, aunque sea brevemente, la siguiente interacción. → **Mitigación**: la duración total
  no cambia respecto al diseño ya aceptado (halo/crossfade usan los mismos tokens de `Motion`,
  subida ≈ 950 ms, bajada ≈ 750 ms); solo cambia *dónde* se percibe esa espera, no cuánto dura.
- **[Riesgo]** Los tests existentes (`PracticeSessionScreenTest`, `DsLevelBadgeChangeTest`) solo
  comprueban presencia (`assertExists()`) y tiempos de desaparición, no que haya una transición
  real — el mismo hueco que permitió que este defecto pasara desapercibido. → **Mitigación**: ver
  Estrategia de test.
- **[Trade-off]** Afinar visualmente la escala y la curva del crossfade requiere prueba manual,
  no solo tests automáticos (que verifican estados antes/después, no la suavidad del tránsito).
  Aceptado, igual que ya reconoció el cambio original para el halo.

## Estrategia de test

- **`:feature:session`** (ViewModel, JUnit 6 + Turbine + `MainDispatcherExtension`): tras
  `answer()` con cambio de nivel, `current` sigue siendo la frase respondida y `busy == true`
  mientras `badgeChange != null`; tras `ConsumeBadgeChange`, `current` ya es la frase siguiente,
  `badgeChange == null` y `busy == false`. Sin cambio de nivel, `current` avanza de inmediato tras
  `answer()`, sin pasar por `badgeChange` (regresión del comportamiento ya existente).
- **`:feature:session`** (Compose UI test, `PracticeSessionScreenTest`): mientras `badgeChange != null`,
  el texto de la frase respondida sigue visible (no el de la frase siguiente); con
  `composeTestRule.mainClock.autoAdvance = false`, en `t≈0` se ve la etiqueta de la insignia
  anterior y no la nueva, y tras avanzar más de `durationMediumMillis` se ve la nueva y no la
  anterior — la aserción que distingue "transiciona" de "aparece estático". Los tests ya
  existentes `givenUpBadgeChange_whenScreenShown_thenHaloIsShownAndRevealControlStaysPresent` y
  `givenDownBadgeChange_whenScreenShown_...` cambian de `Given` (no solo de posición): con el
  nuevo flujo `badgeChange` solo existe tras revelar y autoevaluar, así que su estado de partida
  pasa a `revealed = true`, y la aserción de control presente deja de comprobar "Mostrar
  traducción" (inalcanzable en ese estado) para comprobar "Acierto"/"Fallo" deshabilitados
  (`assertIsNotEnabled()`). El resto de tests de halo-presencia y duración total solo cambian de
  posición (misma altura que la etiqueta CEFR, ya no centrado en pantalla).
- **`:core:designsystem`** (Compose UI test, `DsLevelBadgeChangeTest`): mismo patrón
  antes/después con el reloj pausado, directamente sobre el componente con `fromLevel`/`toLevel`;
  además, con `emphasized = true`, el centro del halo (`getUnclippedBoundsInRoot()`) coincide con
  el centro del nodo del icono (mismo tag que usa `DsLevelBadge`), no con el centro del pill
  completo — la aserción geométrica que distingue "detrás del icono" de "al lado".
