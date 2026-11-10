# Tasks

## 1. `:core:designsystem` — `DsLevelBadgeChange` transiciona de verdad

- [x] 1.1 Actualizar `DsLevelBadgeChangeTest` (Compose UI test) para la nueva firma
  `fromLevel`/`toLevel`: con `composeTestRule.mainClock.autoAdvance = false`, en `t≈0` se ve la
  etiqueta de `fromLevel` y no la de `toLevel`; tras avanzar más de
  `Motion.durationMediumMillis`, se ve `toLevel` y no `fromLevel`. Ejecutar y comprobar que falla
  por aserción (la firma todavía no existe o el componente no transiciona). Escenario: spec
  `practice-session` "Un acierto que certifica muestra el indicador".
- [x] 1.2 Cambiar `DsLevelBadgeChange(level, emphasized, modifier)` a
  `DsLevelBadgeChange(fromLevel, toLevel, emphasized, modifier)`: `Crossfade` entre
  `DsLevelBadge(fromLevel)` y `DsLevelBadge(toLevel)` con `Motion.durationMediumMillis` +
  `standardEasing`, envuelto en un `Box` con `scale` de "pop" (`animateFloatAsState` +
  `graphicsLayer`); corregir `LaunchedEffect(emphasized)` a `LaunchedEffect(Unit)`; el halo
  (`emphasized = true`) pasa a usar `durationMediumMillis`. Verificar: el test de 1.1 pasa.
- [x] 1.3 Actualizar los `@PreviewLightDark` de `DsLevelBadgeChange` para pasar
  `fromLevel`/`toLevel`. Verificar: compila y las previews se renderizan sin error.

## 2. `:feature:session` — `BadgeChange` lleva el nivel de partida

- [x] 2.1 Actualizar los tests de `PracticeSessionViewModelTest` y de
  `PracticeSessionScreenTest` (`feature/session/src/androidTest/.../PracticeSessionScreenTest.kt`,
  construcciones de `BadgeChange(direction, badgeLabel, level = ...)` en los tests de
  `givenUpBadgeChange...`/`givenDownBadgeChange...`/`givenBadgeChange...`) para usar
  `fromLevel`/`toLevel` en vez de `level`, y añadir en `PracticeSessionViewModelTest` la
  aserción de que `fromLevel` coincide con el nivel anterior a autoevaluar. Ejecutar y comprobar
  que falla por compilación (el campo todavía no existe). Escenario: spec `practice-session` "Un
  fallo que baja el nivel muestra el indicador".
- [x] 2.2 Cambiar `BadgeChange(direction, badgeLabel, level)` a
  `BadgeChange(direction, badgeLabel, fromLevel, toLevel)` en `PracticeSessionUiState.kt`, y
  `badgeChangeFor(before, after)` en `PracticeSessionViewModel.kt` para pasar `before.value` como
  `fromLevel` y `after.value` como `toLevel`. Verificar: los tests de 2.1 pasan.

## 3. `:feature:session` — el avance lo dispara el fin de la transición

- [x] 3.1 En `PracticeSessionViewModelTest`: tras `answer()` con cambio de nivel, `current.id`
  sigue siendo el de la frase respondida y `busy == true` mientras `badgeChange != null`.
  Ejecutar y comprobar que falla (hoy `current` ya avanza antes de fijar `badgeChange`).
  Escenario: spec `practice-session` "Un acierto que certifica muestra el indicador".
- [x] 3.2 En `PracticeSessionViewModelTest`: tras dispatch de `ConsumeBadgeChange`, `current` es
  la frase siguiente de la tanda, `badgeChange == null` y `busy == false`. Ejecutar y comprobar
  que falla (hoy `consumeBadgeChange()` solo limpia campos, no avanza). Escenario: spec
  `practice-session` "El indicador desaparece sin intervención del usuario".
- [x] 3.3 En `PracticeSessionViewModelTest`: sin cambio de nivel, `current` avanza de inmediato
  tras `answer()` sin pasar por `badgeChange` (test de regresión del comportamiento ya existente,
  debe seguir en verde). Escenario: spec `practice-session` "Un acierto que no certifica no
  muestra transición y avanza de inmediato".
- [x] 3.4 Reordenar `PracticeSessionViewModel.answer()`: calcular `badgeChange` antes de decidir
  si navega; con cambio de nivel, solo `updatePracticing { copy(badgeChange = badgeChange) }`
  (sin tocar `current`/`revealed`/`busy`); sin cambio, `reload(previous = state)` igual que hoy.
  Reescribir `consumeBadgeChange()` para lanzar `reload(previous = state)` en una corrutina.
  Verificar: los tests de 3.1, 3.2 y 3.3 pasan.

## 4. `:feature:session` — el indicador vive en la tarjeta, no en una capa centrada

- [x] 4.1 Actualizar `PracticeSessionScreenTest`: mientras `badgeChange != null`, el texto de la
  frase respondida sigue visible en pantalla (no el de la frase siguiente), y el nodo
  `"practice_badge_change_indicator"` queda a la misma altura que la etiqueta del nivel CEFR
  (comparar `getUnclippedBoundsInRoot().top`), no centrado en la pantalla. Ejecutar y comprobar
  que falla (hoy el indicador está centrado y la frase ya avanzó). Escenario: spec
  `practice-session` "La transición no oculta el resto de la tarjeta".
- [x] 4.2 Mover `BadgeChangeIndicator` de la capa centrada (`Box` en `PracticingContent`) a la
  `Row` de `PhraseCard` que ya muestra el nivel CEFR + `DsLevelBadge`: `PhraseCard` recibe
  `badgeChange: BadgeChange?` y alterna `DsLevelBadge`/`BadgeChangeIndicator` ahí; quitar
  `Modifier.align(Alignment.Center)`. Verificar: el test de 4.1 pasa.
- [x] 4.3 Corregir `givenUpBadgeChange_whenScreenShown_thenHaloIsShownAndRevealControlStaysPresent`
  y `givenDownBadgeChange_whenScreenShown_...`: su `Given` pasa a construir el estado con
  `revealed = true` (ya no es alcanzable `badgeChange != null` con `revealed = false`), y la
  aserción de control presente deja de comprobar "Mostrar traducción" para comprobar que
  "Acierto"/"Fallo" existen y están deshabilitados (`assertIsNotEnabled()`), mientras la
  traducción revelada sigue visible. Ejecutar y comprobar que fallan con el `Given` actual antes
  de corregirlos. Escenario: spec `practice-session` "La transición no oculta el resto de la
  tarjeta".
- [x] 4.4 Ejecutar `PracticeSessionScreenTest` completo y confirmar que el resto de tests
  existentes de halo-presencia y duración total del indicador siguen en verde en la nueva
  posición.

## 5. Cierre

- [x] 5.1 Ejecutar `./gradlew check` (tests, ktlint, detekt) en verde.
- [x] 5.2 Ejecutar `./gradlew koverVerify` en verde.

## 6. Corrección: el halo queda detrás del icono, no del pill completo

Hallazgo de prueba manual tras el cierre del grupo 5 (design.md D4 corregido): el halo se
centraba respecto al pill entero de `DsLevelBadge` (icono + etiqueta), no respecto al icono, así
que aparecía "al lado" en vez de "detrás"; además el ancho del indicador no coincidía con el de
`DsLevelBadge` al intercambiarlos en la `Row` de `PhraseCard`, provocando un salto de layout al
montar/desmontar.

- [x] 6.1 Añadir a `DsLevelBadgeChangeTest` el test
  `givenEmphasizedWhenPaintedThenHaloIsCenteredOnIcon`: con `emphasized = true`, el centro de
  `getUnclippedBoundsInRoot()` del nodo con el tag del halo coincide (con un margen de 1dp) con el
  centro del nodo con el tag del icono de `toLevel` (ya asentado al no pausar el reloj; mismo tag
  que usa `DsLevelBadge`, p. ej. `ds-level-badge-icon-plata`). Ejecutar y comprobar que falla (hoy
  el halo se centra respecto al pill completo, no al icono). Escenario: spec `practice-session`
  "Un acierto que certifica muestra el indicador".
- [x] 6.2 Cambiar `levelBadgeSpec`/`LevelBadgeSpec` de `private` a `internal` en
  `DsLevelBadge.kt` (visibles solo dentro de `:core:designsystem`, ADR-0010).
- [x] 6.3 Reescribir `DsLevelBadgeChange` para reconstruir la misma estructura `Row(Icon, Text)`
  que `DsLevelBadge`, con dos `Crossfade` independientes (mismo `targetState`/`animationSpec`):
  uno para el `Icon` envuelto junto al halo en un `Box(contentAlignment = Center)` propio (halo e
  icono comparten bounding box), y otro para el `Text` de la etiqueta, fuera de ese `Box`. El
  `scale` de "pop" (`graphicsLayer`) se aplica a la `Row` completa. Verificar: el test de 6.1
  pasa, y los tests existentes de `DsLevelBadgeChangeTest` (halo presente/ausente,
  fromLevel/toLevel antes/después) siguen en verde.
- [x] 6.4 Ejecutar `./gradlew check` y `./gradlew koverVerify` en verde.
