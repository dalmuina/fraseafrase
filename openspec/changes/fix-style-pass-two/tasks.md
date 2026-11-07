# Tasks

## 1. Insignias: tipografía consistente en el design system (design D4)

- [x] 1.1 Test de UI en `core/designsystem` (`DsLevelBadgeTest.kt`) que comprueba que el `Text` de
  la etiqueta de `DsLevelBadge` usa `MaterialTheme.typography.labelMedium` (p. ej. capturando el
  nodo y su `fontSize` reportado) y falla porque hoy no fija ningún `style`.
- [x] 1.2 Fijar `style = MaterialTheme.typography.labelMedium` en el `Text` de `DsLevelBadge`
  (design D4) y verificar que el test de 1.1 pasa en verde.

## 2. Ajustes: límites del stepper de N (settings-screen)

- [x] 2.1 Test instrumentado `givenBatchSizeAtMinimum_whenShowingTheScreen_thenDecreaseButtonIsDisabled`
  en `SettingsScreenTest.kt` (localiza el botón "−" por su contentDescription o texto accesible) y
  comprobar que falla porque hoy el botón siempre está habilitado (spec: settings-screen, escenario
  "El control de disminuir se deshabilita en el mínimo").
- [x] 2.2 Implementar en `BatchSizeCard` (`SettingsScreen.kt`) el cálculo de `enabled` del botón "−"
  a partir del entero actual de `batchSizeText` (`<= BatchSize.MIN` o no numérico deshabilita) y
  verificar que el test de 2.1 pasa en verde.
- [x] 2.3 Test instrumentado `givenBatchSizeAtMaximum_whenShowingTheScreen_thenIncreaseButtonIsDisabled`
  y comprobar que falla por el mismo motivo (spec: settings-screen, escenario "El control de
  aumentar se deshabilita en el máximo").
- [x] 2.4 Implementar el `enabled` simétrico del botón "+" (`>= BatchSize.MAX` o no numérico
  deshabilita) y verificar que el test de 2.3 pasa en verde.
- [x] 2.5 Test instrumentado `givenBatchSizeAboveMinimum_whenTypingAValue_thenDecreaseButtonIsEnabled`
  que escribe "15" tras partir de "10" y comprueba que el botón "−" vuelve a estar habilitado;
  comprobar que falla antes del paso 2.2 (spec: settings-screen, escenario "El control de disminuir
  se reactiva por encima del mínimo") y que pasa en verde tras 2.2.
- [x] 2.6 Igualar el tamaño de `OutlinedIconButton`/`FilledIconButton` en `BatchSizeCard` a la altura
  del `OutlinedTextField` adyacente (design D5) y verificar visualmente en
  `SettingsScreenPreviews.kt` (`@PreviewLightDark`) que quedan centrados y de la misma altura que el
  campo de texto.
- [x] 2.7 Test instrumentado en `SettingsScreenTest.kt` que comprueba que la etiqueta de cada chip
  de nivel CEFR en `CefrLevelCard` usa `MaterialTheme.typography.labelMedium` en vez del estilo por
  defecto del `FilterChip` (design D4), y falla con la implementación actual.
- [x] 2.8 Fijar `style = MaterialTheme.typography.labelMedium` en el `Text` de `CefrLevelCard`
  (`SettingsScreen.kt`) y verificar que el test de 2.7 pasa en verde.

## 3. Practicar: cabecera fija con título, píldora y barra de progreso (design D1)

- [x] 3.1 Test instrumentado `givenPracticingState_whenShowingTheScreen_thenTitleAndProgressPillAreDisplayed`
  en `PracticeSessionScreenTest.kt` que comprueba que se muestran el texto "Practicar" y el texto de
  posición ("X de Y") a la vez que la frase actual; comprobar que falla porque "Practicar" no se
  muestra hoy (`TopAppBar(title = {})`).
- [x] 3.2 Implementar el `topBar` de `PracticeSessionScreen` con el título "Practicar"
  (`MaterialTheme.typography.headlineLarge`) y la píldora de posición (`Surface`/`Box` con
  `MaterialTheme.shapes.extraLarge` y colores `primaryContainer`/`onPrimaryContainer`), visibles
  solo en el estado `Practicing` (design D1), y verificar que el test de 3.1 pasa en verde.
- [x] 3.3 Test instrumentado `givenPracticingState_whenShowingTheScreen_thenProgressIndicatorIsDisplayed`
  que localiza un `LinearProgressIndicator` en la cabecera y comprueba que falla porque hoy no
  existe ninguno.
- [x] 3.4 Añadir el `LinearProgressIndicator(progress = { index / total.toFloat() }, color =
  MaterialTheme.colorScheme.primary)` a la cabecera y verificar que el test de 3.3 pasa en verde.
- [x] 3.5 Test instrumentado `givenNothingToPracticeState_whenShowingTheScreen_thenTitleAndPillAreNotDisplayed`
  que comprueba que en el estado sin frases no aparece la píldora de posición (solo el mensaje
  centrado), y verificar que pasa en verde con la implementación de 3.2 (la cabecera solo se pinta
  en `Practicing`).
- [x] 3.6 Test instrumentado en `PracticeSessionScreenTest.kt` que comprueba que el `Text` del nivel
  CEFR en `PhraseCard` usa `MaterialTheme.typography.labelMedium` (design D4), y falla con el
  `labelLarge` actual.
- [x] 3.7 Cambiar `PhraseCard` (`PracticeSessionScreen.kt`) de `labelLarge` a `labelMedium` para el
  nivel CEFR y verificar que el test de 3.6 pasa en verde.

## 4. Practicar: contenido a ancho completo (design D2)

- [x] 4.1 Test instrumentado `givenRevealedState_whenShowingTheScreen_thenHitAndMissButtonsAreFullWidth`
  en `PracticeSessionScreenTest.kt` que obtiene el ancho de los nodos de "Fallo"/"Acierto" y
  comprueba que cada uno ocupa aproximadamente la mitad del ancho disponible de la pantalla (p. ej.
  usando `onRoot().getUnclippedBoundsInRoot()` como referencia); comprobar que falla con el layout
  centrado actual.
- [x] 4.2 Cambiar `PracticingContent`/`AssessmentButtons` para que el contenido use
  `Modifier.fillMaxWidth()` y `Row` con `Modifier.weight(1f)` en cada botón de autoevaluación
  (design D2), y verificar que el test de 4.1 pasa en verde.
- [x] 4.3 Test instrumentado `givenHiddenState_whenShowingTheScreen_thenRevealButtonIsFullWidth` que
  comprueba el ancho del botón "Mostrar traducción" y falla con el layout actual.
- [x] 4.4 Añadir `Modifier.fillMaxWidth()` al botón "Mostrar traducción" y verificar que el test de
  4.3 pasa en verde.

## 5. Progreso: tarjeta destacada con color primario (design D3)

- [x] 5.1 Test instrumentado en `ProgressScreenTest.kt` que comprueba el color de fondo de la
  tarjeta destacada (p. ej. capturando el nodo de la tarjeta y su `backgroundColor` reportado, o
  comprobando que el texto se pinta con `onPrimary` si no es practicable comprobar el fondo
  directamente) y falla con el `Card` de color por defecto actual.
- [x] 5.2 Implementar `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary,
  contentColor = MaterialTheme.colorScheme.onPrimary)` en la tarjeta destacada de
  `ProgressScreen.kt` y separar "Frases disponibles" / número grande / "Nivel actual: <nivel>" en
  tres `Text` (design D3), y verificar que el test de 5.1 pasa en verde.
- [x] 5.3 Verificar manualmente con `ProgressScreenPreviews.kt` (`@PreviewLightDark`) que el
  resultado coincide con el mock `Progreso.png` en claro y oscuro.

## 6. Verificación final

- [ ] 6.1 Ejecutar `./gradlew check` (tests unitarios, instrumentados donde aplique, ktlint y
  detekt) en verde.
- [ ] 6.2 Ejecutar `./gradlew koverVerify` y confirmar que se mantienen los mínimos de cobertura
  (dominio 90 %, presentación 80 %, global 70 %).

## Workflow follow-up

- Ejecutar `/opsx:verify` para comprobar la implementación contra `proposal.md`, `specs/` y
  `design.md`.
- Ejecutar `/opsx:archive` en la rama del cambio, antes de abrir la pull request a `develop`.
