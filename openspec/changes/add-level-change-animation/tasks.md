# Tasks

## 1. Dominio: `AnswerPhrase` expone el nivel resultante

- [ ] 1.1 Escribir el test de `AnswerPhrase` que espera que `invoke(...)` devuelva el `Level` resultante de
  la frase respondida (acierto que certifica, acierto que no certifica, fallo con nivel ≥ 1, fallo en
  nivel 0 y acierto que certifica de nuevo en nivel 5, que se mantiene en 5, ADR-0015) y comprobar que
  falla al compilar/ejecutar porque `invoke` todavía devuelve `Unit`.
- [ ] 1.2 Cambiar `AnswerPhrase.invoke` para que devuelva `Level` (localizando la frase ya resuelta por
  `PracticeBatch.answer()` en el nuevo snapshot por su `id`) y verificar que el test de 1.1 pasa en los
  cuatro casos.

## 2. Design system: indicador animado de insignia

- [ ] 2.1 Escribir el test de Compose UI de `DsLevelBadgeChange` que espera un halo visible (por `testTag`)
  cuando `emphasized = true` y ausente cuando `emphasized = false`, y comprobar que falla porque el
  componente no existe todavía.
- [ ] 2.2 Implementar `DsLevelBadgeChange(level: Int, emphasized: Boolean, modifier: Modifier = Modifier)`
  en `:core:designsystem` junto a `DsLevelBadge`, reutilizando sus mismos `drawableRes`/etiquetas, con los
  tokens de `Motion` para las duraciones y curvas (design.md, Decisión 4) y comprobar que el test de 2.1
  pasa.
- [ ] 2.3 Añadir `@PreviewLightDark` para el caso `emphasized = true` y `emphasized = false`, igual que ya
  tiene `DsLevelBadge`.

## 3. Presentación: estado y acción del cambio de insignia

- [ ] 3.1 Escribir el test de `PracticeSessionViewModel` que espera `badgeChange` con dirección `UP` y la
  etiqueta de la insignia resultante tras un acierto que certifica (spec: "Un acierto que certifica muestra
  el indicador"), y comprobar que falla porque `Practicing` no tiene ese campo todavía.
- [ ] 3.2 Añadir `BadgeChange`/`BadgeChange.Direction` y el campo `badgeChange: BadgeChange?` a
  `PracticeSessionUiState.Practicing`, y hacer que `PracticeSessionViewModel.answer()` lo calcule comparando
  `state.current.level` (antes) con el `Level` que ahora devuelve `AnswerPhrase` (después); verificar que el
  test de 3.1 pasa.
- [ ] 3.3 Escribir el test que espera `badgeChange` con dirección `DOWN` tras un fallo que baja el nivel
  (spec: "Un fallo que baja el nivel muestra el indicador") y que espera `badgeChange` nulo tras un acierto
  que no certifica, tras un fallo en nivel 0 y tras certificar de nuevo en nivel 5 (spec: "Un acierto que no
  certifica no muestra el indicador", "Un fallo en nivel 0 no muestra el indicador", "Certificar de nuevo en
  nivel máximo no muestra el indicador"), y comprobarlo en rojo si la comparación de 3.2 no cubre todavía
  esos cuatro casos.
- [ ] 3.4 Ajustar `answer()` para que esos tres casos pasen sin romper 3.1.
- [ ] 3.5 Escribir el test que espera que `busy` siga en `true` mientras `badgeChange` no es nulo, y vuelva
  a `false` al recibir `PracticeSessionAction.ConsumeBadgeChange`, que además pone `badgeChange` a `null`
  (design.md, Decisión 3), y comprobar que falla porque la acción no existe todavía.
- [ ] 3.6 Añadir `PracticeSessionAction.ConsumeBadgeChange` y su manejo en `onAction()`; verificar que el
  test de 3.5 pasa.

## 4. Presentación: reproducir la animación en pantalla

- [ ] 4.1 Escribir el test de Compose UI de `PracticeSessionScreen` que, tras una autoevaluación que cambia
  el nivel, espera que `DsLevelBadgeChange` aparezca con `emphasized` acorde a la dirección (spec: "La
  subida se muestra con halo", "La bajada se muestra sin halo") y que el control de revelar de la frase
  siguiente siga presente en el árbol semántico mientras se muestra (spec: "El control de revelar sigue
  presente mientras se anima el cambio de insignia"), y comprobar que falla porque la pantalla no pinta el
  indicador todavía.
- [ ] 4.2 Pintar `DsLevelBadgeChange` en `PracticeSessionScreen` cuando `uiState.badgeChange` no es nulo,
  con `emphasized = true` para `Direction.UP` y `false` para `Direction.DOWN`, y disparar
  `ConsumeBadgeChange` desde un `LaunchedEffect(uiState.badgeChange)` tras la duración correspondiente a su
  dirección; verificar que el test de 4.1 pasa.
- [ ] 4.3 Escribir el test que espera que el indicador deje de mostrarse por sí solo transcurrida su
  duración, sin ninguna interacción del usuario (spec: "El indicador desaparece sin intervención del
  usuario"), y verificar que pasa con la implementación de 4.2.
- [ ] 4.4 Escribir el test que espera que una autoevaluación sin cambio de nivel no pinte
  `DsLevelBadgeChange` en absoluto, y verificar que pasa.

## 5. Verificación final

- [ ] 5.1 Ejecutar `./gradlew test` y comprobar que todos los tests, incluidos los de los grupos 1 a 4,
  están en verde.
- [ ] 5.2 Ejecutar `./gradlew detekt` (ktlint incluido) y corregir cualquier aviso en los archivos tocados
  por este cambio.
- [ ] 5.3 Ejecutar `./gradlew koverVerify` y comprobar que `:core:domain`, `:feature:session` y
  `:core:designsystem` siguen cumpliendo sus mínimos de cobertura (90 %, 80 % y el mínimo global) tras los
  tests nuevos.
